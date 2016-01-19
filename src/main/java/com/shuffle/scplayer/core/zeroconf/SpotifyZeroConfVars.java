package com.shuffle.scplayer.core.zeroconf;

public class SpotifyZeroConfVars {
    private String publicKey;
    private String deviceId;
    private String activeUser;
    private String remoteName;
    private String accountReq;
    private String deviceType;
    private String libraryVersion;

    public SpotifyZeroConfVars(String publicKey, String deviceId, String activeUser,
                               String remoteName, String accountReq, String deviceType,
                               String libraryVersion) {
        this.publicKey = publicKey;
        this.deviceId = deviceId;
        this.activeUser = activeUser;
        this.remoteName = remoteName;
        this.accountReq = accountReq;
        this.deviceType = deviceType;
        this.libraryVersion = libraryVersion;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getActiveUser() {
        return activeUser;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public String getAccountReq() {
        return accountReq;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getLibraryVersion() {
        return libraryVersion;
    }
}
