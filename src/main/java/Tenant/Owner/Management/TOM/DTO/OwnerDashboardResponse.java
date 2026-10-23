package Tenant.Owner.Management.TOM.DTO;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerDashboardResponse {
    private long tenantCount;
    private BigDecimal pendingRent;
    private long openMaintenanceCount;
}