package Tenant.Owner.Management.TOM.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaintenanceStatusUpdate {
    @NotBlank(message = "status must not be blank")
    private String status;
}