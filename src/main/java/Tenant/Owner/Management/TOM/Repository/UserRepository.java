package Tenant.Owner.Management.TOM.Repository;

import Tenant.Owner.Management.TOM.Entity.User;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  boolean existsByEmail(String email);

  boolean existsByEmailAndIdNot(String email, Long id);

  Optional<User> findByEmail(String email);

  Optional<User> findByTenantCode(String tenantCode);
}
