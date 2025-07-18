package com.Ecommerce.Cart.Service.Models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "saved_for_later")
@JsonIgnoreProperties(ignoreUnknown = true)  // Ignore unknown properties during deserialization
public class SavedForLater {
    @Id
    private UUID id;
    private UUID userId;
    private UUID productId;
    private LocalDateTime savedAt;


}