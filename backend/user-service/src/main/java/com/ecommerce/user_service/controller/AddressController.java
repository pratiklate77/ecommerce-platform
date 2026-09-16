package com.ecommerce.user_service.controller;

import com.ecommerce.user_service.dto.AddressRequest;
import com.ecommerce.user_service.dto.AddressResponse;
import com.ecommerce.user_service.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Address book of the currently authenticated user. Each user may have many
 * addresses; at most one can be the default.
 */
@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<AddressResponse> add(@AuthenticationPrincipal UserDetails principal,
                                               @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addressService.add(principal.getUsername(), request));
    }

    @GetMapping
    public List<AddressResponse> list(@AuthenticationPrincipal UserDetails principal) {
        System.out.println("The user info is " + principal);
        return addressService.list(principal.getUsername());
    }

    @PutMapping("/{id}")
    public AddressResponse update(@AuthenticationPrincipal UserDetails principal,
                                  @PathVariable Long id,
                                  @Valid @RequestBody AddressRequest request) {
        return addressService.update(principal.getUsername(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails principal,
                                       @PathVariable Long id) {
        addressService.delete(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}