package edu.language.kbee.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

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
    private Set<String> features;

}
