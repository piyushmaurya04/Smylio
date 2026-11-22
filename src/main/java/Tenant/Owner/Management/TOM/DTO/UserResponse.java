package Tenant.Owner.Management.TOM.DTO;

import java.time.LocalDateTime;
import java.util.List;

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
    private List<String> roles;
    private String tenantCode;
    private LocalDateTime createdAt;
}
