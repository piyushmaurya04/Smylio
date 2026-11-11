package Tenant.Owner.Management.TOM.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaseRequest {
    private Long propertyId;
    private String tenantCode;
    private LocalDate startDate;

    @NotNull(message = "monthlyRent is required")
    @DecimalMin(value = "0.01", message = "monthlyRent must be greater than 0")
    private BigDecimal monthlyRent;

    @NotNull(message = "fineGraceDays is required")
    @Min(value = 0, message = "fineGraceDays must be 0 or greater")
    private Integer fineGraceDays;

    @NotNull(message = "finePerDay is required")
    @DecimalMin(value = "0.00", message = "finePerDay must be 0 or greater")
    private BigDecimal finePerDay;
}
