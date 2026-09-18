package com.hellfire.payment.dto;

import lombok.Data;

/**
 * Destination for a bank-transfer refund. Either account number + IFSC or a UPI id is required;
 * validated in the service because the requirement depends on the payment method.
 */
@Data
public class CustomerBankDetails {

    private String beneficiaryName;
    private String accountNumber;
    private String ifsc;
    private String upiId;
}
