package com.shuffle.scplayer.core;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.shuffle.scplayer.jna.SpotifyLibrary.SpDeviceType;

import fi.iki.elonen.NanoHTTPD;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/* ZeroConf spotify authentication
 * Because SpZeroConfGetVars is giving sigsegvs communication and encryption is implemented here.
 * Based od librespot.
 */
public class SpotifyConnectZeroConf extends NanoHTTPD {
	private static final transient Log log = LogFactory.getLog(SpotifyConnectZeroConf.class);
	private static final Gson gson = new GsonBuilder().create();
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

	@Override
	public Response serve(IHTTPSession session) {
		try {
			session.parseBody(new HashMap<String, String>());
			Map<String, String> params = session.getParms();
			String urlRequested = session.getUri();
			String action = params.get("action");
			log.debug(session.getMethod().name()+": "+urlRequested+" : "+action);
			if (action.equals("getInfo")) {
				return getInfo();
			} else if (action.equals("addUser")) {
				return addUser(params.get("userName"), params.get("blob"), params.get("clientKey"));
			}
			return new Response(Response.Status.NOT_FOUND, "", "");
		}catch(Exception ex) {
			return new Response(Response.Status.INTERNAL_ERROR, "text/html", ex.getMessage());
		}
	}

	public SpotifyConnectZeroConf(SpotifyConnectPlayer player) throws Exception {
		super(4001);
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

	private Response getInfo() {
		byte[] publicKeyByte = clampArray(this.publicKey.toByteArray(), 96);
		String publicKey = Base64.getEncoder().encodeToString(publicKeyByte);

		JsonObject result = new JsonObject();
		result.addProperty("status", 101);
		result.addProperty("statusString", "ERROR-OK");
		result.addProperty("spotifyError", 0);
		result.addProperty("version", "2.1.0");
		result.addProperty("deviceID", player.getDeviceId());
		result.addProperty("remoteName", player.getPlayerName());
		result.addProperty("activeUser", player.getUsername());
		result.addProperty("publicKey", publicKey);
		result.addProperty("deviceType", SpDeviceType.kSpDeviceTypeAudioDongle);
		result.addProperty("libraryVersion", "0.1.0");
		result.addProperty("accountReq", "PREMIUM");
		return new Response(Response.Status.OK, "application/json", gson.toJson(result));
	}

	private Response addUser(String username, String blob, String clientKey) throws NoSuchAlgorithmException, InvalidKeyException, IOException, InvalidKeySpecException, NoSuchProviderException, NoSuchPaddingException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {
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

		JsonObject result = new JsonObject();
		result.addProperty("status", 101);
		result.addProperty("spotifyError", 0);
		result.addProperty("statusString", "ERROR-OK");
		return new Response(Response.Status.OK, "application/json", gson.toJson(result));
	}

	public void runServer() throws IOException, InterruptedException {
		final SpotifyConnectZeroConf webServer = this;
        Thread server = new Thread(new Runnable() {
            public void run() {
                try {
                    webServer.start();
                    System.in.read();
                } catch (IOException e) {
                    log.fatal("ZeroConf server thread error", e);
                    System.exit(-1);
                }
            }
        });
        server.setDaemon(true);
        server.start();
		Thread avahi = new Thread(new Runnable() {
			@Override
			public void run() {
				// can't get JmDNS to work with _spotify-connect._tcp
				// use avahi-publish as workaround - not portable, needs avahi-utils
				try {
					Runtime.getRuntime().exec("avahi-publish -s scplayer _spotify-connect._tcp 4001 \"VERSION=1.0 CPath=/\"");
				}catch(Exception e) {
					log.fatal("Avahi thread error", e);
				}
			}
		});
		avahi.setDaemon(true);
		avahi.start();

	}
}
