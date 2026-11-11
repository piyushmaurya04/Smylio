package Tenant.Owner.Management.TOM.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoticeCreateRequest {
    private Long propertyId;

    @NotBlank(message = "title must not be blank")
    @Size(max = 255, message = "title must be 255 characters or fewer")
    private String title;

    @NotBlank(message = "message must not be blank")
    @Size(max = 255, message = "message must be 255 characters or fewer")
    private String message;
}