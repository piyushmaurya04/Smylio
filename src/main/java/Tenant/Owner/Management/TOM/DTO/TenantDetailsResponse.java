package Tenant.Owner.Management.TOM.DTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantDetailsResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String tenantCode;
    private LocalDateTime createdAt;
    private long maintenanceRequestCount;
    private Map<Long, Long> maintenanceRequestsByProperty;
    private List<LeaseResponse> leases;
}
