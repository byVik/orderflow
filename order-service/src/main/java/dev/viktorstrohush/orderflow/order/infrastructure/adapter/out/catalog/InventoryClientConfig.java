package dev.viktorstrohush.orderflow.order.infrastructure.adapter.out.catalog;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * Builds the HTTP client for inventory-service. It lives apart from the adapter so that a test can
 * hand the adapter a RestClient connected to a fake server.
 */
@Configuration
@EnableConfigurationProperties(InventoryProperties.class)
class InventoryClientConfig {

    /** The builder comes from Spring Boot already set up (Jackson); here it gets the URL and the timeouts. */
    @Bean
    RestClient inventoryRestClient(RestClient.Builder builder, InventoryProperties properties) {
        // Without timeouts, a slow inventory-service would leave the order requests hanging.
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build());
        requestFactory.setReadTimeout(properties.readTimeout());
        return builder.baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
    }
}
