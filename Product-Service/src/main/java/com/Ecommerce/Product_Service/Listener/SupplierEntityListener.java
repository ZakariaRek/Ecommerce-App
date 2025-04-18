package com.Ecommerce.Product_Service.Listener;

import com.Ecommerce.Product_Service.Entities.Supplier;
import com.Ecommerce.Product_Service.Services.Kakfa.SupplierEventService;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class SupplierEntityListener {

    private static SupplierEventService supplierEventService;

    @Autowired
    public void setSupplierEventService(SupplierEventService supplierEventService) {
        SupplierEntityListener.supplierEventService = supplierEventService;
    }

    @PostPersist
    public void postPersist(Supplier supplier) {
        if (supplierEventService != null) {
            supplierEventService.publishSupplierCreatedEvent(supplier);
        }
    }

    @PostUpdate
    public void postUpdate(Supplier supplier) {
        if (supplierEventService != null) {
            // Basic update event
            supplierEventService.publishSupplierUpdatedEvent(supplier);

            // The @PostUpdate doesn't provide the previous state
            // In a real application, you would need to use @PreUpdate to capture previous values
            // and store them in thread-local variables or a similar mechanism
            // This is just a placeholder to show how specific events would be triggered

            // Example of how you might handle specific property change events:
            // if (previousRating != null && !previousRating.equals(supplier.getRating())) {
            //     supplierEventService.publishSupplierRatingChangedEvent(supplier, previousRating);
            // }

            // if (previousContractDetails != null && !previousContractDetails.equals(supplier.getContractDetails())) {
            //     supplierEventService.publishSupplierContractUpdatedEvent(supplier, previousContractDetails);
            // }
        }
    }

    @PostRemove
    public void postRemove(Supplier supplier) {
        if (supplierEventService != null) {
            supplierEventService.publishSupplierDeletedEvent(supplier);
        }
    }

    // In a real application, you would add methods to capture previous state
    // For example:

    @PreUpdate
    public void preUpdate(Supplier supplier) {
        // In a real application, you would store the current state before the update
        // For example, using a ThreadLocal variable or similar mechanism
        // ThreadLocalContext.setPreviousRating(supplier.getRating());
        // ThreadLocalContext.setPreviousContractDetails(supplier.getContractDetails());
    }
}