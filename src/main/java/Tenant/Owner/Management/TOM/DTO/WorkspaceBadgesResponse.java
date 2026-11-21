package Tenant.Owner.Management.TOM.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkspaceBadgesResponse {
    private int ownerPendingApprovals;
    private int ownerOpenTickets;
    private int tenantPendingRents;
    private int tenantUnreadNotices;
}
