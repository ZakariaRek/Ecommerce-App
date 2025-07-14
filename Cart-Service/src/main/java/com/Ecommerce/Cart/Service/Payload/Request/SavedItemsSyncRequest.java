//package com.Ecommerce.Cart.Service.Payload.Request;
//
//import jakarta.validation.Valid;
//import jakarta.validation.constraints.NotNull;
//import lombok.*;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class SavedItemsSyncRequest {
//    @Valid
//    @NotNull
//    private List<LocalStorageSavedItem> items;
//
//    @Builder.Default
//    private ConflictResolutionStrategy conflictStrategy = ConflictResolutionStrategy.KEEP_LATEST;
//
//    private LocalDateTime lastUpdated;
//    private String deviceId;
//    private String sessionId;
//}