package Tenant.Owner.Management.TOM.DTO;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentResponse {
    private Long id;
    private Long requestId;
    private Long userId;
    private String userName;
    private String comment;
    private LocalDateTime createdAt;
}
