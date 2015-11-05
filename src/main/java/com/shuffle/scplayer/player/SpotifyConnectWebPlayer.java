package com.shuffle.scplayer.player;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shuffle.scplayer.core.Track;
import com.shuffle.scplayer.web.WebServer;
import com.shuffle.scplayer.web.WebSocketObject;

public class SpotifyConnectWebPlayer extends SpotifyConnectPlayer {
	
	private static final transient Log log = LogFactory.getLog(SpotifyConnectWebPlayer.class);
	
	private WebServer webServer;
	
	private final transient Gson gson = new GsonBuilder().create();
	
	public SpotifyConnectWebPlayer() {
		super();
	}

	public SpotifyConnectWebPlayer(File appKey) {
		super(appKey);
		final WebServer webServer = new WebServer(4000) {

			@Override
			protected void onMessage(String message, com.shuffle.scplayer.web.WebSocket webSocket) {
				try
				{
					WebSocketObject webSocketObject = gson.fromJson(message, WebSocketObject.class);
					if (webSocketObject.getAction().equals("play"))
					{
						play();
					}
					else if (webSocketObject.getAction().equals("pause"))
					{
						pause();
					}
					else if (webSocketObject.getAction().equals("pause"))
					{
						pause();
					}
					else if (webSocketObject.getAction().equals("prev"))
					{
						prev();
					}
					else if (webSocketObject.getAction().equals("next"))
					{
						next();
					}
					else if (webSocketObject.getAction().equals("seek"))
					{
						seek(((Double)webSocketObject.getData()).intValue());
					}
					else if (webSocketObject.getAction().equals("shuffle"))
					{
						shuffle((boolean)webSocketObject.getData());
					}
					else if (webSocketObject.getAction().equals("repeat"))
					{
						repeat((boolean)webSocketObject.getData());
					}
					else if (webSocketObject.getAction().equals("volume"))
					{
						volume((short)webSocketObject.getData());
					}
					else if (webSocketObject.getAction().equals("playername"))
					{
						setPlayerName((String)webSocketObject.getData());
						webSocket.send(gson.toJson(new WebSocketObject("playername", getPlayerName())).getBytes("UTF-8"));
					}
					else if (webSocketObject.getAction().equals("login"))
					{
						@SuppressWarnings("unchecked")
						Map<String, String> mapLogin = (Map<String, String>) webSocketObject.getData();
						login(mapLogin.get("username"), mapLogin.get("password"));
					}
					else if (webSocketObject.getAction().equals("logout"))
					{
						logout();
					}
					else if (webSocketObject.getAction().equals("track"))
					{
						Track track = getPlayingTrack();
						WebSocketObject webSocketReturnObject = new WebSocketObject("track", track);
						webSocket.send(gson.toJson(webSocketReturnObject).getBytes("UTF-8"));
					}
					else if (webSocketObject.getAction().equals("getShuffle"))
					{
						webSocket.send(gson.toJson(new WebSocketObject("shuffle", getShuffle())).getBytes("UTF-8"));
					}
					else if (webSocketObject.getAction().equals("getRepeat"))
					{
						webSocket.send(gson.toJson(new WebSocketObject("repeat", getRepeat())).getBytes("UTF-8"));
					}
					else if (webSocketObject.getAction().equals("isPlaying"))
					{
						webSocket.send(gson.toJson(new WebSocketObject("playing", isPlaying())).getBytes("UTF-8"));
					}
					else if (webSocketObject.getAction().equals("isLoggedIn"))
					{
						webSocket.send(gson.toJson(new WebSocketObject("logged", isLoggedIn())).getBytes("UTF-8"));
					}
					else if (webSocketObject.getAction().equals("getAlbumCover"))
					{
						webSocket.send(gson.toJson(new WebSocketObject("albumcover", getAlbumCoverURL())).getBytes("UTF-8"));
					}
					else if (webSocketObject.getAction().equals("getPlayerName"))
					{
						webSocket.send(gson.toJson(new WebSocketObject("playername", getPlayerName())).getBytes("UTF-8"));
					}
				}
				catch (Exception e)
				{
					log.error("onMessage error", e);
				}
			}
		};
		this.webServer = webServer;
		Thread server = new Thread(new Runnable() {
			public void run() {
				try {
					webServer.start();
					System.in.read();
				} catch (IOException e) {
					log.fatal("Server thread error", e);
					System.exit(-1);
				}
			}
		});
		server.setDaemon(true);
		server.start();
	}
	
	public SpotifyConnectWebPlayer(String username, String password) {
		this();
		login(username, password);
	}
	
	public SpotifyConnectWebPlayer(String username, String password, String playerName) {
		this();
		login(username, password);
		setPlayerName(playerName);
	}
	
	public SpotifyConnectWebPlayer(String playerName) {
		this();
		setPlayerName(playerName);
	}

	@Override
	protected void onPlay() {
		webServer.sendMessage(gson.toJson(new WebSocketObject("play")));
	}

	@Override
	protected void onPause() {
		webServer.sendMessage(gson.toJson(new WebSocketObject("pause")));
	}

	@Override
	protected void onSeek(int millis) {
		webServer.sendMessage(gson.toJson(new WebSocketObject("seek", millis)));
	}

	@Override
	protected void onTrackChanged(Track track) {
		webServer.sendMessage(gson.toJson(new WebSocketObject("trackChanged", track)));
	}

	@Override
	protected void onNextTrack(Track track) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onPreviousTrack(Track track) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onShuffle(boolean enabled) {
		webServer.sendMessage(gson.toJson(new WebSocketObject("shuffle", enabled)));
	}

	@Override
	protected void onRepeat(boolean enabled) {
		webServer.sendMessage(gson.toJson(new WebSocketObject("repeat", enabled)));
	}

	@Override
	protected void onVolumeChanged(short volume) {
		webServer.sendMessage(gson.toJson(new WebSocketObject("volume", volume)));
	}

	@Override
	protected void onLoggedIn() {
		webServer.sendMessage(gson.toJson(new WebSocketObject("logged", true)));
	}

	@Override
	protected void onLoggedOut() {
		webServer.sendMessage(gson.toJson(new WebSocketObject("logged", false)));
	}
}
