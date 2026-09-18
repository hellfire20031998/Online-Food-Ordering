package com.hellfire.onboarding.service;

import com.hellfire.exceptions.NotAuthorizedException;
import com.hellfire.exceptions.ResourceNotFoundException;
import com.hellfire.model.*;
import com.hellfire.notification.NotificationService;
import com.hellfire.onboarding.ApplicationMapper;
import com.hellfire.onboarding.BankDetails;
import com.hellfire.onboarding.dto.RestaurantApplicationDto;
import com.hellfire.onboarding.dto.RestaurantApplicationRequest;
import com.hellfire.repository.RestaurantApplicationRepository;
import com.hellfire.repository.RestaurantBankAccountRepository;
import com.hellfire.repository.RestaurantRepository;
import com.hellfire.repository.UserRepository;
import com.hellfire.team.dto.PageResponse;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Restaurant owner onboarding: a signed-in user applies, the platform team approves or rejects.
 * Approval creates the Restaurant and its payout account and promotes the applicant to ADMIN.
 */
@Service
@RequiredArgsConstructor
public class RestaurantApplicationService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantApplicationService.class);

    private final RestaurantApplicationRepository applicationRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantBankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ------------------------------------------------------------------ applicant side

    @Transactional
    public RestaurantApplicationDto submit(User applicant, RestaurantApplicationRequest req) {
        if (applicant.getRole() != null && applicant.getRole().isTeamRole()) {
            throw new IllegalArgumentException("Platform team accounts cannot apply for a restaurant");
        }
        if (restaurantRepository.findByOwnerId(applicant.getId()) != null) {
            throw new IllegalArgumentException("You already own a restaurant");
        }
        if (applicationRepository.existsByApplicantIdAndStatus(applicant.getId(), ApplicationStatus.PENDING)) {
            throw new IllegalArgumentException("You already have a pending application");
        }

        RestaurantApplication app = new RestaurantApplication();
        app.setApplicant(applicant);
        app.setStatus(ApplicationStatus.PENDING);
        app.setSubmittedAt(LocalDateTime.now());
        app.setApplicantPhone(req.getApplicantPhone());

        app.setRestaurantName(req.getRestaurantName().trim());
        app.setDescription(req.getDescription());
        app.setCuisineType(req.getCuisineType().trim());
        app.setOpeningHours(req.getOpeningHours());
        app.setStreetAddress(req.getAddress().getStreetAddress());
        app.setCity(req.getAddress().getCity());
        app.setState(req.getAddress().getState());
        app.setPincode(req.getAddress().getPincode());
        app.setCountry(req.getAddress().getCountry());
        app.setContactInformation(new ContactInformation(
                req.getContact().getEmail(), req.getContact().getMobile(),
                req.getContact().getTwitter(), req.getContact().getInstagram()));
        app.setImages(req.getImages() == null ? new ArrayList<>() : new ArrayList<>(req.getImages()));

        app.setBankAccountHolderName(req.getBankAccount().getAccountHolderName().trim());
        app.setBankAccountNumber(req.getBankAccount().getAccountNumber().trim());
        app.setBankIfsc(BankDetails.normalizeIfsc(req.getBankAccount().getIfsc()));
        app.setBankName(req.getBankAccount().getBankName().trim());
        app.setUpiId(BankDetails.normalizeUpi(req.getBankAccount().getUpiId()));

        RestaurantApplication saved = applicationRepository.save(app);
        log.info("AUDIT application submitted: id={} restaurant='{}' by {}", saved.getId(), saved.getRestaurantName(), applicant.getEmail());
        notificationService.applicationReceived(saved);
        return ApplicationMapper.toDto(saved, false);
    }

    @Transactional(readOnly = true)
    public List<RestaurantApplicationDto> myApplications(User applicant) {
        return applicationRepository.findByApplicantIdOrderBySubmittedAtDesc(applicant.getId()).stream()
                .map(a -> ApplicationMapper.toDto(a, false))
                .toList();
    }

    @Transactional
    public RestaurantApplicationDto withdraw(Long id, User applicant) {
        RestaurantApplication app = require(id);
        if (app.getApplicant() == null || !Objects.equals(app.getApplicant().getId(), applicant.getId())) {
            throw new NotAuthorizedException("This application is not yours");
        }
        if (app.getStatus() != ApplicationStatus.PENDING) {
            throw new IllegalArgumentException("Only pending applications can be withdrawn");
        }
        app.setStatus(ApplicationStatus.WITHDRAWN);
        app.setReviewedAt(LocalDateTime.now());
        app.setReviewedBy(applicant.getEmail());
        applicationRepository.save(app);
        log.info("AUDIT application withdrawn: id={} by {}", id, applicant.getEmail());
        return ApplicationMapper.toDto(app, false);
    }

    // ------------------------------------------------------------------ team side

    @Transactional(readOnly = true)
    public PageResponse<RestaurantApplicationDto> list(ApplicationStatus status, String q, int page, int size,
                                                       boolean revealBank) {
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        var pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "submittedAt"));
        return PageResponse.of(applicationRepository.findAll(spec(status, q), pageable),
                a -> ApplicationMapper.toDto(a, revealBank));
    }

    @Transactional(readOnly = true)
    public RestaurantApplicationDto get(Long id, boolean revealBank) {
        return ApplicationMapper.toDto(require(id), revealBank);
    }

    @Transactional
    public RestaurantApplicationDto approve(Long id, String actor, boolean revealBank) {
        RestaurantApplication app = require(id);
        if (app.getStatus() != ApplicationStatus.PENDING) {
            throw new IllegalArgumentException("Only pending applications can be approved");
        }
        User applicant = app.getApplicant();
        if (applicant.getRole() != null && applicant.getRole().isTeamRole()) {
            throw new IllegalArgumentException("Platform team accounts cannot own a restaurant");
        }
        if (restaurantRepository.findByOwnerId(applicant.getId()) != null) {
            throw new IllegalArgumentException("Applicant already owns a restaurant");
        }

        Address address = new Address();
        address.setStreetAddress(app.getStreetAddress());
        address.setCity(app.getCity());
        address.setState(app.getState());
        address.setPincode(app.getPincode());
        address.setCountry(app.getCountry());
        address.setUser(applicant);

        Restaurant restaurant = new Restaurant();
        restaurant.setOwner(applicant);
        restaurant.setName(app.getRestaurantName());
        restaurant.setDescription(app.getDescription());
        restaurant.setCuisineType(app.getCuisineType());
        restaurant.setOpeningHours(app.getOpeningHours());
        restaurant.setAddress(address);
        restaurant.setContactInformation(app.getContactInformation());
        restaurant.setImages(new ArrayList<>(app.getImages()));
        restaurant.setRegistrationDate(LocalDateTime.now());
        restaurant.setStatus(RestaurantStatus.ACTIVE);
        restaurant.setOpen(false); // owner opens once the menu is ready
        restaurant = restaurantRepository.save(restaurant);

        RestaurantBankAccount bank = new RestaurantBankAccount();
        bank.setRestaurant(restaurant);
        bank.setAccountHolderName(app.getBankAccountHolderName());
        bank.setAccountNumber(app.getBankAccountNumber());
        bank.setIfsc(app.getBankIfsc());
        bank.setBankName(app.getBankName());
        bank.setUpiId(app.getUpiId());
        bank.setUpdatedAt(LocalDateTime.now());
        bank.setUpdatedBy(actor);
        bankAccountRepository.save(bank);

        if (applicant.getRole() != UserRole.ADMIN) {
            applicant.setRole(UserRole.ADMIN);
            userRepository.save(applicant);
        }

        app.setStatus(ApplicationStatus.APPROVED);
        app.setReviewedAt(LocalDateTime.now());
        app.setReviewedBy(actor);
        app.setRestaurant(restaurant);
        applicationRepository.save(app);

        log.info("AUDIT application approved: id={} restaurantId={} owner={} by {}",
                id, restaurant.getId(), applicant.getEmail(), actor);
        notificationService.applicationApproved(app);
        return ApplicationMapper.toDto(app, revealBank);
    }

    @Transactional
    public RestaurantApplicationDto reject(Long id, String reason, String actor, boolean revealBank) {
        RestaurantApplication app = require(id);
        if (app.getStatus() != ApplicationStatus.PENDING) {
            throw new IllegalArgumentException("Only pending applications can be rejected");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required");
        }
        app.setStatus(ApplicationStatus.REJECTED);
        app.setRejectionReason(reason.trim());
        app.setReviewedAt(LocalDateTime.now());
        app.setReviewedBy(actor);
        applicationRepository.save(app);

        log.info("AUDIT application rejected: id={} by {} reason='{}'", id, actor, app.getRejectionReason());
        notificationService.applicationRejected(app);
        return ApplicationMapper.toDto(app, revealBank);
    }

    // ------------------------------------------------------------------ helpers

    private RestaurantApplication require(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application with ID " + id + " not found"));
    }

    private static Specification<RestaurantApplication> spec(ApplicationStatus status, String q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                var applicant = root.join("applicant", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("restaurantName")), like),
                        cb.like(cb.lower(root.get("city")), like),
                        cb.like(cb.lower(applicant.get("email")), like),
                        cb.like(cb.lower(applicant.get("fullName")), like)));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
