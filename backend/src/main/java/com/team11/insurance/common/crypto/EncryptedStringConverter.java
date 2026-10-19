package com.team11.insurance.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA Attribute Converter for transparent AES-256-GCM field encryption at rest (INS-SR-004, SAD §4.4).
 * Fails securely and refuses to silently bypass encryption under any persistence lifecycle state.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static volatile FieldEncryptionService encryptionService;

    @Autowired
    public void setFieldEncryptionService(FieldEncryptionService service) {
        encryptionService = service;
    }

    public static void setStaticEncryptionService(FieldEncryptionService service) {
        encryptionService = service;
    }

    private static FieldEncryptionService getService() {
        if (encryptionService == null) {
            encryptionService = FieldEncryptionService.getDefaultInstance();
        }
        if (encryptionService == null) {
            throw new IllegalStateException("Field encryption service is not available - failing securely to prevent plaintext leakage.");
        }
        return encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return getService().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return getService().decrypt(dbData);
    }
}
