package Tenant.Owner.Management.TOM.DTO;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoticeResponse {
    private Long id;
    private Long ownerId;
    private Long propertyId;
    private String propertyTitle;
    private String title;
    private String message;
    private LocalDateTime createdAt;
}