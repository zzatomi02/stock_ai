package com.noono0.stock.integration.kis.credential.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class KisSecretCrypto {
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final SecretKeySpec keySpec;

    public KisSecretCrypto(@Value("${app.kis.credential-encryption-key:local-dev-kis-credential-key-change-it}") String key) {
        this.keySpec = new SecretKeySpec(sha256(key), "AES");
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) return "";
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            byte[] enc = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + enc.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(enc, 0, out, iv.length, enc.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("KIS 자격증명 암호화 실패", e);
        }
    }

    public String decrypt(String enc) {
        if (enc == null || enc.isBlank()) return "";
        try {
            byte[] all = Base64.getDecoder().decode(enc);
            if (all.length <= IV_LENGTH) return "";
            byte[] iv = new byte[IV_LENGTH];
            byte[] body = new byte[all.length - IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            System.arraycopy(all, IV_LENGTH, body, 0, body.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            byte[] dec = cipher.doFinal(body);
            return new String(dec, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("KIS 자격증명 복호화 실패", e);
        }
    }

    private static byte[] sha256(String src) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(src.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 초기화 실패", e);
        }
    }
}
