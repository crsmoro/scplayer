package com.shuffle.scplayer.web;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.WebSocketFrame;
import fi.iki.elonen.WebSocketFrame.CloseCode;

public abstract class WebSocket extends fi.iki.elonen.WebSocket {

	public WebSocket(IHTTPSession handshakeRequest) {
		super(handshakeRequest);
	}

	@Override
	protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {

	}

	@Override
	protected void onException(IOException e) {

	}

	@Override
	protected void onPong(WebSocketFrame pongFrame) {

	}

}
