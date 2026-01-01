package com.tp27.book.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PricingClient {

    private static final Logger logger = LoggerFactory.getLogger(PricingClient.class);

    @Value("${pricing.service.url}")
    private String pricingServiceUrl;

    private final RestTemplate restTemplate;

    public PricingClient() {
        this.restTemplate = new RestTemplate();
    }

    @CircuitBreaker(name = "pricingService", fallbackMethod = "getPriceFallback")
    @Retry(name = "pricingService")
    public Double getPrice(Long bookId) {
        logger.info("Calling pricing service for bookId: {}", bookId);
        String url = pricingServiceUrl + "/api/pricing/book/" + bookId;
        PriceResponse response = restTemplate.getForObject(url, PriceResponse.class);
        return response != null ? response.getPrice() : 0.0;
    }

    public Double getPriceFallback(Long bookId, Throwable ex) {
        logger.warn("Fallback triggered for bookId: {}. Reason: {}", bookId, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        return 0.0; // Default price when pricing service is down
    }

    public static class PriceResponse {
        private Long bookId;
        private Double price;

        public PriceResponse() {
        }

        public Long getBookId() {
            return bookId;
        }

        public void setBookId(Long bookId) {
            this.bookId = bookId;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }
    }
}
