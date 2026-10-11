package dev.viktorstrohush.orderflow.order.infrastructure.adapter.out.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The HTTP code of the adapter, without Spring and without a network: MockRestServiceServer takes
 * the place of inventory-service. It checks what is sent and how the answer is read. Retries and
 * circuit breaker need the proxy, so they are in {@link InventoryCatalogResilienceTest}.
 */
class InventoryCatalogClientTest {

    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://inventory");
    // bindTo replaces the transport of the builder: it has to happen before build().
    private final MockRestServiceServer inventory = MockRestServiceServer.bindTo(builder).build();
    private final InventoryCatalogClient client = new InventoryCatalogClient(builder.build());

    @Test
    void asksForTheSkusAndReturnsThePriceOfEachOne() {
        inventory.expect(requestTo("http://inventory/api/products?skus=KB-01"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"sku":"KB-01","name":"Mechanical keyboard","price":49.90,"availableQuantity":7}]
                        """, MediaType.APPLICATION_JSON));

        Map<String, BigDecimal> prices = client.pricesFor(Set.of("KB-01"));

        assertThat(prices).containsOnly(Map.entry("KB-01", new BigDecimal("49.90")));
        inventory.verify();
    }

    @Test
    void aSkuThatIsNotInTheCatalogIsNotInTheMap() {
        inventory.expect(requestTo("http://inventory/api/products?skus=XX-99"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.pricesFor(Set.of("XX-99"))).isEmpty();
    }

    /** inventory-service can add fields to its response without breaking this service. */
    @Test
    void ignoresTheFieldsItDoesNotKnow() {
        inventory.expect(requestTo("http://inventory/api/products?skus=KB-01"))
                .andRespond(withSuccess("""
                        [{"sku":"KB-01","price":49.90,"category":"keyboards","tags":["new"]}]
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.pricesFor(Set.of("KB-01"))).containsKey("KB-01");
    }
}
