package Tenant.Owner.Management.TOM.Service;

import java.util.Locale;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Tenant.Owner.Management.TOM.Constants.Constants;
import Tenant.Owner.Management.TOM.DTO.UserResponse;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.UserRepository;

@Service
public class TenantService {
    private final UserRepository userRepository;

    public TenantService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse findByTenantCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Tenant code must not be blank");
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        User tenant = userRepository.findByTenantCode(normalizedCode)
                .filter(user -> Constants.ROLE_TENANT.equals(user.getRole()))
                .orElseThrow(() -> new NoSuchElementException("Tenant not found for code: " + normalizedCode));

        UserResponse response = new UserResponse();
        response.setId(tenant.getId());
        response.setName(tenant.getName());
        response.setEmail(tenant.getEmail());
        response.setPhone(tenant.getPhone());
        response.setRole(tenant.getRole());
        response.setTenantCode(tenant.getTenantCode());
        response.setCreatedAt(tenant.getCreatedAt());
        return response;
    }
}