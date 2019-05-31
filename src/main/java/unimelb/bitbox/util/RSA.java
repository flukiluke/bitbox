package unimelb.bitbox.util;

import java.io.*;
import java.security.*;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

import java.math.BigInteger;
import java.security.spec.RSAPublicKeySpec;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Splitter;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.base.Charsets;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.get;
import static com.google.common.collect.Iterables.size;


/**
 * Functions performing the asymmetric RSA cipher.
 * <p>
 * Handles loading of keys and performing the cipher.
 *
 * @author TransfictionRailways
 */
public class RSA {
    // File to load the server's secret key from (defined by spec.)
    private static final String PRIVATE_KEY_FILE = "bitboxclient_rsa";

    private static final String SSH_MARKER = "ssh-rsa";

    private static ByteSource supplier;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Encrypt data with a public key such that it can only be decrypted by the corresponding
     * private key. Based on https://www.devglan.com/java8/rsa-encryption-decryption-java
     *
     * @param keyString The public key in the "ssh-rsa" format
     * @param bytes     Plain text
     * @return Cipher text
     * @throws GeneralSecurityException if key is invalid or algorithm is not available
     */
    public static byte[] encrypt(String keyString, byte[] bytes) throws GeneralSecurityException {
        SSHEncodedToRSAPublicConverter(keyString);
        KeySpec spec = convertToRSAPublicKey();
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey pubKey = kf.generatePublic(spec);
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return cipher.doFinal(bytes);
    }

    /**
     * Decrypt data using our private key. Based on https://www.devglan.com/java8/rsa-encryption-decryption-java
     *
     * @param input Cipher text as a base64-encoded string
     * @return Plain text
     * @throws IOException              If our private key cannot be read
     * @throws GeneralSecurityException If decryption fails
     */
    public static byte[] decrypt(String input) throws IOException, GeneralSecurityException {
        PrivateKey privKey = getPrivateKey();
        Cipher cipher;
        cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privKey);
        return cipher.doFinal(Base64.getDecoder().decode(input.getBytes()));
    }

    /**
     * Load our private key from an unencrypted keypair in PEM format.
     *
     * @return Our private RSA key
     * @throws IOException if the key cannot be read
     */
    private static PrivateKey getPrivateKey() throws IOException {
        FileReader fileReader = new FileReader(PRIVATE_KEY_FILE);
        PEMParser pemParser = new PEMParser(fileReader);
        Object pemObject;
        pemObject = pemParser.readObject();
        pemParser.close();
        fileReader.close();
        if (!(pemObject instanceof PEMKeyPair)) {
            throw new IOException("File " + PRIVATE_KEY_FILE + " does not contain an unencrypted key pair");
        }
        KeyPair kp = new JcaPEMKeyConverter().setProvider("BC").getKeyPair((PEMKeyPair) pemObject);
        return kp.getPrivate();
    }

    /**
     * Converts an SSH public key to a x.509 compliant format RSA public key spec. Based on
     * https://stackoverflow.com/questions/47816938/java-ssh-rsa-string-to-public-key and
     * https://github.com/jclouds/jclouds/blob/master/compute/src/main/java/org/jclouds/ssh/SshKeys.java
     *
     * @return RSAPublicKeySpec
     */
    private static RSAPublicKeySpec convertToRSAPublicKey() {
        try {
            InputStream stream = supplier.openStream();
            Iterable<String> parts = Splitter.on(' ').split(IOUtils.toString(stream,
                    Charsets.UTF_8));
            checkArgument(size(parts) >= 2 && SSH_MARKER.equals(get(parts, 0)), "bad format, " +
                    "should be: ssh-rsa AAAB3....");
            stream = new ByteArrayInputStream(Base64.getDecoder().decode(get(parts, 1)));
            String marker = new String(readLengthFirst(stream));
            checkArgument(SSH_MARKER.equals(marker), "looking for marker %s but received %s",
                    RSA.SSH_MARKER, marker);
            BigInteger publicExponent = new BigInteger(readLengthFirst(stream));
            BigInteger modulus = new BigInteger(readLengthFirst(stream));
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, publicExponent);
            return keySpec;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    // Prepare data for parsing
    private static void SSHEncodedToRSAPublicConverter(String string) {
        try {
            byte[] data = string.getBytes();
            supplier = ByteSource.wrap(data);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    // Read the length header from the ssh-rsa format
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
