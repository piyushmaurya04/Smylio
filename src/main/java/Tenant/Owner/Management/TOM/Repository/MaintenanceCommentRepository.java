package Tenant.Owner.Management.TOM.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import Tenant.Owner.Management.TOM.Entity.MaintenanceComment;

public interface MaintenanceCommentRepository extends JpaRepository<MaintenanceComment, Long> {
    List<MaintenanceComment> findByRequestIdOrderByCreatedAtAsc(Long requestId);

    void deleteByRequestId(Long requestId);
}