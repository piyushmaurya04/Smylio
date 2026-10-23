package Tenant.Owner.Management.TOM.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import Tenant.Owner.Management.TOM.DTO.LeaseRequest;
import Tenant.Owner.Management.TOM.DTO.LeaseResponse;
import Tenant.Owner.Management.TOM.Service.LeaseService;
import Tenant.Owner.Management.TOM.Security.JwtPrincipal;

@RestController
@RequestMapping("/api/leases")
public class LeaseController {
    private final LeaseService leaseService;

    public LeaseController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    @PostMapping
    public ResponseEntity<LeaseResponse> create(@RequestBody LeaseRequest leaseRequest,
            @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        LeaseResponse response = leaseService.create(leaseRequest, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<LeaseResponse> getMyLeases(@AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        return leaseService.getMyLeases(ownerId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeaseResponse> leaseUpdate(@PathVariable Long id,
            @RequestBody LeaseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        LeaseResponse response = leaseService.leaseUpdate(id, request, ownerId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/end")
    public ResponseEntity<LeaseResponse> endLease(@PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        LeaseResponse response = leaseService.endLease(id, ownerId);
        return ResponseEntity.ok(response);
    }

}
