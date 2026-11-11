package Tenant.Owner.Management.TOM.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaintenanceRequestCreate {
    @NotNull(message = "propertyId is required")
    private Long propertyId;

    @NotBlank(message = "title must not be blank")
    @Size(max = 255, message = "title must be 255 characters or fewer")
    private String title;

    @NotBlank(message = "description must not be blank")
    @Size(max = 255, message = "description must be 255 characters or fewer")
    private String description;

    @NotBlank(message = "priority must not be blank")
    private String priority;
}
