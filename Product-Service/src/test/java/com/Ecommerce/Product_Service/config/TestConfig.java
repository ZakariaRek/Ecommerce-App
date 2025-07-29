package com.Ecommerce.Product_Service.config;

import com.Ecommerce.Product_Service.Services.Kakfa.*;
import org.mockito.Mock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@TestConfiguration
@ActiveProfiles("test")
public class TestConfig {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ProductEventService productEventService;

    @Mock
    private CategoryEventService categoryEventService;

    @Mock
    private InventoryEventService inventoryEventService;

    @Mock
    private DiscountEventService discountEventService;

    @Mock
    private ReviewEventService reviewEventService;

    @Mock
    private SupplierEventService supplierEventService;
}