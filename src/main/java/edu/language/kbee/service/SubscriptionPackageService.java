package edu.language.kbee.service;

import edu.language.kbee.payload.request.SubscriptionPackageRequest;
import edu.language.kbee.payload.response.SubscriptionPackageResponse;

import java.util.List;

public interface SubscriptionPackageService {

    List<SubscriptionPackageResponse> getAllPackages();
    
    List<SubscriptionPackageResponse> getActivePackages();

    SubscriptionPackageResponse getPackageById(Long id);

    SubscriptionPackageResponse createPackage(SubscriptionPackageRequest request);

    SubscriptionPackageResponse updatePackage(Long id, SubscriptionPackageRequest request);

    void deletePackage(Long id);
    
    void togglePackageStatus(Long id);
}
