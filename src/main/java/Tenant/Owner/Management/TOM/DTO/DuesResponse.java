package Tenant.Owner.Management.TOM.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DuesResponse {
    private BigDecimal totalDue;
    private LocalDate nextDueDate;
    private List<RentResponse> breakdown;
}
