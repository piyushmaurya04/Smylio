package Tenant.Owner.Management.TOM.DTO;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyRequest {
    @NotBlank(message = "title must not be blank")
    @Size(max = 255, message = "title must be 255 characters or fewer")
    private String title;

    @NotBlank(message = "address must not be blank")
    @Size(max = 255, message = "address must be 255 characters or fewer")
    private String address;

    @NotBlank(message = "type must not be blank")
    private String type;

    @NotNull(message = "rentAmount is required")
    @DecimalMin(value = "0.01", message = "rentAmount must be greater than 0")
    private BigDecimal rentAmount;

    @Size(max = 255, message = "description must be 255 characters or fewer")
    private String description;

    private List<String> amenities;
}
