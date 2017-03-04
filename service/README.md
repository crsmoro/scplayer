# Arch linux
Download the service <a href="https://github.com/crsmoro/scplayer/service/scplayer.service">file</a><br>
Put the file at `/etc/systemd/system`

Credits to <a href="https://github.com/severeCavok">@severeCavok</a>
<br><br>
#Debian based linux
Download the service <a href="https://github.com/crsmoro/scplayer/service/scplayer">file</a><br><br>
Put the file at `/etc/init.d`

To Active

```
sudo update-rc.d scplayer defaults
```

To Inactive

```
sudo update-rc.d -f scplayer remove
```

Credits to <a href="https://github.com/tokazio">@tokazio</a>