package unimelb.bitbox.util;

//snippets from https://stackoverflow.com/questions/47816938/java-ssh-rsa-string-to-public-key
//https://www.devglan.com/java8/rsa-encryption-decryption-java

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.util.encoders.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.spec.RSAPublicKeySpec;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Splitter;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.base.Charsets;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.get;
import static com.google.common.collect.Iterables.size;


public class SSH {
    private static final String SSH_MARKER = "ssh-rsa";

    private static ByteSource supplier;
    private static Logger log = Logger.getLogger(SSH.class.getName());

    public static byte[] encrypt(String keyString, byte[] bytes) throws GeneralSecurityException {
        SSHEncodedToRSAPublicConverter(keyString);
        KeySpec spec = convertToRSAPublicKey();
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey pubKey = kf.generatePublic(spec);

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        byte[] cipherData = cipher.doFinal(bytes);
        return cipherData;
    }

    public static byte[] decrypt(String input) throws InvalidKeyException,
            NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException,
            BadPaddingException {
        return decrypt(Base64.decode(input.getBytes()));
    }

    public static byte[] decrypt(byte[] data) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException {
        PrivateKey privKey = getPrivate();
        Cipher cipher;
        cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privKey);
        return cipher.doFinal(data);
    }


    public static PrivateKey getPrivate() {
        try {

            String filename = "bitboxclient_rsa.der";
            byte[] keyBytes = Files.readAllBytes(Paths.get(filename));

            PKCS8EncodedKeySpec spec =
                    new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (Exception e) {
            log.severe(e.getMessage());
        }
        return null;
    }


    public static void SSHEncodedToRSAPublicConverter(String string) {
        try {
            byte[] data = string.getBytes();
            supplier = ByteSource.wrap(data);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Converts an SSH public key to a x.509 compliant format RSA public key spec
     * Source: https://github.com/jclouds/jclouds/blob/master/compute/src/main/java/org/jclouds
     * /ssh/SshKeys.java
     *
     * @return RSAPublicKeySpec
     */
    public static RSAPublicKeySpec convertToRSAPublicKey() {
        try {
            InputStream stream = supplier.openStream();
            Iterable<String> parts = Splitter.on(' ').split(IOUtils.toString(stream,
                    Charsets.UTF_8));
            checkArgument(size(parts) >= 2 && SSH_MARKER.equals(get(parts, 0)), "bad format, " +
                    "should be: ssh-rsa AAAB3....");
            stream = new ByteArrayInputStream(Base64.decode(get(parts, 1)));
            String marker = new String(readLengthFirst(stream));
            checkArgument(SSH_MARKER.equals(marker), "looking for marker %s but received %s",
                    SSH.SSH_MARKER, marker);
            BigInteger publicExponent = new BigInteger(readLengthFirst(stream));
            BigInteger modulus = new BigInteger(readLengthFirst(stream));
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, publicExponent);
            return keySpec;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static byte[] readLengthFirst(InputStream in) throws IOException {
        int[] bytes = new int[]{in.read(), in.read(), in.read(), in.read()};
        int length = 0;
        int shift = 24;
        for (int i = 0; i < bytes.length; i++) {
            length += bytes[i] << shift;
            shift -= 8;
        }
        byte[] val = new byte[length];
        ByteStreams.readFully(in, val);
        return val;
    }
}
