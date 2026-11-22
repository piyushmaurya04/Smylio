package Tenant.Owner.Management.TOM.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SwitchRoleRequest {
    @NotBlank(message = "Role must not be blank")
    private String role;
}
