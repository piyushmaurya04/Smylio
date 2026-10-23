package Tenant.Owner.Management.TOM.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {
    private String name;
    private String email;
    private String phone;
}
