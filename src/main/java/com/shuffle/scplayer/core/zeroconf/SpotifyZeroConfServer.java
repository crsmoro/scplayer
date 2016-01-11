package com.shuffle.scplayer.core.zeroconf;

import java.io.IOException;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import fi.iki.elonen.NanoHTTPD;

/* Zeroconf spotify authentication server */
public class SpotifyZeroConfServer extends NanoHTTPD {
	private static final transient Log log = LogFactory.getLog(SpotifyZeroConfServer.class);
	private static final Gson gson = new GsonBuilder().create();
	private final SpotifyZeroConfProvider provider;
	private final ZeroConfService zeroConfService;

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

	public SpotifyZeroConfServer(SpotifyZeroConfProvider provider, ZeroConfService zeroConfService) throws Exception {
		super(4001);
		this.provider = provider;
		this.zeroConfService = zeroConfService;
	}

	private Response getInfo() {
		SpotifyZeroConfVars vars = provider.getVars();

		JsonObject result = new JsonObject();
		result.addProperty("status", 101);
		result.addProperty("statusString", "ERROR-OK");
		result.addProperty("spotifyError", 0);
		result.addProperty("version", "2.1.0");
		result.addProperty("deviceID", vars.getDeviceId());
		result.addProperty("remoteName", vars.getRemoteName());
		result.addProperty("activeUser", vars.getActiveUser());
		result.addProperty("publicKey", vars.getPublicKey());
		result.addProperty("deviceType", vars.getDeviceType());
		result.addProperty("libraryVersion", "0.1.0");
		result.addProperty("accountReq", vars.getAccountReq());
		return new Response(Response.Status.OK, "application/json", gson.toJson(result));
	}

	private Response addUser(String username, String blob, String clientKey) throws Exception {
		provider.loginZeroConf(username, blob, clientKey);

		JsonObject result = new JsonObject();
		result.addProperty("status", 101);
		result.addProperty("spotifyError", 0);
		result.addProperty("statusString", "ERROR-OK");
		return new Response(Response.Status.OK, "application/json", gson.toJson(result));
	}

	public void runServer() throws IOException, InterruptedException {
		final SpotifyZeroConfServer webServer = this;
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
		HashMap<String, String> values = new HashMap<>();
		values.put("VERSION","1.0");
		values.put("CPath","/");
		zeroConfService.publishService("scplayer", "_spotify-connect._tcp", 4001, values);
	}
}
