package Tenant.Owner.Management.TOM.DTO;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyRequest {
    private String title;
    private String address;
    private String type;
    private BigDecimal rentAmount;
    private String description;
    private List<String> amenities;
}
