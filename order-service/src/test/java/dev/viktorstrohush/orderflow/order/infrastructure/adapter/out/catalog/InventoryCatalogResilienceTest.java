package dev.viktorstrohush.orderflow.order.infrastructure.adapter.out.catalog;

import dev.viktorstrohush.orderflow.order.application.port.out.CatalogUnavailableException;
import dev.viktorstrohush.orderflow.order.application.port.out.ProductCatalog;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Retries and circuit breaker (spec 009). They are applied by a proxy, so this test needs a Spring
 * context, but a minimal one: the adapter, a fake inventory-service and the three
 * auto-configurations that create the proxy. No database and no Kafka. The values under test are
 * the real ones of application.yml, except the wait between attempts.
 */
@SpringBootTest(
        classes = {InventoryCatalogClient.class, InventoryCatalogResilienceTest.FakeInventory.class},
        properties = "resilience4j.retry.instances.inventory.wait-duration=1ms")
@ImportAutoConfiguration({AopAutoConfiguration.class, RetryAutoConfiguration.class, CircuitBreakerAutoConfiguration.class})
class InventoryCatalogResilienceTest {

    static final String URL = "http://inventory/api/products?skus=KB-01";
    static final Set<String> SKUS = Set.of("KB-01");

    @TestConfiguration
    static class FakeInventory {

        private final RestClient.Builder builder = RestClient.builder().baseUrl("http://inventory");

        @Bean
        MockRestServiceServer inventory() {
            return MockRestServiceServer.bindTo(builder).build();
        }

        /** The parameter is not used: it makes Spring bind the fake server before building the client. */
        @Bean
        RestClient inventoryRestClient(MockRestServiceServer inventory) {
            return builder.build();
        }
    }

    /** Through the port, as OrderService does: that is the proxy, not the bare class. */
    @Autowired
    ProductCatalog catalog;
    @Autowired
    MockRestServiceServer inventory;
    @Autowired
    CircuitBreakerRegistry circuitBreakers;

    @BeforeEach
    void startFromAClosedCircuit() {
        inventory.reset();
        circuitBreakers.circuitBreaker(InventoryCatalogClient.INVENTORY).reset();
    }

    @Test
    void aServerErrorIsAttemptedThreeTimesAndEndsInThePortsException() {
        inventory.expect(times(3), requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> catalog.pricesFor(SKUS)).isInstanceOf(CatalogUnavailableException.class);

        inventory.verify();
    }

    @Test
    void aConnectionFailureIsRetriedToo() {
        inventory.expect(times(3), requestTo(URL)).andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> catalog.pricesFor(SKUS)).isInstanceOf(CatalogUnavailableException.class);

        inventory.verify();
    }

    @Test
    void ifTheSecondAttemptSucceedsTheCallerNeverKnowsAboutTheFirst() {
        inventory.expect(once(), requestTo(URL)).andRespond(withServerError());
        inventory.expect(once(), requestTo(URL)).andRespond(withSuccess("""
                [{"sku":"KB-01","price":49.90}]
                """, MediaType.APPLICATION_JSON));

        Map<String, BigDecimal> prices = catalog.pricesFor(SKUS);

        assertThat(prices).containsEntry("KB-01", new BigDecimal("49.90"));
        inventory.verify();
    }

    /** A second request would fail the test: the fake server expects exactly one. */
    @Test
    void aClientErrorIsAttemptedOnlyOnce() {
        inventory.expect(once(), requestTo(URL)).andRespond(withBadRequest());

        assertThatThrownBy(() -> catalog.pricesFor(SKUS)).isInstanceOf(CatalogUnavailableException.class);

        inventory.verify();
    }

    /**
     * Two failed orders are six attempts. The fifth failure reaches the minimum of calls with 100%
     * of failures and opens the circuit, so the sixth attempt and the whole third order are answered
     * without calling inventory-service: it receives 5 requests, not 9.
     */
    @Test
    void afterRepeatedFailuresTheCircuitOpensAndInventoryIsNotCalledAnyMore() {
        inventory.expect(times(5), requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> catalog.pricesFor(SKUS)).isInstanceOf(CatalogUnavailableException.class);
        assertThatThrownBy(() -> catalog.pricesFor(SKUS)).isInstanceOf(CatalogUnavailableException.class);

        assertThat(circuitBreakers.circuitBreaker(InventoryCatalogClient.INVENTORY).getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
        assertThatThrownBy(() -> catalog.pricesFor(SKUS))
                .isInstanceOf(CatalogUnavailableException.class)
                .hasCauseInstanceOf(CallNotPermittedException.class);
        inventory.verify();
    }
}
