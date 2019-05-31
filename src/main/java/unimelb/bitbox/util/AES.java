package unimelb.bitbox.util;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Functions for performing the AES-128 symmetric cipher.
 *
 * @author TransfictionRailways
 */
public class AES {
    /**
     * Generates a new secret key suitable for use with AES-128.
     *
     * @return A 128 bit key
     */
    public static byte[] generateSecretKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[16];
        secureRandom.nextBytes(key);
        return key;
    }

    /**
     * Encrypt UTF-8 data with a given secret key using AES. Operates in ECB mode with PKCS5Padding.
     *
     * @param message Plain text
     * @param key     Secret key
     * @return Cipher text
     * @throws GeneralSecurityException     if there is a cryptography error
     * @throws UnsupportedEncodingException If the data is not valid UTF-8
     */
    public static String encrypt(String message, byte[] key) throws GeneralSecurityException,
            UnsupportedEncodingException {
        // Like in tutorial examples, we use ECB - which isn't ideal but there's nothing in the
        // project spec about how to go about sending IVs to use something like CBC.
        Key aesKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encrypted = cipher.doFinal(message.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Decrypt data with a given secret key using AES. Operates in ECB mode with PKCS5Padding.
     *
     * @param message Cipher text
     * @param key     Secret key
     * @return Plain text
     * @throws GeneralSecurityException if there is a cryptography error
     */
    public static String decrypt(String message, byte[] key) throws GeneralSecurityException {
        Key aesKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        message = new String(cipher.doFinal(Base64.getDecoder().decode(message.getBytes())));
        return message;
    }
}
