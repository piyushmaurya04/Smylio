package Tenant.Owner.Management.TOM.Service;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import Tenant.Owner.Management.TOM.DTO.RegisterRequest;
import Tenant.Owner.Management.TOM.DTO.ProfileUpdateRequest;
import Tenant.Owner.Management.TOM.DTO.UserResponse;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import Tenant.Owner.Management.TOM.Constants.Constants;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    static final String ROLE_OWNER = Constants.ROLE_OWNER;
    static final String ROLE_TENANTS = Constants.ROLE_TENANT;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration details are required");
        }
        requireText(request.getName(), "name");
        requireText(request.getEmail(), "email");
        requireText(request.getPassword(), "password");
        requireText(request.getPhone(), "phone");
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be 72 UTF-8 bytes or fewer");
        }

        String role = request.getRole();
        if (!ROLE_OWNER.equals(role) && !ROLE_TENANTS.equals(role)) {
            throw new IllegalArgumentException("Invalid Role: " + role);

        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (emailExist(email)) {
            throw new IllegalStateException("Email already taken: " + email + " Please try again with different Email");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(email);
        user.setPhone(request.getPhone().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setCreatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        if (ROLE_TENANTS.equals(saved.getRole())) {
            saved.setTenantCode("TNT-" + saved.getId());
            saved = userRepository.save(saved);
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return toResponse(requireUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Profile details are required");
        }
        requireText(request.getName(), "name");
        requireText(request.getEmail(), "email");
        requireText(request.getPhone(), "phone");

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid email address");
        }

        User user = requireUser(userId);
        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new IllegalStateException("Email already taken: " + email);
        }
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPhone(request.getPhone().trim());
        return toResponse(userRepository.save(user));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    }

    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setTenantCode(user.getTenantCode());
        response.setCreatedAt(user.getCreatedAt());

        return response;
    }

    private boolean emailExist(String email) {
        boolean response = userRepository.existsByEmail(email);

        return response;
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
