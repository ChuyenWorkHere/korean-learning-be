package edu.language.kbee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPackageResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer durationDays;
    private Boolean isActive;
    private List<String> features;

}
