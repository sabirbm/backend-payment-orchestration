package com.yuno.payment.engine;

import com.yuno.payment.connector.PaymentProviderConnector;
import com.yuno.payment.model.PaymentMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Routing Engine: Determines which provider to use based on payment method.
 *
 * Routing Logic:
 * - CARD → ProviderA (primary)
 * - UPI → ProviderB (primary)
 *
 * Failover: If primary provider is unavailable, attempt secondary connectors
 * (for now, we only have one per method, but structure allows for future expansion).
 */
@Component
public class RoutingEngine {

    private static final Logger log = LoggerFactory.getLogger(RoutingEngine.class);

    private final Map<PaymentMethod, List<PaymentProviderConnector>> methodToConnectors;

    public RoutingEngine(List<PaymentProviderConnector> connectors) {
        // Group connectors by their supported payment method
        this.methodToConnectors = connectors.stream()
                .collect(Collectors.groupingBy(PaymentProviderConnector::getSupportedMethod));

        log.info("Routing engine initialized with {} payment methods", methodToConnectors.size());
        methodToConnectors.forEach((method, providers) ->
                log.info("  {} → {} providers", method, providers.stream()
                        .map(PaymentProviderConnector::getProviderName)
                        .collect(Collectors.joining(", ")))
        );
    }

    /**
     * Routes a payment to an available provider based on its payment method.
     *
     * @param paymentMethod The payment method (CARD or UPI)
     * @return The assigned connector, or null if no available providers exist
     */
    public PaymentProviderConnector route(PaymentMethod paymentMethod) {
        List<PaymentProviderConnector> providers = methodToConnectors.get(paymentMethod);

        if (providers == null || providers.isEmpty()) {
            log.error("No providers configured for payment method: {}", paymentMethod);
            return null;
        }

        // Try to find the first available provider (in practice, load balance across multiple)
        for (PaymentProviderConnector provider : providers) {
            if (provider.isAvailable()) {
                log.debug("Routing {} payment to {}", paymentMethod, provider.getProviderName());
                return provider;
            }
        }

        // No available providers for this method
        log.warn("All providers unavailable for payment method: {}", paymentMethod);
        return null;
    }

    /**
     * Get all connectors for a given payment method.
     */
    public List<PaymentProviderConnector> getConnectors(PaymentMethod paymentMethod) {
        return methodToConnectors.getOrDefault(paymentMethod, List.of());
    }
}
