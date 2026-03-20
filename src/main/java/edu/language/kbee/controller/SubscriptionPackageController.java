package edu.language.kbee.controller;


import edu.language.kbee.payload.request.SubscriptionPackageRequest;
import edu.language.kbee.payload.response.SubscriptionPackageResponse;
import edu.language.kbee.service.SubscriptionPackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionPackageController {

    private final SubscriptionPackageService service;

    @GetMapping
    public ResponseEntity<List<SubscriptionPackageResponse>> getAllPackages(
            @RequestParam(required = false, defaultValue = "false") boolean onlyActive) {
        List<SubscriptionPackageResponse> packages = onlyActive 
                ? service.getActivePackages() 
                : service.getAllPackages();
        return ResponseEntity.ok(packages);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionPackageResponse> getPackageById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getPackageById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionPackageResponse> createPackage(
            @Valid @RequestBody SubscriptionPackageRequest request) {
        SubscriptionPackageResponse created = service.createPackage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionPackageResponse> updatePackage(
            @PathVariable Long id, 
            @Valid @RequestBody SubscriptionPackageRequest request) {
        SubscriptionPackageResponse updated = service.updatePackage(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePackage(@PathVariable Long id) {
        service.deletePackage(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> togglePackageStatus(@PathVariable Long id) {
        service.togglePackageStatus(id);
        return ResponseEntity.ok().build();
    }
}
