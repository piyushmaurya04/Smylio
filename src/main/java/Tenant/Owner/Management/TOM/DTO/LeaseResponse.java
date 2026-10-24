package Tenant.Owner.Management.TOM.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaseResponse {
    private Long id;
    private Long propertyId;
    private String propertyTitle;
    private List<String> amenities;
    private Long tenantId;
    private String tenantName;
    private String tenantCode;
    private String ownerName;
    private String ownerEmail;
    private String ownerPhone;

    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private Integer fineGraceDays;
    private BigDecimal finePerDay;
    private String status;

}
