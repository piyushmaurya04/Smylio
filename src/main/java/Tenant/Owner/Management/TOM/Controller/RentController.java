package Tenant.Owner.Management.TOM.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import Tenant.Owner.Management.TOM.DTO.DuesResponse;
import Tenant.Owner.Management.TOM.DTO.RentResponse;
import Tenant.Owner.Management.TOM.Service.RentService;
import Tenant.Owner.Management.TOM.Security.JwtPrincipal;

@RestController
@RequestMapping("/api/rents")
public class RentController {
    private final RentService rentService;

    public RentController(RentService rentService) {
        this.rentService = rentService;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generate(@AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        RentService.RentGenerationResult result = rentService.generateRentForOwner(ownerId);
        String message;
        if (result.leasesChecked() == 0) {
            message = "You have no active leases; no rent records were generated.";
        } else if (result.rentRecordsCreated() == 0) {
            message = "Checked " + result.leasesChecked() + " of your active lease"
                    + (result.leasesChecked() == 1 ? "" : "s")
                    + "; no new rent records were generated. Rent is created on each lease's monthly due date, and existing records are not duplicated.";
        } else {
            message = "Checked " + result.leasesChecked() + " of your active lease"
                    + (result.leasesChecked() == 1 ? "" : "s") + "; generated "
                    + result.rentRecordsCreated() + " rent record"
                    + (result.rentRecordsCreated() == 1 ? "." : "s.");
        }
        return ResponseEntity.ok(message);
    }

    @PostMapping("/{id}/pay/online")
    public RentResponse payOnline(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long tenantId = JwtPrincipal.userId(jwt);
        return rentService.payOnline(id, tenantId);
    }

    @PostMapping("/{id}/pay/cash")
    public RentResponse payCash(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long tenantId = JwtPrincipal.userId(jwt);
        return rentService.payCash(id, tenantId);
    }

    @PutMapping("/{id}/approve")
    public RentResponse approveCash(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        return rentService.approveCash(id, ownerId);
    }

    @PutMapping("/{id}/reject")
    public RentResponse rejectCash(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        return rentService.rejectCash(id, ownerId);
    }

    // owner's pending-approvals box
    @GetMapping("/approvals")
    public List<RentResponse> pendingApprovals(@AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        return rentService.getPendingApprovals(ownerId);
    }

    @GetMapping("/dues")
    public DuesResponse getDues(@AuthenticationPrincipal Jwt jwt) {
        Long tenantId = JwtPrincipal.userId(jwt);
        return rentService.getDues(tenantId);
    }

    @GetMapping("/lease/{leaseId}")
    public List<RentResponse> getLeaseHistory(
            @PathVariable Long leaseId,
            @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.ownerIdOrNull(jwt);
        Long tenantId = JwtPrincipal.tenantIdOrNull(jwt);
        return rentService.getLeaseHistory(leaseId, ownerId, tenantId);
    }
}
