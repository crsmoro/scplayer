package com.shuffle.scplayer.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fi.iki.elonen.IWebSocketFactory;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.WebSocket;
import fi.iki.elonen.WebSocketFrame;
import fi.iki.elonen.WebSocketResponseHandler;

public abstract class WebServer extends NanoHTTPD {
	
	private static final transient Log log = LogFactory.getLog(WebServer.class);

	private static final String MIME_PLAINTEXT = "text/plain", MIME_HTML = "text/html",
			MIME_JS = "application/javascript", MIME_CSS = "text/css", MIME_PNG = "image/png",
			MIME_DEFAULT_BINARY = "application/octet-stream", MIME_XML = "text/xml";
	private static final Map<String, String> extensionsMimes = new HashMap<String, String>();

	private WebSocketResponseHandler webSocketResponseHandler;
	
	private static final Gson gson = new GsonBuilder().create();
	
	private static final JsonParser  jsonParser = new JsonParser();

	private List<com.shuffle.scplayer.web.WebSocket> webSockets = new ArrayList<com.shuffle.scplayer.web.WebSocket>();

	private IWebSocketFactory webSocketFactory = new IWebSocketFactory() {

		@Override
		public WebSocket openWebSocket(IHTTPSession handshake) {
			com.shuffle.scplayer.web.WebSocket webSocket = new com.shuffle.scplayer.web.WebSocket(handshake) {

				@Override
				protected void onMessage(WebSocketFrame messageFrame) {
					if (!messageFrame.getTextPayload().equals(""))
					{
						try
						{
							JsonObject jsonObject = jsonParser.parse(messageFrame.getTextPayload()).getAsJsonObject();
							if (jsonObject.has("action") &&jsonObject.get("action").getAsString().equals("ping"))
							{
								JsonObject jo = new JsonObject();
								jo.addProperty("action", "pong");
								this.send(gson.toJson(jo).getBytes("UTF-8"));
							}
						}
						catch (Exception e)
						{
							log.error("onMessage error", e);
						}
					}
					WebServer.this.onMessage(messageFrame.getTextPayload(), this);
				}
			};
			webSockets.add(webSocket);
			return webSocket;
		}
	};

	static {
		extensionsMimes.put("txt", MIME_PLAINTEXT);
		extensionsMimes.put("htm", MIME_HTML);
		extensionsMimes.put("html", MIME_HTML);
		extensionsMimes.put("js", MIME_JS);
		extensionsMimes.put("css", MIME_CSS);
		extensionsMimes.put("png", MIME_PNG);
		extensionsMimes.put("xml", MIME_XML);
	}

	public WebServer(int port) {
		super(port);
		webSocketResponseHandler = new WebSocketResponseHandler(webSocketFactory);
	}

	@Override
	public Response serve(IHTTPSession session) {

		Response ws = webSocketResponseHandler.serve(session);
		if (ws != null) {
			return ws;
		} else {
			try {
				
				String docBase = "com/shuffle/scplayer/web/webapp";
				String urlRequested = session.getUri();
				//System.out.println(urlRequested);
				String extension = "";
				if (urlRequested == null || urlRequested.equals("") || urlRequested.equals("/")) {
					urlRequested = "";
					if (!urlRequested.endsWith("/"))
					{
						urlRequested += "/";
					}
					urlRequested += "index.html";
				}
				extension = urlRequested.split("\\.")[urlRequested.split("\\.").length - 1];
				//System.out.println(session.getQueryParameterString());
				//System.out.println(extension);

				return new Response(Response.Status.OK,
						(extensionsMimes.get(extension) != null ? extensionsMimes.get(extension) : MIME_DEFAULT_BINARY),
						getClass().getProtectionDomain().getClassLoader().getResourceAsStream(docBase + urlRequested));
			} catch (Exception e) {
				log.error("Not found error", e);
				return new Response(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
			}
		}
	}

	public void sendMessage(String message) {
		List<com.shuffle.scplayer.web.WebSocket> webSocketsToRemove = new ArrayList<com.shuffle.scplayer.web.WebSocket>();
		for (com.shuffle.scplayer.web.WebSocket webSocket : webSockets) {
			try {
				webSocket.send(message.getBytes("UTF-8"));
			} catch (IOException e) {
				log.debug("Session expired", e);
				webSocketsToRemove.add(webSocket);
			}
		}
		webSockets.removeAll(webSocketsToRemove);
	}

	protected abstract void onMessage(String message, com.shuffle.scplayer.web.WebSocket webSocket);

}
