//package com.Ecommerce.User_Service.Services.Kafka;
//
//import com.Ecommerce.User_Service.Config.KafkaConfig;
//import com.Ecommerce.User_Service.Models.User;
//import com.Ecommerce.User_Service.Models.Role;
//import com.Ecommerce.User_Service.Models.ERole;
//import com.Ecommerce.User_Service.Models.UserStatus;
//import com.Ecommerce.User_Service.Models.UserAddress;
//import com.Ecommerce.User_Service.Models.AddressType;
//import com.Ecommerce.User_Service.Events.UserEvents;
//import com.Ecommerce.User_Service.Events.UserAddressEvents;
//import com.Ecommerce.User_Service.Events.RoleEvents;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.apache.kafka.clients.consumer.ConsumerRecords;
//import org.apache.kafka.clients.consumer.KafkaConsumer;
//import org.apache.kafka.common.serialization.StringDeserializer;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.kafka.test.EmbeddedKafkaBroker;
//import org.springframework.kafka.test.context.EmbeddedKafka;
//import org.springframework.kafka.test.utils.KafkaTestUtils;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.time.Duration;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//// Solution 1: Use @DirtiesContext and proper Spring Boot Test configuration
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
//@EmbeddedKafka(
//        partitions = 1,
//        controlledShutdown = false,
//        brokerProperties = {
//                "listeners=PLAINTEXT://localhost:0", // Use port 0 for automatic port assignment
//                "auto.create.topics.enable=true"
//        },
//        topics = {
//                KafkaConfig.TOPIC_USER_CREATED,
//                KafkaConfig.TOPIC_USER_UPDATED,
//                KafkaConfig.TOPIC_USER_DELETED,
//                KafkaConfig.TOPIC_USER_STATUS_CHANGED,
//                KafkaConfig.TOPIC_USER_ADDRESS_CREATED,
//                KafkaConfig.TOPIC_USER_ROLE_CHANGED,
//                KafkaConfig.TOPIC_ROLE_CREATED
//        }
//)
//@ActiveProfiles("test")
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//class KafkaEventServiceIntegrationTest {
//
//    @Autowired
//    private EmbeddedKafkaBroker embeddedKafkaBroker;
//
//    @Autowired
//    private UserEventService userEventService;
//
//    @Autowired
//    private UserAddressEventService userAddressEventService;
//
//    @Autowired
//    private RoleEventService roleEventService;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    private KafkaConsumer<String, String> testConsumer;
//    private User testUser;
//    private UserAddress testAddress;
//    private Role testRole;
//
//    @BeforeEach
//    void setUp() {
//        // Setup test consumer with proper configuration
//        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group-" + System.currentTimeMillis(), "true", embeddedKafkaBroker);
//        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
//        consumerProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 100);
//
//        testConsumer = new KafkaConsumer<>(consumerProps);
//
//        // Setup test data
//        testRole = new Role();
//        testRole.setId("role123");
//        testRole.setName(ERole.ROLE_USER);
//
//        testUser = new User();
//        testUser.setId("user123");
//        testUser.setUsername("testuser");
//        testUser.setEmail("testuser@test.com");
//        testUser.setStatus(UserStatus.ACTIVE);
//        Set<Role> roles = new HashSet<>();
//        roles.add(testRole);
//        testUser.setRoles(roles);
//
//        testAddress = new UserAddress();
//        testAddress.setId("addr123");
//        testAddress.setUserId(testUser.getId());
//        testAddress.setAddressType(AddressType.HOME);
//        testAddress.setStreet("123 Test St");
//        testAddress.setCity("Test City");
//        testAddress.setState("Test State");
//        testAddress.setCountry("USA");
//        testAddress.setZipCode("12345");
//        testAddress.setDefault(true);
//    }
//
//    @AfterEach
//    void tearDown() {
//        if (testConsumer != null) {
//            testConsumer.close();
//        }
//    }
//
//    @Test
//    void shouldPublishUserCreatedEvent() throws Exception {
//        // Given
//        testConsumer.subscribe(Collections.singletonList(KafkaConfig.TOPIC_USER_CREATED));
//
//        // When
//        userEventService.publishUserCreatedEvent(testUser);
//
//        // Then
//        ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofSeconds(10));
//        assertThat(records).isNotEmpty();
//
//        ConsumerRecord<String, String> record = records.iterator().next();
//        assertThat(record.key()).isEqualTo(testUser.getId());
//
//        UserEvents.UserCreatedEvent event = objectMapper.readValue(record.value(), UserEvents.UserCreatedEvent.class);
//        assertThat(event.getUserId()).isEqualTo(testUser.getId());
//        assertThat(event.getUsername()).isEqualTo(testUser.getUsername());
//        assertThat(event.getEmail()).isEqualTo(testUser.getEmail());
//        assertThat(event.getStatus()).isEqualTo(testUser.getStatus());
//        assertThat(event.getRoles()).contains("ROLE_USER");
//        assertThat(event.getCreatedAt()).isNotNull();
//    }
//
//    @Test
//    void shouldPublishUserUpdatedEvent() throws Exception {
//        // Given
//        testConsumer.subscribe(Collections.singletonList(KafkaConfig.TOPIC_USER_UPDATED));
//
//        // When
//        userEventService.publishUserUpdatedEvent(testUser);
//
//        // Then
//        ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofSeconds(10));
//        assertThat(records).isNotEmpty();
//
//        ConsumerRecord<String, String> record = records.iterator().next();
//        assertThat(record.key()).isEqualTo(testUser.getId());
//
//        UserEvents.UserUpdatedEvent event = objectMapper.readValue(record.value(), UserEvents.UserUpdatedEvent.class);
//        assertThat(event.getUserId()).isEqualTo(testUser.getId());
//        assertThat(event.getUsername()).isEqualTo(testUser.getUsername());
//        assertThat(event.getEmail()).isEqualTo(testUser.getEmail());
//        assertThat(event.getUpdatedAt()).isNotNull();
//    }
//
//    @Test
//    void shouldPublishUserDeletedEvent() throws Exception {
//        // Given
//        testConsumer.subscribe(Collections.singletonList(KafkaConfig.TOPIC_USER_DELETED));
//
//        // When
//        userEventService.publishUserDeletedEvent(testUser);
//
//        // Then
//        ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofSeconds(10));
//        assertThat(records).isNotEmpty();
//
//        ConsumerRecord<String, String> record = records.iterator().next();
//        assertThat(record.key()).isEqualTo(testUser.getId());
//
//        UserEvents.UserDeletedEvent event = objectMapper.readValue(record.value(), UserEvents.UserDeletedEvent.class);
//        assertThat(event.getUserId()).isEqualTo(testUser.getId());
//        assertThat(event.getUsername()).isEqualTo(testUser.getUsername());
//        assertThat(event.getEmail()).isEqualTo(testUser.getEmail());
//        assertThat(event.getDeletedAt()).isNotNull();
//    }
//
//    @Test
//    void shouldPublishUserStatusChangedEvent() throws Exception {
//        // Given
//        testConsumer.subscribe(Collections.singletonList(KafkaConfig.TOPIC_USER_STATUS_CHANGED));
//        UserStatus previousStatus = UserStatus.INACTIVE;
//
//        // When
//        userEventService.publishUserStatusChangedEvent(testUser, previousStatus);
//
//        // Then
//        ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofSeconds(10));
//        assertThat(records).isNotEmpty();
//
//        ConsumerRecord<String, String> record = records.iterator().next();
//        assertThat(record.key()).isEqualTo(testUser.getId());
//
//        UserEvents.UserStatusChangedEvent event = objectMapper.readValue(record.value(), UserEvents.UserStatusChangedEvent.class);
//        assertThat(event.getUserId()).isEqualTo(testUser.getId());
//        assertThat(event.getUsername()).isEqualTo(testUser.getUsername());
//        assertThat(event.getPreviousStatus()).isEqualTo(previousStatus);
//        assertThat(event.getNewStatus()).isEqualTo(testUser.getStatus());
//        assertThat(event.getUpdatedAt()).isNotNull();
//    }
//
//    @Test
//    void shouldPublishUserAddressCreatedEvent() throws Exception {
//        // Given
//        testConsumer.subscribe(Collections.singletonList(KafkaConfig.TOPIC_USER_ADDRESS_CREATED));
//
//        // When
//        userAddressEventService.publishUserAddressCreatedEvent(testAddress);
//
//        // Then
//        ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofSeconds(10));
//        assertThat(records).isNotEmpty();
//
//        ConsumerRecord<String, String> record = records.iterator().next();
//        assertThat(record.key()).isEqualTo(testAddress.getId());
//
//        UserAddressEvents.UserAddressCreatedEvent event = objectMapper.readValue(record.value(), UserAddressEvents.UserAddressCreatedEvent.class);
//        assertThat(event.getAddressId()).isEqualTo(testAddress.getId());
//        assertThat(event.getUserId()).isEqualTo(testAddress.getUserId());
//        assertThat(event.getAddressType()).isEqualTo(testAddress.getAddressType());
//        assertThat(event.getStreet()).isEqualTo(testAddress.getStreet());
//        assertThat(event.getCity()).isEqualTo(testAddress.getCity());
//        assertThat(event.isDefault()).isEqualTo(testAddress.isDefault());
//        assertThat(event.getCreatedAt()).isNotNull();
//    }
//
//    @Test
//    void shouldPublishRoleCreatedEvent() throws Exception {
//        // Given
//        testConsumer.subscribe(Collections.singletonList(KafkaConfig.TOPIC_ROLE_CREATED));
//
//        // When
//        roleEventService.publishRoleCreatedEvent(testRole);
//
//        // Then
//        ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofSeconds(10));
//        assertThat(records).isNotEmpty();
//
//        ConsumerRecord<String, String> record = records.iterator().next();
//        assertThat(record.key()).isEqualTo(testRole.getId());
//
//        RoleEvents.RoleCreatedEvent event = objectMapper.readValue(record.value(), RoleEvents.RoleCreatedEvent.class);
//        assertThat(event.getRoleId()).isEqualTo(testRole.getId());
//        assertThat(event.getRoleName()).isEqualTo(testRole.getName());
//        assertThat(event.getCreatedAt()).isNotNull();
//    }
//
//    @Test
//    void shouldPublishUserRoleChangedEvent() throws Exception {
//        // Given
//        testConsumer.subscribe(Collections.singletonList(KafkaConfig.TOPIC_USER_ROLE_CHANGED));
//        Set<String> previousRoles = Set.of("ROLE_GUEST");
//
//        // When
//        userEventService.publishUserRoleChangedEvent(testUser, previousRoles);
//
//        // Then
//        ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofSeconds(10));
//        assertThat(records).isNotEmpty();
//
//        ConsumerRecord<String, String> record = records.iterator().next();
//        assertThat(record.key()).isEqualTo(testUser.getId());
//
//        UserEvents.UserRoleChangedEvent event = objectMapper.readValue(record.value(), UserEvents.UserRoleChangedEvent.class);
//        assertThat(event.getUserId()).isEqualTo(testUser.getId());
//        assertThat(event.getUsername()).isEqualTo(testUser.getUsername());
//        assertThat(event.getPreviousRoles()).containsExactly("ROLE_GUEST");
//        assertThat(event.getNewRoles()).contains("ROLE_USER");
//        assertThat(event.getUpdatedAt()).isNotNull();
//    }
//
//    @Test
//    void shouldHandleKafkaPublishingErrors() {
//        // Given - Invalid user with minimal data
//        User invalidUser = new User();
//        invalidUser.setId("invalid");
//
//        // When/Then - Should not throw exception
//        try {
//            userEventService.publishUserCreatedEvent(invalidUser);
//            // If we reach here, the service handled the error gracefully
//            assertThat(true).isTrue();
//        } catch (Exception e) {
//            // If an exception is thrown, it should be a controlled exception from the service
//            assertThat(e.getMessage()).contains("Failed to publish");
//        }
//    }
//}