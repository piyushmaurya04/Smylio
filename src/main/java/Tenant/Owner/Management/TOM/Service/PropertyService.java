package Tenant.Owner.Management.TOM.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import Tenant.Owner.Management.TOM.Constants.Constants;
import Tenant.Owner.Management.TOM.DTO.PropertyRequest;
import Tenant.Owner.Management.TOM.DTO.PropertyResponse;
import Tenant.Owner.Management.TOM.Entity.Property;
import Tenant.Owner.Management.TOM.Repository.PropertyRepository;

@Service
public class PropertyService {

   private final PropertyRepository propertyRepository;
   private static final String STATUS_AVAILABLE = Constants.STATUS_AVAILABLE;

   public PropertyService(PropertyRepository propertyRepository) {
      this.propertyRepository = propertyRepository;
   }

   @Transactional
   public PropertyResponse create(PropertyRequest request, Long ownerId) {
      Property property = new Property();
      property.setTitle(request.getTitle());
      property.setAddress(request.getAddress());
      property.setRentAmount(request.getRentAmount());
      property.setDescription(request.getDescription());
      property.setType(request.getType());
      property.setCreatedAt(LocalDateTime.now());
      property.setStatus(STATUS_AVAILABLE);
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
      propertyResponse.setStatus(property.getStatus());
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
      if (property != null) {
         propertyRepository.delete(property);
      }

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
