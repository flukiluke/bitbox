package unimelb.bitbox.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class AES {
	
	public static byte[] generateSecretKey() {
		SecureRandom secureRandom = new SecureRandom();
		byte[] key = new byte[16];
		secureRandom.nextBytes(key);
		return key;
	}
	
	public static String encrypt(String message, byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException{
		// Encrypt first
			return encrypt(message.getBytes("UTF-8"), key);
	}
	

	public static String encrypt(byte[] message, byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException{
		// Encrypt first
			Key aesKey = new SecretKeySpec(key, "AES");
			Cipher cipher = Cipher.getInstance("AES");
			// Perform encryption
			cipher.init(Cipher.ENCRYPT_MODE, aesKey);
			byte[] encrypted = cipher.doFinal(message);
			return Base64.getEncoder().encodeToString(encrypted);
	}
	
	public static String decrypt(String message, byte[] key){
		// Decrypt result
		try {
			Key aesKey = new SecretKeySpec(key, "AES");
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, aesKey);
			message = new String(cipher.doFinal(Base64.getDecoder().decode(message.getBytes())));
			return message;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "";
	}
	
	/*
	
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
	*/
}
