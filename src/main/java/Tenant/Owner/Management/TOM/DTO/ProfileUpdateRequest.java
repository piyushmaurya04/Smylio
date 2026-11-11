package Tenant.Owner.Management.TOM.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {
    @NotBlank(message = "name must not be blank")
    @Size(max = 255, message = "name must be 255 characters or fewer")
    private String name;

    @NotBlank(message = "email must not be blank")
    @Email(message = "Enter a valid email address")
    @Size(max = 255, message = "email must be 255 characters or fewer")
    private String email;

    @NotBlank(message = "phone must not be blank")
    @Size(max = 255, message = "phone must be 255 characters or fewer")
    private String phone;
}
