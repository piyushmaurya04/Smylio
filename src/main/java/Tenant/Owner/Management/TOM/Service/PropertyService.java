package Tenant.Owner.Management.TOM.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import Tenant.Owner.Management.TOM.Constants.PropertyStatus;
import Tenant.Owner.Management.TOM.DTO.PropertyRequest;
import Tenant.Owner.Management.TOM.DTO.PropertyResponse;
import Tenant.Owner.Management.TOM.Entity.Property;
import Tenant.Owner.Management.TOM.Repository.LeaseRepository;
import Tenant.Owner.Management.TOM.Repository.MaintenanceRequestRepository;
import Tenant.Owner.Management.TOM.Repository.NoticeRepository;
import Tenant.Owner.Management.TOM.Repository.PropertyRepository;

@Service
public class PropertyService {

   private final PropertyRepository propertyRepository;
   private final LeaseRepository leaseRepository;
   private final MaintenanceRequestRepository maintenanceRequestRepository;
   private final NoticeRepository noticeRepository;
   private final Clock clock;

   public PropertyService(PropertyRepository propertyRepository,
         LeaseRepository leaseRepository,
         MaintenanceRequestRepository maintenanceRequestRepository,
         NoticeRepository noticeRepository,
         Clock clock) {
      this.propertyRepository = propertyRepository;
      this.leaseRepository = leaseRepository;
      this.maintenanceRequestRepository = maintenanceRequestRepository;
      this.noticeRepository = noticeRepository;
      this.clock = clock;
   }

   @Transactional
   public PropertyResponse create(PropertyRequest request, Long ownerId) {
      Property property = new Property();
      property.setTitle(request.getTitle());
      property.setAddress(request.getAddress());
      property.setRentAmount(request.getRentAmount());
      property.setDescription(request.getDescription());
      property.setType(request.getType());
      property.setCreatedAt(LocalDateTime.now(clock));
      property.setStatus(PropertyStatus.AVAILABLE);
      property.setOwnerId(ownerId);
      property.setAmenities(normalizeAmenities(request.getAmenities()));

      Property saved = propertyRepository.save(property);

      return toResponse(saved);
   }

   private PropertyResponse toResponse(Property property) {
      PropertyResponse propertyResponse = new PropertyResponse();
      propertyResponse.setId(property.getId());
      propertyResponse.setOwnerId(property.getOwnerId());
      propertyResponse.setTitle(property.getTitle());
      propertyResponse.setAddress(property.getAddress());
      propertyResponse.setRentAmount(property.getRentAmount());
      propertyResponse.setDescription(property.getDescription());
      propertyResponse.setType(property.getType());
      propertyResponse.setStatus(property.getStatus() != null ? property.getStatus().name() : null);
      propertyResponse.setCreatedAt(property.getCreatedAt());
      propertyResponse.setAmenities(property.getAmenities().stream()
            .sorted(String.CASE_INSENSITIVE_ORDER).toList());

      return propertyResponse;

   }

   @Transactional(readOnly = true)
   public List<PropertyResponse> getMyProperties(Long ownerId) {

      return propertyRepository.findByOwnerId(ownerId).stream().map(this::toResponse).toList();
   }

   @Transactional(readOnly = true)
   public PropertyResponse getOne(Long id, Long ownerId) {
      Property property = propertyRepository.findByIdAndOwnerId(id, ownerId)
            .orElseThrow(() -> new java.util.NoSuchElementException("Property not found with id: " + id));
      return toResponse(property);
   }

   @Transactional
   public PropertyResponse updateProperty(Long id, Long ownerId, PropertyRequest request) {
      Property property = propertyRepository.findByIdAndOwnerId(id, ownerId)
            .orElseThrow(() -> new java.util.NoSuchElementException("No Property found " + id));
      property.setTitle(request.getTitle());
      property.setAddress(request.getAddress());
      property.setDescription(request.getDescription());
      property.setRentAmount(request.getRentAmount());
      property.setType(request.getType());
      property.setAmenities(normalizeAmenities(request.getAmenities()));

      Property saved = propertyRepository.save(property);
      return toResponse(saved);
   }

   @Transactional
   public void deleteProperty(long id, Long ownerId) {
      Property property = propertyRepository.findByIdAndOwnerId(id, ownerId)
            .orElseThrow(() -> new java.util.NoSuchElementException("Property Not Found " + id));
      if (PropertyStatus.OCCUPIED == property.getStatus()
            || leaseRepository.existsByPropertyId(id)
            || maintenanceRequestRepository.existsByPropertyId(id)
            || noticeRepository.existsByProperty_Id(id)) {
         throw new IllegalStateException(
               "Property has existing leases, maintenance requests, or notices and cannot be deleted");
      }
      propertyRepository.delete(property);

   }

   private Set<String> normalizeAmenities(List<String> amenities) {
      Map<String, String> uniqueAmenities = new LinkedHashMap<>();
      if (amenities == null) {
         return new LinkedHashSet<>();
      }

      for (String amenity : amenities) {
         if (amenity == null || amenity.isBlank()) {
            throw new IllegalArgumentException("Amenities must not contain blank values");
         }
         String normalized = amenity.trim();
         if (normalized.length() > 100) {
            throw new IllegalArgumentException("Each amenity must be 100 characters or fewer");
         }
         uniqueAmenities.putIfAbsent(normalized.toLowerCase(Locale.ROOT), normalized);
      }
      return new LinkedHashSet<>(uniqueAmenities.values());
   }

}
