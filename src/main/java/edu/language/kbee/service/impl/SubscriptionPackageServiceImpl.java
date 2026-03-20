package edu.language.kbee.service.impl;

import edu.language.kbee.payload.request.SubscriptionPackageRequest;
import edu.language.kbee.payload.response.SubscriptionPackageResponse;
import edu.language.kbee.entity.SubscriptionPackage;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.repository.SubscriptionPackageRepository;
import edu.language.kbee.service.SubscriptionPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionPackageServiceImpl implements SubscriptionPackageService {

    private final SubscriptionPackageRepository repository;

    @Override
    public List<SubscriptionPackageResponse> getAllPackages() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubscriptionPackageResponse> getActivePackages() {
        return repository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SubscriptionPackageResponse getPackageById(Long id) {
        SubscriptionPackage sp = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription Package not found with id: " + id));
        return mapToResponse(sp);
    }

    @Override
    @Transactional
    public SubscriptionPackageResponse createPackage(SubscriptionPackageRequest request) {
        if (repository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Package name already exists");
        }

        SubscriptionPackage sp = SubscriptionPackage.builder()
                .name(request.getName())
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .durationDays(request.getDurationDays())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .features(request.getFeatures())
                .build();

        SubscriptionPackage saved = repository.save(sp);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public SubscriptionPackageResponse updatePackage(Long id, SubscriptionPackageRequest request) {
        SubscriptionPackage sp = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription Package not found with id: " + id));

        // Check if updating to a name that already exists in another package
        repository.findByName(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Package name already exists");
            }
        });

        sp.setName(request.getName());
        sp.setPrice(request.getPrice());
        sp.setOriginalPrice(request.getOriginalPrice());
        sp.setDurationDays(request.getDurationDays());
        sp.setIsActive(request.getIsActive() != null ? request.getIsActive() : sp.getIsActive());
        
        // Clear and add to ensure the elements are updated
        sp.getFeatures().clear();
        if (request.getFeatures() != null) {
            sp.getFeatures().addAll(request.getFeatures());
        }

        SubscriptionPackage updated = repository.save(sp);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deletePackage(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Subscription Package not found with id: " + id);
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public void togglePackageStatus(Long id) {
        SubscriptionPackage sp = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription Package not found with id: " + id));
        sp.setIsActive(!sp.getIsActive());
        repository.save(sp);
    }

    private SubscriptionPackageResponse mapToResponse(SubscriptionPackage entity) {
        return SubscriptionPackageResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .price(entity.getPrice())
                .originalPrice(entity.getOriginalPrice())
                .durationDays(entity.getDurationDays())
                .isActive(entity.getIsActive())
                .features(entity.getFeatures())
                .build();
    }
}
