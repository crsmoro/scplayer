package com.shuffle.scplayer.core;

public interface AuthenticationListener {
	void onLoggedIn(String username);

	void onLoggedOut(String username);

	void onNewCredentials(String username, String blob);
}
