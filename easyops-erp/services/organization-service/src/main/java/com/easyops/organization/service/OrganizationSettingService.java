package com.easyops.organization.service;

import com.easyops.organization.entity.OrganizationSetting;
import com.easyops.organization.repository.OrganizationSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Organization Setting Service
 * Handles business logic for organization settings with encryption support
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationSettingService {

    private final OrganizationSettingRepository settingRepository;
    
    // In production, this should be stored securely (e.g., AWS KMS, HashiCorp Vault)
    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String ENCRYPTION_KEY = "EasyOpsSecretKey1234567890123456"; // 32 bytes for AES-256

    /**
     * Get all settings for organization
     */
    public Map<String, String> getOrganizationSettings(UUID organizationId) {
        log.debug("Fetching settings for organization: {}", organizationId);
        
        List<OrganizationSetting> settings = settingRepository.findByOrganizationId(organizationId);
        
        return settings.stream().collect(Collectors.toMap(
                OrganizationSetting::getSettingKey,
                setting -> {
                    if (Boolean.TRUE.equals(setting.getIsEncrypted())) {
                        return decrypt(setting.getSettingValue());
                    }
                    return setting.getSettingValue();
                }
        ));
    }

    /**
     * Get specific setting
     */
    public String getSetting(UUID organizationId, String key) {
        log.debug("Fetching setting {} for organization: {}", key, organizationId);
        
        OrganizationSetting setting = settingRepository
                .findByOrganizationIdAndSettingKey(organizationId, key)
                .orElse(null);
        
        if (setting == null) {
            return null;
        }
        
        if (Boolean.TRUE.equals(setting.getIsEncrypted())) {
            return decrypt(setting.getSettingValue());
        }
        
        return setting.getSettingValue();
    }

    /**
     * Set or update a setting
     */
    @Transactional
    public void setSetting(UUID organizationId, String key, String value, String type, Boolean encrypted) {
        log.info("Setting {} for organization: {}", key, organizationId);
        
        OrganizationSetting setting = settingRepository
                .findByOrganizationIdAndSettingKey(organizationId, key)
                .orElse(OrganizationSetting.builder()
                        .organizationId(organizationId)
                        .settingKey(key)
                        .build());
        
        String valueToStore = value;
        if (Boolean.TRUE.equals(encrypted)) {
            valueToStore = encrypt(value);
            setting.setIsEncrypted(true);
        } else {
            setting.setIsEncrypted(false);
        }
        
        setting.setSettingValue(valueToStore);
        setting.setSettingType(type);
        
        settingRepository.save(setting);
        log.info("Setting {} updated successfully for organization: {}", key, organizationId);
    }

    /**
     * Delete a setting
     */
    @Transactional
    public void deleteSetting(UUID organizationId, String key) {
        log.info("Deleting setting {} for organization: {}", key, organizationId);
        settingRepository.deleteByOrganizationIdAndSettingKey(organizationId, key);
    }

    /**
     * Encrypt a value
     */
    private String encrypt(String value) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(
                    ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), 
                    ENCRYPTION_ALGORITHM
            );
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Error encrypting value", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt a value
     */
    private String decrypt(String encryptedValue) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(
                    ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), 
                    ENCRYPTION_ALGORITHM
            );
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedValue));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error decrypting value", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
}

