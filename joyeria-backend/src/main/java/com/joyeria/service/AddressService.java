package com.joyeria.service;

import com.joyeria.dto.*;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.Address;
import com.joyeria.model.User;
import com.joyeria.repository.AddressRepository;
import com.joyeria.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public List<AddressDTO> getUserAddresses(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return addressRepository.findByUserId(user.getId()).stream().map(this::mapToDTO).toList();
    }

    @Transactional
    public AddressDTO createAddress(String email, AddressCreateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.findByUserId(user.getId()).forEach(a -> {
                a.setIsDefault(false);
                addressRepository.save(a);
            });
        }

        Address address = new Address();
        address.setUser(user);
        address.setFirstName(request.getFirstName());
        address.setLastName(request.getLastName());
        address.setPhone(request.getPhone());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setDepartment(request.getDepartment());
        address.setPostalCode(request.getPostalCode());
        address.setAddressType(request.getAddressType());
        address.setIsDefault(request.getIsDefault());

        return mapToDTO(addressRepository.save(address));
    }

    @Transactional
    public AddressDTO updateAddress(String email, Long id, AddressCreateRequest request) {
        Address address = getOwnedAddress(email, id);
        if (request.getFirstName() != null) address.setFirstName(request.getFirstName());
        if (request.getLastName() != null) address.setLastName(request.getLastName());
        if (request.getPhone() != null) address.setPhone(request.getPhone());
        if (request.getAddressLine1() != null) address.setAddressLine1(request.getAddressLine1());
        if (request.getCity() != null) address.setCity(request.getCity());
        if (request.getDepartment() != null) address.setDepartment(request.getDepartment());
        if (request.getAddressLine2() != null) address.setAddressLine2(request.getAddressLine2());
        if (request.getPostalCode() != null) address.setPostalCode(request.getPostalCode());
        if (request.getAddressType() != null) address.setAddressType(request.getAddressType());
        if (request.getIsDefault() != null) address.setIsDefault(request.getIsDefault());
        return mapToDTO(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(String email, Long id) {
        Address address = getOwnedAddress(email, id);
        addressRepository.delete(address);
    }

    private Address getOwnedAddress(String email, Long id) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada"));
        if (!address.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Dirección no encontrada");
        }
        return address;
    }

    private AddressDTO mapToDTO(Address address) {
        return AddressDTO.builder()
                .id(address.getId())
                .firstName(address.getFirstName())
                .lastName(address.getLastName())
                .phone(address.getPhone())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .department(address.getDepartment())
                .postalCode(address.getPostalCode())
                .addressType(address.getAddressType())
                .isDefault(address.getIsDefault())
                .build();
    }
}
