package com.tp27.pricing.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    private final Random random = new Random();

    @GetMapping("/book/{bookId}")
    public PriceResponse getPrice(@PathVariable Long bookId) {
        // Simulate pricing logic - random price between 10 and 50
        double price = 10.0 + (40.0 * random.nextDouble());
        return new PriceResponse(bookId, Math.round(price * 100.0) / 100.0);
    }

    public static class PriceResponse {
        private Long bookId;
        private Double price;

        public PriceResponse() {
        }

        public PriceResponse(Long bookId, Double price) {
            this.bookId = bookId;
            this.price = price;
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
