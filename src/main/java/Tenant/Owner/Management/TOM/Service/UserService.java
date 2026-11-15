package Tenant.Owner.Management.TOM.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import Tenant.Owner.Management.TOM.DTO.ChangePasswordRequest;
import Tenant.Owner.Management.TOM.DTO.RegisterRequest;
import Tenant.Owner.Management.TOM.DTO.ProfileUpdateRequest;
import Tenant.Owner.Management.TOM.DTO.UserResponse;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.RefreshTokenRepository;
import Tenant.Owner.Management.TOM.Repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import Tenant.Owner.Management.TOM.Constants.UserRole;

@Service
public class UserService {
    private static final char[] CROCKFORD_BASE32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int TENANT_CODE_LENGTH = 8;
    private static final int MAX_CODE_ATTEMPTS = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
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

        UserRole role;
        try {
            role = UserRole.valueOf(request.getRole());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("Invalid Role: " + request.getRole());
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (emailExist(email)) {
            throw new IllegalStateException("An account with this email address already exists. Please log in or use a different email.");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPhone(request.getPhone().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now(clock));
        if (UserRole.TENANT == role) {
            user.setTenantCode(generateUniqueTenantCode());
        }
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    private String generateUniqueTenantCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String candidate = randomTenantCode();
            if (!userRepository.existsByTenantCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate a unique tenant code; please retry");
    }

    private String randomTenantCode() {
        StringBuilder code = new StringBuilder(4 + TENANT_CODE_LENGTH).append("TNT-");
        for (int i = 0; i < TENANT_CODE_LENGTH; i++) {
            code.append(CROCKFORD_BASE32[SECURE_RANDOM.nextInt(CROCKFORD_BASE32.length)]);
        }
        return code.toString();
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

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Password change details are required");
        }
        requireText(request.getCurrentPassword(), "current password");
        requireText(request.getNewPassword(), "new password");

        if (request.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters");
        }
        if (request.getNewPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("New password must be 72 UTF-8 bytes or fewer");
        }

        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as your current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now(clock));
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
        response.setRole(user.getRole() != null ? user.getRole().name() : null);
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
