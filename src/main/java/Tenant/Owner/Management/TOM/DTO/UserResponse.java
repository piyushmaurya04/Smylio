package Tenant.Owner.Management.TOM.DTO;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String tenantCode;
    private LocalDateTime createdAt;
}
