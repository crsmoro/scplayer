package com.shuffle.scplayer.core;

public class Track {
	private String name;
	private String uri;
	private int duration;
	private String artist;
	private String artistUri;
	private String album;
	private String albumUri;
	private String coverUri;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getArtistUri() {
		return artistUri;
	}

	public void setArtistUri(String artistUri) {
		this.artistUri = artistUri;
	}

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public String getAlbumUri() {
		return albumUri;
	}

	public void setAlbumUri(String albumUri) {
		this.albumUri = albumUri;
	}

	public String getCoverUri() {
		return coverUri;
	}

	public void setCoverUri(String coverUri) {
		this.coverUri = coverUri;
	}

	@Override
	public String toString() {
		return "Track [name=" + name + ", uri=" + uri + ", duration=" + duration + ", artist=" + artist + ", artistUri="
				+ artistUri + ", album=" + album + ", albumUri=" + albumUri + ", coverUri=" + coverUri + "]";
	}
}
