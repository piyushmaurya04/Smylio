package Tenant.Owner.Management.TOM;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Tenant.Owner.Management.TOM.DTO.PropertyRequest;
import Tenant.Owner.Management.TOM.Entity.Property;
import Tenant.Owner.Management.TOM.Repository.PropertyRepository;
import Tenant.Owner.Management.TOM.Service.PropertyService;

@ExtendWith(MockitoExtension.class)
class PropertyServiceAmenitiesTest {
    @Mock
    private PropertyRepository propertyRepository;

    @InjectMocks
    private PropertyService propertyService;

    @Test
    void createTrimsAndDeduplicatesAmenities() {
        PropertyRequest request = request(List.of(" Parking ", "Wi-Fi", "parking"));
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> {
            Property property = invocation.getArgument(0);
            property.setId(12L);
            return property;
        });

        var response = propertyService.create(request, 4L);

        assertEquals(12L, response.getId());
        assertEquals(List.of("Parking", "Wi-Fi"), response.getAmenities());
    }

    @Test
    void updateReplacesAmenitiesAndNullClearsThem() {
        Property property = new Property();
        property.setId(12L);
        property.setAmenities(new HashSet<>(Set.of("Parking", "Wi-Fi")));
        when(propertyRepository.findByIdAndOwnerId(12L, 4L)).thenReturn(Optional.of(property));
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var updated = propertyService.updateProperty(12L, 4L, request(List.of("Gym")));
        var cleared = propertyService.updateProperty(12L, 4L, request(null));

        assertEquals(List.of("Gym"), updated.getAmenities());
        assertEquals(List.of(), cleared.getAmenities());
    }

    @Test
    void createRejectsBlankOrOverlongAmenity() {
        PropertyRequest blank = request(List.of("Parking", "  "));
        PropertyRequest overlong = request(List.of("A".repeat(101)));

        assertThrows(IllegalArgumentException.class, () -> propertyService.create(blank, 4L));
        assertThrows(IllegalArgumentException.class, () -> propertyService.create(overlong, 4L));
        verify(propertyRepository, never()).save(any(Property.class));
    }

    private PropertyRequest request(List<String> amenities) {
        PropertyRequest request = new PropertyRequest();
        request.setTitle("Test property");
        request.setAddress("1 Test Street");
        request.setType("FLAT");
        request.setRentAmount(BigDecimal.valueOf(1000));
        request.setAmenities(amenities);
        return request;
    }
}