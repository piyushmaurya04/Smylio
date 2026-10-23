package Tenant.Owner.Management.TOM.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaseRequest {
    private Long propertyId;
    private String tenantCode;
    private LocalDate startDate;
    private BigDecimal monthlyRent;
    private Integer fineGraceDays;
    private BigDecimal finePerDay;
}
