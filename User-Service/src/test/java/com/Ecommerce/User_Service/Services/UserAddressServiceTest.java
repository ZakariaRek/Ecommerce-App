package com.Ecommerce.User_Service.Services;

import com.Ecommerce.User_Service.Models.UserAddress;
import com.Ecommerce.User_Service.Models.AddressType;
import com.Ecommerce.User_Service.Repositories.UserAddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceTest {

    @Mock
    private UserAddressRepository userAddressRepository;

    @InjectMocks
    private UserAddressService userAddressService;

    private UserAddress homeAddress;
    private UserAddress workAddress;
    private UserAddress defaultAddress;
    private String userId;

    @BeforeEach
    void setUp() {
        userId = "user123";

        homeAddress = new UserAddress();
        homeAddress.setId("addr1");
        homeAddress.setUserId(userId);
        homeAddress.setAddressType(AddressType.HOME);
        homeAddress.setStreet("123 Home St");
        homeAddress.setCity("Home City");
        homeAddress.setState("Home State");
        homeAddress.setCountry("USA");
        homeAddress.setZipCode("12345");
        homeAddress.setDefault(false);

        workAddress = new UserAddress();
        workAddress.setId("addr2");
        workAddress.setUserId(userId);
        workAddress.setAddressType(AddressType.WORK);
        workAddress.setStreet("456 Work Ave");
        workAddress.setCity("Work City");
        workAddress.setState("Work State");
        workAddress.setCountry("USA");
        workAddress.setZipCode("67890");
        workAddress.setDefault(false);

        defaultAddress = new UserAddress();
        defaultAddress.setId("addr3");
        defaultAddress.setUserId(userId);
        defaultAddress.setAddressType(AddressType.HOME);
        defaultAddress.setStreet("789 Default Rd");
        defaultAddress.setCity("Default City");
        defaultAddress.setState("Default State");
        defaultAddress.setCountry("USA");
        defaultAddress.setZipCode("11111");
        defaultAddress.setDefault(true);
    }

    @Test
    void getAllAddresses_ShouldReturnAllAddresses() {
        // Given
        List<UserAddress> expectedAddresses = List.of(homeAddress, workAddress, defaultAddress);
        when(userAddressRepository.findAll()).thenReturn(expectedAddresses);

        // When
        List<UserAddress> actualAddresses = userAddressService.getAllAddresses();

        // Then
        assertThat(actualAddresses).isEqualTo(expectedAddresses);
        assertThat(actualAddresses).hasSize(3);
        verify(userAddressRepository).findAll();
    }

    @Test
    void getAddressesByUserId_ShouldReturnUserAddresses() {
        // Given
        List<UserAddress> userAddresses = List.of(homeAddress, workAddress, defaultAddress);
        when(userAddressRepository.findByUserId(userId)).thenReturn(userAddresses);

        // When
        List<UserAddress> result = userAddressService.getAddressesByUserId(userId);

        // Then
        assertThat(result).isEqualTo(userAddresses);
        assertThat(result).hasSize(3);
        verify(userAddressRepository).findByUserId(userId);
    }

    @Test
    void getAddressById_WhenAddressExists_ShouldReturnAddress() {
        // Given
        when(userAddressRepository.findById(homeAddress.getId())).thenReturn(Optional.of(homeAddress));

        // When
        Optional<UserAddress> result = userAddressService.getAddressById(homeAddress.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(homeAddress);
        verify(userAddressRepository).findById(homeAddress.getId());
    }

    @Test
    void getAddressById_WhenAddressDoesNotExist_ShouldReturnEmpty() {
        // Given
        String nonExistentId = "nonexistent";
        when(userAddressRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        Optional<UserAddress> result = userAddressService.getAddressById(nonExistentId);

        // Then
        assertThat(result).isEmpty();
        verify(userAddressRepository).findById(nonExistentId);
    }

    @Test
    void getDefaultAddress_WhenDefaultExists_ShouldReturnDefaultAddress() {
        // Given
        when(userAddressRepository.findByUserIdAndIsDefaultTrue(userId)).thenReturn(Optional.of(defaultAddress));

        // When
        Optional<UserAddress> result = userAddressService.getDefaultAddress(userId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(defaultAddress);
        assertThat(result.get().isDefault()).isTrue();
        verify(userAddressRepository).findByUserIdAndIsDefaultTrue(userId);
    }

    @Test
    void getDefaultAddress_WhenNoDefaultExists_ShouldReturnEmpty() {
        // Given
        when(userAddressRepository.findByUserIdAndIsDefaultTrue(userId)).thenReturn(Optional.empty());

        // When
        Optional<UserAddress> result = userAddressService.getDefaultAddress(userId);

        // Then
        assertThat(result).isEmpty();
        verify(userAddressRepository).findByUserIdAndIsDefaultTrue(userId);
    }

    @Test
    void createAddress_WhenNotDefault_ShouldSaveAddress() {
        // Given
        when(userAddressRepository.save(any(UserAddress.class))).thenReturn(homeAddress);

        // When
        UserAddress result = userAddressService.createAddress(homeAddress);

        // Then
        assertThat(result).isEqualTo(homeAddress);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(userAddressRepository).save(homeAddress);
        verify(userAddressRepository, never()).findByUserIdAndIsDefaultTrue(anyString());
    }

    @Test
    void createAddress_WhenDefault_ShouldUnsetExistingDefaultAndSaveNewDefault() {
        // Given
        UserAddress existingDefault = new UserAddress();
        existingDefault.setId("existing");
        existingDefault.setUserId(userId);
        existingDefault.setDefault(true);

        defaultAddress.setDefault(true);

        when(userAddressRepository.findByUserIdAndIsDefaultTrue(userId)).thenReturn(Optional.of(existingDefault));
        when(userAddressRepository.save(any(UserAddress.class))).thenReturn(defaultAddress);

        // When
        UserAddress result = userAddressService.createAddress(defaultAddress);

        // Then
        assertThat(result).isEqualTo(defaultAddress);
        verify(userAddressRepository).findByUserIdAndIsDefaultTrue(userId);
        verify(userAddressRepository, times(2)).save(any(UserAddress.class)); // Once for unsetting, once for new address
    }

    @Test
    void updateAddress_WhenDefault_ShouldHandleDefaultAddressLogic() {
        // Given
        UserAddress existingDefault = new UserAddress();
        existingDefault.setId("existing");
        existingDefault.setUserId(userId);
        existingDefault.setDefault(true);

        workAddress.setDefault(true);

        when(userAddressRepository.findByUserIdAndIsDefaultTrue(userId)).thenReturn(Optional.of(existingDefault));
        when(userAddressRepository.save(any(UserAddress.class))).thenReturn(workAddress);

        // When
        UserAddress result = userAddressService.updateAddress(workAddress);

        // Then
        assertThat(result).isEqualTo(workAddress);
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(userAddressRepository).findByUserIdAndIsDefaultTrue(userId);
        verify(userAddressRepository, times(2)).save(any(UserAddress.class));
    }

    @Test
    void setAddressAsDefault_WhenAddressExists_ShouldSetAsDefault() {
        // Given
        when(userAddressRepository.findById(homeAddress.getId())).thenReturn(Optional.of(homeAddress));
        when(userAddressRepository.findByUserIdAndIsDefaultTrue(userId)).thenReturn(Optional.of(defaultAddress));

        UserAddress updatedAddress = new UserAddress();
        updatedAddress.setId(homeAddress.getId());
        updatedAddress.setDefault(true);

        when(userAddressRepository.save(any(UserAddress.class))).thenReturn(updatedAddress);

        // When
        UserAddress result = userAddressService.setAddressAsDefault(homeAddress.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isDefault()).isTrue();
        verify(userAddressRepository).findById(homeAddress.getId());
        verify(userAddressRepository).findByUserIdAndIsDefaultTrue(userId);
        verify(userAddressRepository, times(2)).save(any(UserAddress.class)); // Once for unsetting existing, once for setting new
    }

    @Test
    void setAddressAsDefault_WhenAddressDoesNotExist_ShouldReturnNull() {
        // Given
        String nonExistentId = "nonexistent";
        when(userAddressRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        UserAddress result = userAddressService.setAddressAsDefault(nonExistentId);

        // Then
        assertThat(result).isNull();
        verify(userAddressRepository).findById(nonExistentId);
        verify(userAddressRepository, never()).save(any(UserAddress.class));
    }

    @Test
    void deleteAddress_ShouldCallRepositoryDelete() {
        // Given
        String addressId = "addr1";
        doNothing().when(userAddressRepository).deleteById(addressId);

        // When
        userAddressService.deleteAddress(addressId);

        // Then
        verify(userAddressRepository).deleteById(addressId);
    }

    @Test
    void deleteAddressesByUserId_ShouldCallRepositoryDeleteByUserId() {
        // Given
        doNothing().when(userAddressRepository).deleteByUserId(userId);

        // When
        userAddressService.deleteAddressesByUserId(userId);

        // Then
        verify(userAddressRepository).deleteByUserId(userId);
    }
}