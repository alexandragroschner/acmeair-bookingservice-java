package com.acmeair.client;

public class PriceResponse {
    private Long price;

    public PriceResponse(Long price) {
        this.setPrice(price);
    }

    public PriceResponse() {
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }
}
