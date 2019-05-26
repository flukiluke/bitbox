package unimelb.bitbox.util;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES {
	
	public static String generateSecretKey(String publicKey) {
		return null;
	}
	
	public static String encrypt(String keyString, String input) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		
		SecureRandom secureRandom = new SecureRandom();
		byte[] key = new byte[16];
		secureRandom.nextBytes(key);
		Logger log = Logger.getLogger("AES");
		
		log.info(key.length + "compared to " + keyString.getBytes().length + " compared to :" + Base64.getDecoder().decode(keyString.getBytes()).length);
		SecretKey secretKey = new SecretKeySpec(Base64.getEncoder().encode(keyString.getBytes()), "AES");
		
		byte[] iv = new byte[12]; //NEVER REUSE THIS IV WITH SAME KEY
		secureRandom.nextBytes(iv);
		
		final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv); //128 bit auth tag length
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
		
		byte[] cipherText = cipher.doFinal(input.getBytes());
		
		ByteBuffer byteBuffer = ByteBuffer.allocate(4 + iv.length + cipherText.length);
		byteBuffer.putInt(iv.length);
		byteBuffer.put(iv);
		byteBuffer.put(cipherText);
		byte[] cipherMessage = byteBuffer.array();
		
		return cipherMessage.toString();
	}

	public static String decrypt(String key, String input) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException{
		
		ByteBuffer byteBuffer = ByteBuffer.wrap(input.getBytes());
		int ivLength = byteBuffer.getInt();
		if(ivLength < 12 || ivLength >= 16) { // check input parameter
		    throw new IllegalArgumentException("invalid iv length");
		}
		byte[] iv = new byte[ivLength];
		byteBuffer.get(iv);
		byte[] cipherText = new byte[byteBuffer.remaining()];
		byteBuffer.get(cipherText);
		
		final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(), "AES"), new GCMParameterSpec(128, iv));

		byte[] plainText= cipher.doFinal(cipherText);
		
		return plainText.toString();
	}
}
