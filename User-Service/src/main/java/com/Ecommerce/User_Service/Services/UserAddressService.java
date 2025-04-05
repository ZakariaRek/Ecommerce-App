package com.Ecommerce.User_Service.Services;

import com.Ecommerce.User_Service.Models.UserAddress;
import com.Ecommerce.User_Service.Repositories.UserAddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserAddressService {

    @Autowired
    private UserAddressRepository userAddressRepository;

    public List<UserAddress> getAllAddresses() {
        return userAddressRepository.findAll();
    }

    public List<UserAddress> getAddressesByUserId(String userId) {
        return userAddressRepository.findByUserId(userId);
    }

    public Optional<UserAddress> getAddressById(String id) {
        return userAddressRepository.findById(id);
    }

    public Optional<UserAddress> getDefaultAddress(String userId) {
        return userAddressRepository.findByUserIdAndIsDefaultTrue(userId);
    }

    public UserAddress createAddress(UserAddress address) {
        address.setCreatedAt(LocalDateTime.now());
        address.setUpdatedAt(LocalDateTime.now());

        // If this is set as default, unset any existing default
        if (address.isDefault()) {
            handleDefaultAddress(address.getUserId());
        }

        return userAddressRepository.save(address);
    }

    public UserAddress updateAddress(UserAddress address) {
        address.setUpdatedAt(LocalDateTime.now());

        // If this is set as default, unset any existing default
        if (address.isDefault()) {
            handleDefaultAddress(address.getUserId());
        }

        return userAddressRepository.save(address);
    }

    private void handleDefaultAddress(String userId) {
        Optional<UserAddress> existingDefault = userAddressRepository.findByUserIdAndIsDefaultTrue(userId);
        existingDefault.ifPresent(addr -> {
            addr.setDefault(false);
            addr.setUpdatedAt(LocalDateTime.now());
            userAddressRepository.save(addr);
        });
    }

    public UserAddress setAddressAsDefault(String addressId) {
        Optional<UserAddress> optionalAddress = userAddressRepository.findById(addressId);
        if (optionalAddress.isPresent()) {
            UserAddress address = optionalAddress.get();
            handleDefaultAddress(address.getUserId());
            address.setDefault(true);
            address.setUpdatedAt(LocalDateTime.now());
            return userAddressRepository.save(address);
        }
        return null;
    }

    public void deleteAddress(String id) {
        userAddressRepository.deleteById(id);
    }

    @Transactional
    public void deleteAddressesByUserId(String userId) {
        userAddressRepository.deleteByUserId(userId);
    }
}