package Tenant.Owner.Management.TOM.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private UserResponse user;
}