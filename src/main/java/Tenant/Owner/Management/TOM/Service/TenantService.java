package Tenant.Owner.Management.TOM.Service;

import java.util.Locale;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Tenant.Owner.Management.TOM.Constants.UserRole;
import Tenant.Owner.Management.TOM.DTO.TenantSearchResponse;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.UserRepository;

@Service
public class TenantService {
    private final UserRepository userRepository;

    public TenantService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public TenantSearchResponse findByTenantCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Tenant code must not be blank");
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        User tenant = userRepository.findByTenantCode(normalizedCode)
                .filter(user -> UserRole.TENANT == user.getRole())
                .orElseThrow(() -> new NoSuchElementException("Tenant not found for code: " + normalizedCode));

        TenantSearchResponse response = new TenantSearchResponse();
        response.setName(tenant.getName());
        response.setMaskedPhone(maskPhone(tenant.getPhone()));
        response.setTenantCode(tenant.getTenantCode());
        return response;
    }

    static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "****";
        }
        String trimmed = phone.trim();
        int nonSpaceCount = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            if (!Character.isWhitespace(trimmed.charAt(i))) {
                nonSpaceCount++;
            }
        }
        int visibleTail = nonSpaceCount > 4 ? 4 : 0;
        int toMask = nonSpaceCount - visibleTail;
        StringBuilder masked = new StringBuilder(trimmed.length());
        int maskedSoFar = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isWhitespace(c)) {
                masked.append(c);
            } else if (maskedSoFar < toMask) {
                masked.append('*');
                maskedSoFar++;
            } else {
                masked.append(c);
            }
        }
        return masked.toString();
    }
}