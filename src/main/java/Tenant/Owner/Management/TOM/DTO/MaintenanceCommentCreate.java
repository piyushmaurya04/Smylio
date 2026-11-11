package Tenant.Owner.Management.TOM.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaintenanceCommentCreate {
    @NotBlank(message = "comment must not be blank")
    @Size(max = 255, message = "comment must be 255 characters or fewer")
    private String comment;
}