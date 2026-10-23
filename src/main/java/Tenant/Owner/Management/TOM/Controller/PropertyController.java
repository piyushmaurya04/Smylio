package Tenant.Owner.Management.TOM.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import Tenant.Owner.Management.TOM.DTO.PropertyRequest;
import Tenant.Owner.Management.TOM.DTO.PropertyResponse;
import Tenant.Owner.Management.TOM.Service.PropertyService;
import Tenant.Owner.Management.TOM.Security.JwtPrincipal;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {
    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @PostMapping
    public ResponseEntity<PropertyResponse> create(@RequestBody PropertyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        PropertyResponse response = propertyService.create(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<PropertyResponse> getMyProperties(@AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        return propertyService.getMyProperties(ownerId);
    }

    @GetMapping("/{id}")
    public PropertyResponse getOne(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        return propertyService.getOne(id, ownerId);
    }

    @PutMapping("/{id}")
    public PropertyResponse update(@PathVariable Long id, @RequestBody PropertyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        return propertyService.updateProperty(id, ownerId, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long ownerId = JwtPrincipal.userId(jwt);
        propertyService.deleteProperty(id, ownerId);
        return ResponseEntity.noContent().build();
    }

}
