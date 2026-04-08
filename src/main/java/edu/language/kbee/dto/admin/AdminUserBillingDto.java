package edu.language.kbee.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserBillingDto {
    private String currentPlan;
    private String price;
    private String billingCycle;
    private String renewalDate;
    private List<InvoiceDto> invoices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceDto {
        private String id; // txnRef
        private LocalDateTime date;
        private String amount; // Or could be derived from package
        private String status;
    }
}
