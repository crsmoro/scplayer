package com.shuffle.scplayer.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shuffle.scplayer.core.PlayerListener;
import com.shuffle.scplayer.core.SpotifyConnectPlayer;
import com.shuffle.scplayer.core.Track;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Map;

/**
 * @author LeanderK
 * @version 1.0
 */
public class PlayerWebServerIntegration implements PlayerListener {

    private static final transient Log log = LogFactory.getLog(PlayerWebServerIntegration.class);

    private WebServer webServer;

    private final transient Gson gson = new GsonBuilder().create();
    
    public PlayerWebServerIntegration(final SpotifyConnectPlayer spotifyConnectPlayer) {
    	this(spotifyConnectPlayer, 4000);
    }

    public PlayerWebServerIntegration(final SpotifyConnectPlayer spotifyConnectPlayer, int port) {
        final WebServer webServer = new WebServer(port) {

            @Override
            protected void onMessage(String message, com.shuffle.scplayer.web.WebSocket webSocket) {
                try
                {
                    WebSocketObject webSocketObject = gson.fromJson(message, WebSocketObject.class);
                    switch (webSocketObject.getAction()) {
                        case "play":
                            spotifyConnectPlayer.play();
                            break;
                        case "pause":
                            spotifyConnectPlayer.pause();
                            break;
                        case "prev":
                            spotifyConnectPlayer.prev();
                            break;
                        case "next":
                            spotifyConnectPlayer.next();
                            break;
                        case "seek":
                            spotifyConnectPlayer.seek(((Double) webSocketObject.getData()).intValue());
                            break;
                        case "shuffle":
                            spotifyConnectPlayer.shuffle((boolean) webSocketObject.getData());
                            break;
                        case "repeat":
                            spotifyConnectPlayer.repeat((boolean) webSocketObject.getData());
                            break;
                        case "volume":
                            spotifyConnectPlayer.volume((short) webSocketObject.getData());
                            break;
                        case "playername":
                            spotifyConnectPlayer.setPlayerName((String) webSocketObject.getData());
                            webSocket.send(gson.toJson(new WebSocketObject("playername", spotifyConnectPlayer.getPlayerName())).getBytes("UTF-8"));
                            break;
                        case "login":
                            @SuppressWarnings("unchecked")
                            Map<String, String> mapLogin = (Map<String, String>) webSocketObject.getData();
                            spotifyConnectPlayer.login(mapLogin.get("username"), mapLogin.get("password"));
                            break;
                        case "logout":
                            spotifyConnectPlayer.logout();
                            break;
                        case "track":
                            Track track = spotifyConnectPlayer.getPlayingTrack();
                            WebSocketObject webSocketReturnObject = new WebSocketObject("track", track);
                            webSocket.send(gson.toJson(webSocketReturnObject).getBytes("UTF-8"));
                            break;
                        case "getShuffle":
                            webSocket.send(gson.toJson(new WebSocketObject("shuffle", spotifyConnectPlayer.getShuffle())).getBytes("UTF-8"));
                            break;
                        case "getRepeat":
                            webSocket.send(gson.toJson(new WebSocketObject("repeat", spotifyConnectPlayer.getRepeat())).getBytes("UTF-8"));
                            break;
                        case "isPlaying":
                            webSocket.send(gson.toJson(new WebSocketObject("playing", spotifyConnectPlayer.isPlaying())).getBytes("UTF-8"));
                            break;
                        case "isLoggedIn":
                            webSocket.send(gson.toJson(new WebSocketObject("logged", spotifyConnectPlayer.isLoggedIn())).getBytes("UTF-8"));
                            break;
                        case "getAlbumCover":
                            webSocket.send(gson.toJson(new WebSocketObject("albumcover", spotifyConnectPlayer.getAlbumCoverURL())).getBytes("UTF-8"));
                            break;
                        case "getPlayerName":
                            webSocket.send(gson.toJson(new WebSocketObject("playername", spotifyConnectPlayer.getPlayerName())).getBytes("UTF-8"));
                            break;
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

    public WebServer getWebServer() {
        return webServer;
    }

    @Override
    public void onActive() {

    }

    @Override
    public void onInactive() {

    }

    @Override
    public void onTokenLost() {

    }

    @Override
    public void onPlay() {
        webServer.sendMessage(gson.toJson(new WebSocketObject("play")));
    }

    @Override
    public void onPause() {
        webServer.sendMessage(gson.toJson(new WebSocketObject("pause")));
    }

    @Override
    public void onSeek(int millis) {
        webServer.sendMessage(gson.toJson(new WebSocketObject("seek", millis)));
    }

    @Override
    public void onTrackChanged(Track track) {
        webServer.sendMessage(gson.toJson(new WebSocketObject("trackChanged", track)));
    }

    @Override
    public void onNextTrack(Track track) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPreviousTrack(Track track) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onShuffle(boolean enabled) {
        webServer.sendMessage(gson.toJson(new WebSocketObject("shuffle", enabled)));
    }

    @Override
    public void onRepeat(boolean enabled) {
        webServer.sendMessage(gson.toJson(new WebSocketObject("repeat", enabled)));
    }

    @Override
    public void onVolumeChanged(short volume) {
        webServer.sendMessage(gson.toJson(new WebSocketObject("volume", volume)));
    }

    @Override
    public void onLoggedIn() {
        webServer.sendMessage(gson.toJson(new WebSocketObject("logged", true)));
    }

    @Override
    public void onLoggedOut() {
        webServer.sendMessage(gson.toJson(new WebSocketObject("logged", false)));
    }
}
