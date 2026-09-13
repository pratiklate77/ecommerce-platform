package com.ecommerce.user_service.service;

import com.ecommerce.user_service.dto.AddressRequest;
import com.ecommerce.user_service.exception.ResourceNotFoundException;
import com.ecommerce.user_service.model.Address;
import com.ecommerce.user_service.model.Role;
import com.ecommerce.user_service.model.User;
import com.ecommerce.user_service.repository.AddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @InjectMocks
    private AddressService addressService;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserService userService;

    private User user() {
        return User.builder()
                .id(10L)
                .email("alice@example.com")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();
    }

    private AddressRequest request(boolean isDefault) {
        return new AddressRequest("1 Main St", null, "Springfield", "IL", "62701", "USA", isDefault);
    }

    @Test
    void firstAddressIsAutomaticallyDefault() {
        when(userService.findByEmailOrThrow("alice@example.com")).thenReturn(user());
        when(addressRepository.countByUserId(10L)).thenReturn(0L);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        addressService.add("alice@example.com", request(false));

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertTrue(captor.getValue().isDefault());
    }

    @Test
    void nonDefaultNotFirstAddressKeepsExistingDefaults() {
        when(userService.findByEmailOrThrow("alice@example.com")).thenReturn(user());
        when(addressRepository.countByUserId(10L)).thenReturn(2L);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        addressService.add("alice@example.com", request(false));

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertFalse(captor.getValue().isDefault());
        verify(addressRepository, never()).findByUserId(anyLong());
    }

    @Test
    void makingAnAddressDefaultClearsOtherDefaults() {
        when(userService.findByEmailOrThrow("alice@example.com")).thenReturn(user());

        Address existingDefault = Address.builder()
                .id(1L)
                .user(user())
                .isDefault(true)
                .build();
        when(addressRepository.findByUserId(10L)).thenReturn(List.of(existingDefault));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        addressService.add("alice@example.com", request(true));

        assertFalse(existingDefault.isDefault());
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertTrue(captor.getValue().isDefault());
        assertEquals("1 Main St", captor.getValue().getLine1());
    }

    @Test
    void updateThrowsWhenAddressDoesNotBelongToUser() {
        when(userService.findByEmailOrThrow("alice@example.com")).thenReturn(user());
        when(addressRepository.findByIdAndUserId(99L, 10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> addressService.update("alice@example.com", 99L, request(false)));
    }

    @Test
    void deleteThrowsWhenAddressDoesNotBelongToUser() {
        when(userService.findByEmailOrThrow("alice@example.com")).thenReturn(user());
        when(addressRepository.findByIdAndUserId(99L, 10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> addressService.delete("alice@example.com", 99L));
    }
}