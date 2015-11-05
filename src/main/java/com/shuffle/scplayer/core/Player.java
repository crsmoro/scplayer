package com.shuffle.scplayer.core;

public abstract class Player {
	protected abstract void onPlay();

	protected abstract void onPause();

	protected abstract void onSeek(int millis);

	protected abstract void onTrackChanged(Track track);

	protected abstract void onNextTrack(Track track);

	protected abstract void onPreviousTrack(Track track);

	protected abstract void onShuffle(boolean enabled);

	protected abstract void onRepeat(boolean enabled);

	protected abstract void onActive();

	protected abstract void onInactive();

	protected abstract void onTokenLost();

	protected abstract void onAudioFlush();

	protected abstract void onAudioData(byte[] data);

	protected abstract void onVolumeChanged(short volume);

	public abstract Track getPlayingTrack();
	
	public abstract boolean getShuffle();
	
	public abstract boolean getRepeat();
	
	public abstract short getVolume();
	
	public abstract int getSeek();
	
	public abstract String getAlbumCoverURL();
	
	public abstract boolean isPlaying();

	public abstract void setPlayerName(String playerName);
	
	public abstract String getPlayerName();

	public abstract void play();

	public abstract void pause();

	public abstract void prev();

	public abstract void next();

	public abstract void seek(int millis);

	public abstract void shuffle(boolean enabled);

	public abstract void repeat(boolean enabled);

	public abstract void volume(short volume);
	
	public abstract void login(String username, String password);
	
	public abstract void logout();
	
	protected abstract void onLoggedIn();
	
	protected abstract void onLoggedOut();
	
	public abstract boolean isLoggedIn();
}
