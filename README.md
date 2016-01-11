# SCPlayer - Spotify Connect Player
Open source client of Spotify Connect closed library developed in Java

I decided to make a Java version because i needed the player to run without any privileged or any additional installation.<br/><br/>

This version is *working with Raspberry Pi 2* with the hard float version of the spotify's library<br/><br/>
There is a version to run in Raspberry Pi 1 <a href="https://github.com/crsmoro/scplayer/tree/armel" target="_blank">here</a>, but it only works in a soft float system (you will have to install Raspbian armel or emulate with chroot, docker, etc...). This version will only be updated after a while the main version.<br/><br/>
It's possible that it will be run other devices if you use the right version of spotify's library.<br/>

If you managed to run on others devices, or need help to, let me know. 


Quick run / I just want to use
---------------
1. Download Java, <a href="http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html" target="_blank">link here</a>
2. Get apropriate version of spotify lib, you can find all versions on <a href="https://github.com/sashahilton00/spotify-connect-resources" target="_blank">this github</a>, to run in Raspberry Pi 2 is <a href="https://github.com/sashahilton00/spotify-connect-resources/raw/master/armhf%20version/libspotify_embedded_shared.so">this one</a>
3. Get your spotify key, <a href="https://devaccount.spotify.com/my-account/keys/" target="_blank">here</a>, download the binary version
4. Get SCPlayer jar, <a href="https://github.com/crsmoro/scplayer/raw/master/SCPlayer.jar">here</a>
5. Put all on the same folder
6. Run using the command /path/to/java -jar SCPlayer.jar
7. From a browser access: http://IP:4000 and login with your login clicking on the right top conner


Detailed Usage
---------------
Java options -D&lt;option&gt;=&lt;value&gt;

* player.name - Set the player name, will appears on all your connect devices (Default will be SCPlayer)<br/>
* username - Set username<br/>
* password - Set password<br/>
* remember.me - Save your credentials for the next start (true/false), defaults to true<br/>
* standalone - Runs without the web version (true/false)<br/>
* debug - Set debug level - 0 > WARN, 1 > INFO, 2 > DEBUG, 3 > TRACE<br/>
* app.key - Path to your spotify_appkey.key<br/>
* list.mixers - List the available mixers for use<br/>
* mixer - Set the mixer, you can use either the name or index provided by list.mixers<br/>
* bitrate - Set the bitrate (90/160/320)

Example: `./jdk1.8.0_60/bin/java -Dplayer.name=Kitchen -Dmixer=0 -Dapp.key=/home/pi/spotify_appkey.key -jar SCPlayer.jar`

Troubleshooting
----------------

If you are running on Openelec like I am, you have to run this command to enable alsa drivers<br/>
from ssh type `echo snd_bcm2835 >> /storage/.config/modules-load.d/audio.conf`<br/>
and reboot

If you are running on OSMC you have to run this command to enable alsa drivers<br/>
for older versions from ssh type `sudo modprobe snd_bcm2835` , no need to reboot<br/>
for newer versions add this line `dtparam=audio=on` to `/boot/config.txt`



Disclaimer
----------------
Using this code to connect to Spotify's API is probably forbidden by them, and might result in your application key getting banned. Use at you own risk
