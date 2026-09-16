package com.ecommerce.order_service.dto;

import com.ecommerce.order_service.model.ShippingAddress;

public record ShippingAddressResponse(
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country
) {
    public static ShippingAddressResponse from(ShippingAddress address) {
        if (address == null) {
            return null;
        }
        return new ShippingAddressResponse(
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry()
        );
    }
}