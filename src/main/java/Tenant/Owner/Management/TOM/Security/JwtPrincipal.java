package Tenant.Owner.Management.TOM.Security;

import org.springframework.security.oauth2.jwt.Jwt;

import Tenant.Owner.Management.TOM.Constants.Constants;

public final class JwtPrincipal {
    private JwtPrincipal() {
    }

    public static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    public static Long ownerIdOrNull(Jwt jwt) {
        return Constants.ROLE_OWNER.equals(jwt.getClaimAsString("role")) ? userId(jwt) : null;
    }

    public static Long tenantIdOrNull(Jwt jwt) {
        return Constants.ROLE_TENANT.equals(jwt.getClaimAsString("role")) ? userId(jwt) : null;
    }
}