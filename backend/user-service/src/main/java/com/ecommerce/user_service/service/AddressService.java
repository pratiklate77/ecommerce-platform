package com.ecommerce.user_service.service;

import com.ecommerce.user_service.dto.AddressRequest;
import com.ecommerce.user_service.dto.AddressResponse;
import com.ecommerce.user_service.exception.ResourceNotFoundException;
import com.ecommerce.user_service.model.Address;
import com.ecommerce.user_service.model.User;
import com.ecommerce.user_service.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserService userService;

    @Transactional
    public AddressResponse add(String email, AddressRequest request) {
        User user = userService.findByEmailOrThrow(email.toLowerCase());

        boolean makeDefault = request.isDefault() || addressRepository.countByUserId(user.getId()) == 0;
        if (makeDefault) {
            clearDefault(user);
        }

        Address address = Address.builder()
                .user(user)
                .line1(request.line1())
                .line2(request.line2())
                .city(request.city())
                .state(request.state())
                .postalCode(request.postalCode())
                .country(request.country())
                .isDefault(makeDefault)
                .build();

        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(String email) {
        User user = userService.findByEmailOrThrow(email.toLowerCase());
        return addressRepository.findByUserId(user.getId()).stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Transactional
    public AddressResponse update(String email, Long id, AddressRequest request) {
        User user = userService.findByEmailOrThrow(email.toLowerCase());
        Address address = addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + id));

        address.setLine1(request.line1());
        address.setLine2(request.line2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());
        address.setDefault(request.isDefault());

        if (request.isDefault()) {
            clearDefault(user);
            address.setDefault(true);
        }

        return AddressResponse.from(address);
    }

    @Transactional
    public void delete(String email, Long id) {
        User user = userService.findByEmailOrThrow(email.toLowerCase());
        Address address = addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + id));
        addressRepository.delete(address);
    }

    private void clearDefault(User user) {
        addressRepository.findByUserId(user.getId()).stream()
                .filter(Address::isDefault)
                .forEach(address -> address.setDefault(false));
    }
}