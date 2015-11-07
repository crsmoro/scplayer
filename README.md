# scplayer - Spotify Connect Player
Open source client of Spotify Connect closed library developed in Java

I decided to make a Java version because i needed the player to run without any privileged or any additional installation.

Why: I have to run on the same Raspberry that my media center, in this case Kodi, that i run over Openelec. As Openelec is built only to run Kodi (and that what makes it so good) and i didn't wanna mess up everything rebuilding the whole system to allow to install other packages ruining all. So I decided to develop a java version which fit perfect the requirement (No privileged, No installation, No additional libs).

Quick run / I just want to use
---------------
1 - Download Java, <a href="http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html" target="_blank">link here</a>

2 - Get apropriate version of spotify lib, you can find all versions in <a href="https://github.com/sashahilton00/spotify-connect-resources" target="_blank">this github</a>, if you dont know which one, its problably <a href="https://github.com/sashahilton00/spotify-connect-resources/raw/master/armhf%20version/libspotify_embedded_shared.so">this one</a>

3 - Get your spotify key, <a href="https://devaccount.spotify.com/my-account/keys/" target="_blank">here</a>, download the binary version

4 - Get SCPlayer jar, <a href="https://github.com/crsmoro/scplayer/raw/master/SCPlayer.jar">here</a>

4 - Put all on the same folder

5 - Run using the command /path/to/java -jar SCPlayer.jar

6 - From a browser access: http://IP:4000 and login with your login clicking on the right top conner


Detailed Usage
---------------
Java options -D&lt;option&gt;=&lt;value&gt;

playerName - Set the player name, will appears on all your connect devices (Default will be SCPlayer)<br/>
username - Set username<br/>
password - Set password<br/>
standalone - Runs wihtout the web version (true/false)<br/>
debug - Set debug level - 0 > WARN, 1 > INFO, 2 > DEBUG, 3 > TRACE<br/>
appKey - Location of your spotify_appkey.key


Troubleshooting
----------------
If you connect using a facebook account, librespot will not show up among the devices in the Spotify app. What you need to do is apply for a <a href="http://www.spotify.com/account/set-device-password/" target="_blank">device password</a> and use that to sign in instead.

Disclaimer
----------------
Using this code to connect to Spotify's API is probably forbidden by them, and might result in you application key getting banned. Use at you own risk
