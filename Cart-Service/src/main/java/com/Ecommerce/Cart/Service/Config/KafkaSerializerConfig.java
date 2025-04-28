//package com.Ecommerce.Cart.Service.Config;
//
//import com.Ecommerce.Cart.Service.Events.CartEvent;
//import com.Ecommerce.Cart.Service.Events.ProductPriceUpdateEvent;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.core.ProducerFactory;
//
//@Configuration
//public class KafkaSerializerConfig {
//
//    /**
//     * KafkaTemplate for sending CartEvent objects
//     */
//    @Bean
//    public KafkaTemplate<String, CartEvent> cartEventKafkaTemplate(
//            ProducerFactory<String, CartEvent> producerFactory) {
//        return new KafkaTemplate<>(producerFactory);
//    }
//
//    /**
//     * KafkaTemplate for sending ProductPriceUpdateEvent objects
//     */
//    @Bean
//    public KafkaTemplate<String, ProductPriceUpdateEvent> productPriceUpdateKafkaTemplate(
//            ProducerFactory<String, ProductPriceUpdateEvent> producerFactory) {
//        return new KafkaTemplate<>(producerFactory);
//    }
//}