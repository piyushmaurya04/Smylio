package Tenant.Owner.Management.TOM.Entity;

import java.time.LocalDateTime;

import Tenant.Owner.Management.TOM.Constants.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String password;
    @Column(nullable = false)
    private String phone;
    @Enumerated(EnumType.STRING)
    private UserRole role;
    @Column(name = "tenant_code")
    private String tenantCode;
    @Column(name = "created_at")
    private LocalDateTime createdAt;


}
