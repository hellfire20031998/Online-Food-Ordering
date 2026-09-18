package com.hellfire.payment.service;

import com.hellfire.exceptions.ResourceNotFoundException;
import com.hellfire.model.CustomerBankAccount;
import com.hellfire.model.User;
import com.hellfire.onboarding.BankDetails;
import com.hellfire.onboarding.dto.BankAccountDto;
import com.hellfire.payment.dto.CustomerBankAccountRequest;
import com.hellfire.payment.dto.CustomerBankDetails;
import com.hellfire.repository.CustomerBankAccountRepository;
import com.hellfire.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/** Customers' saved refund accounts. The owner sees their own unmasked; the team sees them masked unless entitled. */
@Service
@RequiredArgsConstructor
public class CustomerBankAccountService {

    private static final Logger log = LoggerFactory.getLogger(CustomerBankAccountService.class);

    private final CustomerBankAccountRepository repository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<BankAccountDto> getOwn(User user) {
        return repository.findByUserId(user.getId()).map(a -> toDto(a, true));
    }

    @Transactional(readOnly = true)
    public Optional<CustomerBankDetails> savedDetails(User user) {
        return repository.findByUserId(user.getId()).map(a -> {
            CustomerBankDetails d = new CustomerBankDetails();
            d.setBeneficiaryName(a.getAccountHolderName());
            d.setAccountNumber(a.getAccountNumber());
            d.setIfsc(a.getIfsc());
            d.setUpiId(a.getUpiId());
            return d;
        });
    }

    @Transactional
    public BankAccountDto upsertOwn(User user, CustomerBankAccountRequest req) {
        return upsert(user, req.getAccountHolderName(), req.getAccountNumber(), req.getIfsc(), req.getBankName(), req.getUpiId());
    }

    /** Called when a customer ticks "save these details" on a refund request. */
    @Transactional
    public void saveFromRefund(User user, CustomerBankDetails details) {
        if (details == null) {
            return;
        }
        upsert(user, details.getBeneficiaryName(), details.getAccountNumber(), details.getIfsc(), null, details.getUpiId());
    }

    @Transactional
    public void deleteOwn(User user) {
        repository.findByUserId(user.getId()).ifPresent(a -> {
            repository.delete(a);
            log.info("AUDIT customer bank account deleted: userId={}", user.getId());
        });
    }

    /** Team view. Callers restrict to TEAM_ADMIN / TEAM_MANAGER and record the reveal. */
    @Transactional(readOnly = true)
    public BankAccountDto getForTeam(Long customerId) {
        userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with ID " + customerId + " not found"));
        return repository.findByUserId(customerId)
                .map(a -> toDto(a, true))
                .orElseThrow(() -> new ResourceNotFoundException("No refund account on file for customer " + customerId));
    }

    private BankAccountDto upsert(User user, String holder, String number, String ifsc, String bankName, String upi) {
        String cleanHolder = holder == null ? null : holder.trim();
        String cleanNumber = number == null || number.isBlank() ? null : number.trim();
        String cleanIfsc = ifsc == null || ifsc.isBlank() ? null : BankDetails.normalizeIfsc(ifsc);
        String cleanUpi = BankDetails.normalizeUpi(upi);

        if (cleanHolder == null || cleanHolder.isBlank()) {
            throw new IllegalArgumentException("accountHolderName is required");
        }
        if (cleanNumber != null && !cleanNumber.matches("\\d{9,18}")) {
            throw new IllegalArgumentException("accountNumber must be 9 to 18 digits");
        }
        if (cleanIfsc != null && !cleanIfsc.matches("^[A-Z]{4}0[A-Z0-9]{6}$")) {
            throw new IllegalArgumentException("ifsc must be a valid IFSC code");
        }
        if ((cleanNumber == null) != (cleanIfsc == null)) {
            throw new IllegalArgumentException("accountNumber and ifsc must be provided together");
        }
        if (cleanNumber == null && cleanUpi == null) {
            throw new IllegalArgumentException("Provide an account number with IFSC, or a UPI id");
        }

        CustomerBankAccount account = repository.findByUserId(user.getId()).orElseGet(() -> {
            CustomerBankAccount created = new CustomerBankAccount();
            created.setUser(user);
            return created;
        });
        account.setAccountHolderName(cleanHolder);
        account.setAccountNumber(cleanNumber);
        account.setIfsc(cleanIfsc);
        account.setBankName(bankName == null || bankName.isBlank() ? null : bankName.trim());
        account.setUpiId(cleanUpi);
        account.setUpdatedAt(LocalDateTime.now());
        repository.save(account);
        log.info("AUDIT customer bank account saved: userId={}", user.getId());
        return toDto(account, true);
    }

    static BankAccountDto toDto(CustomerBankAccount a, boolean reveal) {
        return new BankAccountDto(
                a.getAccountHolderName(),
                reveal ? a.getAccountNumber() : BankDetails.maskAccountNumber(a.getAccountNumber()),
                a.getIfsc(),
                a.getBankName(),
                reveal ? a.getUpiId() : BankDetails.maskUpi(a.getUpiId()),
                !reveal,
                a.getUpdatedAt(),
                null);
    }
}
