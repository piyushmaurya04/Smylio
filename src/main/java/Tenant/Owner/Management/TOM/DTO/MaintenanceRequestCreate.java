package Tenant.Owner.Management.TOM.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaintenanceRequestCreate {
    private Long propertyId;
    private String title;
    private String description;
    private String priority;
}
