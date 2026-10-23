package Tenant.Owner.Management.TOM.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import Tenant.Owner.Management.TOM.Entity.Property;

public interface PropertyRepository extends JpaRepository<Property, Long> {
    List<Property> findByOwnerId(Long ownerId);
    Optional<Property> findByIdAndOwnerId(Long id, Long ownerId);

}
