package Tenant.Owner.Management.TOM.Entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import Tenant.Owner.Management.TOM.Constants.UserRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles = new HashSet<>();

    public Set<UserRole> getRoles() {
        if (roles == null) {
            roles = new HashSet<>();
        }
        if (roles.isEmpty() && role != null) {
            roles.add(role);
        }
        return roles;
    }

    public boolean hasRole(UserRole targetRole) {
        return getRoles().contains(targetRole);
    }

    public void addRole(UserRole targetRole) {
        getRoles().add(targetRole);
    }
}
