package Tenant.Owner.Management.TOM.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Tenant.Owner.Management.TOM.DTO.AuthResponse;
import Tenant.Owner.Management.TOM.DTO.LoginRequest;
import Tenant.Owner.Management.TOM.DTO.UserResponse;
import Tenant.Owner.Management.TOM.Entity.User;
import Tenant.Owner.Management.TOM.Repository.UserRepository;
import Tenant.Owner.Management.TOM.Security.JwtTokenService;

@Service
public class AuthService {
    private static final String INVALID_CREDENTIALS = "Invalid email or password";
    private static final Pattern BCRYPT_HASH = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS));

        String storedPassword = user.getPassword();
        boolean legacyPlaintext = !BCRYPT_HASH.matcher(storedPassword).matches();
        boolean passwordMatches = legacyPlaintext
                ? MessageDigest.isEqual(storedPassword.getBytes(StandardCharsets.UTF_8),
                        request.getPassword().getBytes(StandardCharsets.UTF_8))
                : passwordEncoder.matches(request.getPassword(), storedPassword);
        if (!passwordMatches) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        if (legacyPlaintext) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);
        }

        AuthResponse response = new AuthResponse();
        response.setAccessToken(jwtTokenService.createToken(user));
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtTokenService.getExpirationSeconds());
        response.setUser(toResponse(user));
        return response;
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
}