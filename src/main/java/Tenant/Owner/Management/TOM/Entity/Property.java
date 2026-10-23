package Tenant.Owner.Management.TOM.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "properties")
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id")
    private Long ownerId;

    private String title;
    private String address;
    private String type;

    @Column(name = "rent_amount")
    private BigDecimal rentAmount;

    private String status;
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "property_amenities", joinColumns = @JoinColumn(name = "property_id"), uniqueConstraints = @UniqueConstraint(name = "uk_property_amenity", columnNames = {
            "property_id", "amenity" }), foreignKey = @ForeignKey(name = "fk_property_amenities_property"))
    @Column(name = "amenity", nullable = false, length = 100)
    private Set<String> amenities = new HashSet<>();
}
