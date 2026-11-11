package Tenant.Owner.Management.TOM.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantSearchResponse {
    private String name;
    private String maskedPhone;
    private String tenantCode;
}

