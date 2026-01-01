package com.tp27.book.dto;

public class BorrowResponse {
    private Long bookId;
    private String title;
    private Integer stockLeft;
    private Double price;
    private String message;

    public BorrowResponse() {
    }

    public BorrowResponse(Long bookId, String title, Integer stockLeft, Double price, String message) {
        this.bookId = bookId;
        this.title = title;
        this.stockLeft = stockLeft;
        this.price = price;
        this.message = message;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getStockLeft() {
        return stockLeft;
    }

    public void setStockLeft(Integer stockLeft) {
        this.stockLeft = stockLeft;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
