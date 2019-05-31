package unimelb.bitbox.util;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AES {

    public static byte[] generateSecretKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[16];
        secureRandom.nextBytes(key);
        return key;
    }

    public static String encrypt(String message, byte[] key) throws GeneralSecurityException,
            UnsupportedEncodingException {
        return encrypt(message.getBytes("UTF-8"), key);
    }


    public static String encrypt(byte[] message, byte[] key) throws GeneralSecurityException {
        // Like in tutorial examples, we use ECB - which isn't ideal but there's nothing in the
        // project spec about how to go about sending IVs to use something like CBC.
        Key aesKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        // Perform encryption
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encrypted = cipher.doFinal(message);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String decrypt(String message, byte[] key) {
        // Decrypt result
        try {
            Key aesKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            message = new String(cipher.doFinal(Base64.getDecoder().decode(message.getBytes())));
            return message;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return "";
    }
}
