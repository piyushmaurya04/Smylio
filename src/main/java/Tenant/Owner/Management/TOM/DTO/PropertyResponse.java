package Tenant.Owner.Management.TOM.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyResponse {
   private Long id;
   private Long ownerId;
   private String title;
   private String address;
   private String type;
   private BigDecimal rentAmount;
   private String status;
   private String description;
   private LocalDateTime createdAt;
   private List<String> amenities;
}
