package com.shuffle.scplayer.core.zeroconf;

import java.math.BigInteger;
import java.security.Key;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.shuffle.scplayer.core.SpotifyConnectPlayer;
import com.shuffle.scplayer.jna.SpotifyLibrary;

/* Spotify zeroconf vars and login provider based on open source implementation (code based on libRespot) */
public class SpotifyZeroConfProviderOS implements SpotifyZeroConfProvider {

    private final SpotifyConnectPlayer player;

    BigInteger publicKey, privateKey;
    // pregenerated prime that client is aware of for DH key exchange
    // IMO java should have unsigned types
    BigInteger dhPrime = new BigInteger(1, new byte[] { (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xc9,
            0x0f, (byte)0xda, (byte)0xa2, 0x21, 0x68, (byte)0xc2, 0x34, (byte)0xc4, (byte)0xc6,
            0x62, (byte)0x8b, (byte)0x80, (byte)0xdc, 0x1c, (byte)0xd1, 0x29, 0x02, 0x4e,
            0x08, (byte)0x8a, 0x67, (byte)0xcc, 0x74, 0x02, 0x0b, (byte)0xbe, (byte)0xa6,
            0x3b, 0x13, (byte)0x9b, 0x22, 0x51, 0x4a, 0x08, 0x79, (byte)0x8e,
            0x34, 0x04,(byte) 0xdd, (byte)0xef,(byte) 0x95, 0x19, (byte)0xb3, (byte)0xcd, 0x3a,
            0x43, 0x1b, 0x30, 0x2b, 0x0a, 0x6d, (byte)0xf2, 0x5f, 0x14,
            0x37, 0x4f, (byte)0xe1, 0x35, 0x6d, 0x6d, 0x51, (byte)0xc2, 0x45,
            (byte)0xe4, (byte)0x85, (byte)0xb5, 0x76, 0x62, 0x5e, 0x7e, (byte)0xc6, (byte)0xf4,
            0x4c, 0x42, (byte)0xe9, (byte)0xa6, 0x3a, 0x36, 0x20, (byte)0xff, (byte)0xff,
            (byte)0xff, (byte)0xff, (byte)0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff });

    // and a standard generator
    BigInteger dhGenerator = BigInteger.valueOf(2);
    public SpotifyZeroConfProviderOS(SpotifyConnectPlayer player) {
        this.player = player;

        // simple DH keys generation - this way is simpler than use of crypto library
        privateKey = new BigInteger(768, new SecureRandom());
        publicKey = dhGenerator.modPow(privateKey, dhPrime);
    }
    // BigInteger class adds one byte on the beginning to preserve sign when values less than 0
    // This is a helper to cut off this additional byte as it breaks encryption
    private byte[] clampArray(byte[] array, int length) {
        if(array.length > length) {
            return Arrays.copyOfRange(array, array.length - length, array.length);
        }
        return array;
    }
    @Override
    public SpotifyZeroConfVars getVars() {
        byte[] publicKeyByte = clampArray(this.publicKey.toByteArray(), 96);
        String publicKey = Base64.getEncoder().encodeToString(publicKeyByte);

        return new SpotifyZeroConfVars(publicKey, player.getDeviceId(), player.getUsername(),
                player.getPlayerName(), "PREMIUM", String.valueOf(SpotifyLibrary.SpDeviceType.kSpDeviceTypeAudioDongle), "0.1.0");
    }

    @Override
    public void loginZeroConf(String username, String blob, String clientKey) throws Exception {
        BigInteger clientPublicKey = new BigInteger(1, Base64.getDecoder().decode(clientKey));

        // DH shared key calculation
        byte[] sharedKey = clientPublicKey.modPow(privateKey, dhPrime).toByteArray();

        MessageDigest messageDigest = MessageDigest.getInstance("SHA1");

        byte[] blobBytes = Base64.getDecoder().decode(blob);
        byte[] iv = Arrays.copyOfRange(blobBytes, 0, 16); //16 byte padding for AES
        byte[] encrypted = Arrays.copyOfRange(blobBytes, 16, blobBytes.length-20); // actual encrypted blob
        byte[] checksum = Arrays.copyOfRange(blobBytes, blobBytes.length-20, blobBytes.length); // checksum

        Key base = new SecretKeySpec(Arrays.copyOfRange(messageDigest.digest(clampArray(sharedKey, 96)), 0, 16), "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(base);

        // checksum check
        Key checksumKey = new SecretKeySpec(mac.doFinal("checksum".getBytes()), "HmacSHA1");
        Mac checksumMac = Mac.getInstance("HmacSHA1");
        checksumMac.init(checksumKey);
        byte[] actualChecksum = checksumMac.doFinal(encrypted);
        assert Arrays.equals(checksum, actualChecksum);

        Key encryptionKey = new SecretKeySpec(Arrays.copyOfRange(mac.doFinal("encryption".getBytes()), 0, 16), "AES");
        // Finally some strange cipher AES CTR with padding
        Cipher cipher = Cipher.getInstance("AES/CTR/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey,new IvParameterSpec(iv));
        byte[] decrypted = cipher.doFinal(encrypted);
        String decryptedBlob = new String(decrypted);

        player.loginBlob(username, decryptedBlob);
    }
}
