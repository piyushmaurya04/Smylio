package Tenant.Owner.Management.TOM.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RentResponse {
    private Long id;
    private Long leaseId;
    private String forMonth;
    private BigDecimal amount;
    private BigDecimal fine;
    private LocalDate dueDate;
    private LocalDate paidDate;
    private String method;
    private String transactionId;
    private String status;
}
