package com.harbr.property.application;

import com.harbr.auth.domain.Role;
import com.harbr.auth.domain.User;
import com.harbr.auth.infrastructure.UserRepository;
import com.harbr.common.exception.EntityNotFoundException;
import com.harbr.common.exception.ForbiddenException;
import com.harbr.common.storage.FileStorageService;
import com.harbr.common.web.PagedResponse;
import com.harbr.property.application.dto.*;
import com.harbr.property.domain.*;
import com.harbr.property.infrastructure.*;
import com.harbr.search.application.ElasticsearchSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final AddressRepository addressRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final AmenityRepository amenityRepository;
    private final PropertyAmenityRepository propertyAmenityRepository;
    private final AvailabilityRuleRepository availabilityRuleRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ElasticsearchSyncService elasticsearchSyncService;

    @Transactional
    public PropertyResponse create(UUID hostId, CreatePropertyRequest request) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new EntityNotFoundException("User", hostId));

        if (host.getRole() == Role.GUEST) {
            throw new ForbiddenException("Only hosts or admins can create properties");
        }

        Address address = addressRepository.save(Address.builder()
                .street(request.address().street())
                .city(request.address().city())
                .state(request.address().state())
                .country(request.address().country())
                .postalCode(request.address().postalCode())
                .lat(request.address().lat())
                .lng(request.address().lng())
                .build());

        Property property = Property.builder()
                .host(host)
                .address(address)
                .title(request.title())
                .description(request.description())
                .propertyType(PropertyType.valueOf(request.propertyType().name()))
                .maxGuests(request.maxGuests())
                .bedrooms(request.bedrooms())
                .bathrooms(request.bathrooms())
                .basePricePerNight(request.basePricePerNight())
                .cleaningFee(request.cleaningFee() != null ? request.cleaningFee() : BigDecimal.ZERO)
                .isInstantBook(request.isInstantBook() != null ? request.isInstantBook() : false)
                .build();

        property = propertyRepository.save(property);

        if (request.amenityIds() != null && !request.amenityIds().isEmpty()) {
            linkAmenities(property, request.amenityIds());
        }

        elasticsearchSyncService.indexProperty(property);

        return toPropertyResponse(property);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PropertyResponse> list(int page, int size) {
        Page<Property> properties = propertyRepository.findByDeletedAtIsNull(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PagedResponse.from(properties.map(this::toPropertyResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<PropertyResponse> search(PropertySearchRequest request) {
        Specification<Property> spec = PropertySpecifications.withSearch(request);
        Page<Property> page = propertyRepository.findAll(spec,
                PageRequest.of(request.page(), request.size(), Sort.by(Sort.Direction.DESC, "createdAt")));
        return PagedResponse.from(page.map(this::toPropertyResponse));
    }

    @Transactional(readOnly = true)
    public PropertyResponse getById(UUID id) {
        Property property = findPropertyOrThrow(id);
        return toPropertyResponse(property);
    }

    @Transactional
    public PropertyResponse update(UUID propertyId, UUID userId, UpdatePropertyRequest request) {
        Property property = findPropertyOrThrow(propertyId);
        verifyOwnerOrAdmin(property, userId);

        if (request.title() != null) property.setTitle(request.title());
        if (request.description() != null) property.setDescription(request.description());
        if (request.propertyType() != null) property.setPropertyType(PropertyType.valueOf(request.propertyType().name()));
        if (request.maxGuests() != null) property.setMaxGuests(request.maxGuests());
        if (request.bedrooms() != null) property.setBedrooms(request.bedrooms());
        if (request.bathrooms() != null) property.setBathrooms(request.bathrooms());
        if (request.basePricePerNight() != null) property.setBasePricePerNight(request.basePricePerNight());
        if (request.cleaningFee() != null) property.setCleaningFee(request.cleaningFee());
        if (request.isInstantBook() != null) property.setIsInstantBook(request.isInstantBook());
        if (request.status() != null) property.setStatus(PropertyStatus.valueOf(request.status()));

        if (request.address() != null) {
            Address addr = property.getAddress();
            addr.setStreet(request.address().street() != null ? request.address().street() : addr.getStreet());
            addr.setCity(request.address().city() != null ? request.address().city() : addr.getCity());
            addr.setState(request.address().state() != null ? request.address().state() : addr.getState());
            addr.setCountry(request.address().country() != null ? request.address().country() : addr.getCountry());
            addr.setPostalCode(request.address().postalCode() != null ? request.address().postalCode() : addr.getPostalCode());
            addr.setLat(request.address().lat() != null ? request.address().lat() : addr.getLat());
            addr.setLng(request.address().lng() != null ? request.address().lng() : addr.getLng());
        }

        if (request.amenityIds() != null) {
            propertyAmenityRepository.deleteAllByPropertyId(propertyId);
            linkAmenities(property, request.amenityIds());
        }

        property = propertyRepository.save(property);
        elasticsearchSyncService.indexProperty(property);
        return toPropertyResponse(property);
    }

    @Transactional
    public void softDelete(UUID propertyId, UUID userId) {
        Property property = findPropertyOrThrow(propertyId);
        verifyOwnerOrAdmin(property, userId);
        property.setDeletedAt(Instant.now());
        propertyRepository.save(property);
        elasticsearchSyncService.removeProperty(propertyId);
    }

    @Transactional
    public PropertyImageResponse addImage(UUID propertyId, UUID userId, MultipartFile file, boolean isCover) {
        Property property = findPropertyOrThrow(propertyId);
        verifyOwnerOrAdmin(property, userId);

        List<PropertyImage> existing = propertyImageRepository.findByPropertyIdOrderByDisplayOrderAsc(propertyId);
        if (isCover) {
            existing.stream().filter(PropertyImage::getIsCover).forEach(img -> {
                img.setIsCover(false);
                propertyImageRepository.save(img);
            });
        }

        String url;
        try {
            url = fileStorageService.store("properties/" + propertyId, file.getOriginalFilename(), file.getInputStream(), file.getContentType());
        } catch (IOException e) {
            throw new RuntimeException("Failed to store image", e);
        }

        PropertyImage image = PropertyImage.builder()
                .property(property)
                .url(url)
                .displayOrder(existing.size())
                .isCover(isCover)
                .build();

        image = propertyImageRepository.save(image);
        return toPropertyImageResponse(image);
    }

    @Transactional
    public void deleteImage(UUID imageId, UUID userId) {
        PropertyImage image = propertyImageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("PropertyImage", imageId));
        verifyOwnerOrAdmin(image.getProperty(), userId);
        fileStorageService.delete(image.getUrl());
        propertyImageRepository.delete(image);
    }

    @Transactional
    public AvailabilityRuleDto addAvailabilityRule(UUID propertyId, UUID userId, CreateAvailabilityRuleRequest request) {
        Property property = findPropertyOrThrow(propertyId);
        verifyOwnerOrAdmin(property, userId);

        AvailabilityRule rule = AvailabilityRule.builder()
                .property(property)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .ruleType(RuleType.valueOf(request.ruleType()))
                .priceOverride(request.priceOverride())
                .minStayNights(request.minStayNights())
                .note(request.note())
                .build();

        rule = availabilityRuleRepository.save(rule);
        return toAvailabilityRuleDto(rule);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityRuleDto> getAvailabilityRules(UUID propertyId) {
        findPropertyOrThrow(propertyId);
        return availabilityRuleRepository.findByPropertyIdOrderByStartDateAsc(propertyId).stream()
                .map(this::toAvailabilityRuleDto)
                .toList();
    }

    private Property findPropertyOrThrow(UUID propertyId) {
        return propertyRepository.findById(propertyId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("Property", propertyId));
    }

    private void verifyOwnerOrAdmin(Property property, UUID userId) {
        if (!property.getHost().getId().equals(userId)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User", userId));
            if (user.getRole() != Role.ADMIN) {
                throw new ForbiddenException("You do not have permission to modify this property");
            }
        }
    }

    private void linkAmenities(Property property, List<UUID> amenityIds) {
        for (UUID amenityId : amenityIds) {
            Amenity amenity = amenityRepository.findById(amenityId)
                    .orElseThrow(() -> new EntityNotFoundException("Amenity", amenityId));
            propertyAmenityRepository.save(PropertyAmenity.builder()
                    .property(property)
                    .amenity(amenity)
                    .build());
        }
    }

    public PropertyResponse toPropertyResponsePublic(Property property) {
        return toPropertyResponse(property);
    }

    private PropertyResponse toPropertyResponse(Property property) {
        List<PropertyImage> images = propertyImageRepository.findByPropertyIdOrderByDisplayOrderAsc(property.getId());
        List<PropertyAmenity> propertyAmenities = propertyAmenityRepository.findByPropertyId(property.getId());

        return new PropertyResponse(
                property.getId(),
                property.getTitle(),
                property.getDescription(),
                property.getPropertyType().name(),
                property.getMaxGuests(),
                property.getBedrooms(),
                property.getBathrooms(),
                property.getBasePricePerNight(),
                property.getCleaningFee(),
                property.getStatus().name(),
                property.getIsInstantBook(),
                property.getCreatedAt(),
                property.getUpdatedAt(),
                property.getHost().getId(),
                property.getHost().getFullName(),
                toAddressDto(property.getAddress()),
                images.stream().map(this::toPropertyImageResponse).toList(),
                propertyAmenities.stream().map(pa -> toAmenityResponse(pa.getAmenity())).toList()
        );
    }

    private AddressDto toAddressDto(Address address) {
        return new AddressDto(
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getCountry(),
                address.getPostalCode(),
                address.getLat(),
                address.getLng()
        );
    }

    private PropertyImageResponse toPropertyImageResponse(PropertyImage image) {
        return new PropertyImageResponse(
                image.getId(),
                image.getUrl(),
                image.getDisplayOrder(),
                image.getIsCover(),
                image.getCreatedAt()
        );
    }

    private AmenityResponse toAmenityResponse(Amenity amenity) {
        return new AmenityResponse(
                amenity.getId(),
                amenity.getName(),
                amenity.getCategory().name(),
                amenity.getIconKey()
        );
    }

    private AvailabilityRuleDto toAvailabilityRuleDto(AvailabilityRule rule) {
        return new AvailabilityRuleDto(
                rule.getId(),
                rule.getStartDate(),
                rule.getEndDate(),
                rule.getRuleType().name(),
                rule.getPriceOverride(),
                rule.getMinStayNights(),
                rule.getNote()
        );
    }
}