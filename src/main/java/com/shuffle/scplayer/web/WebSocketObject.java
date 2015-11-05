package com.shuffle.scplayer.web;

public class WebSocketObject {
	private String action;
	private Object data;

	public WebSocketObject(String action) {
		super();
		this.action = action;
	}

	public WebSocketObject(String action, Object data) {
		super();
		this.action = action;
		this.data = data;
	}

	public WebSocketObject() {
		super();
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
}
