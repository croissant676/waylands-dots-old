# Waybar configs

These were my personal waybar configs, you can see them below:
![img_3.png](img_3.png)

This repo contains 3 custom modules:

### Pomodoro 
Under `pomodoro_kt/src` there should be a kotlin file (`Main.kt`). When compiled, it will act
as a pomodoro server. To actually make the server run, compile the file into a jar
(or download it, it's `pomodoro/.config.jar`), then execute it using `java -jar`

> [!NOTE]  
> If you're using hyprland, as I am, add the pomodoro launch command or script as an `exec-once`.

For each module, hovering over it should tell you all the actions you can do with it. 

#### Custom session list
The pomodoro server relies on a pomodoro session list file, and will auto-generate one if 
missing. The tooltip should tell you the location of this file. Each line should simply
be the name of the session and the number of seconds, separated by a tab. For example, the default
pomodoro session listk, which is `4*(25 mins work, 5 mins break), 30 mins break` is:

```
work	1500
break	300
work	1500
break	300
work	1500
break	300
work	1500
break	300
break	1800
```

If you update the session list and want the server to read the new version, scroll click. This
will lose all progress in the current session list (reset to session 1, 0 time).

### Media

This media player is very similar to the sample one in waybar, but also displays
the track time, limits the length of the module, and uses emojis to represent the play/pause
status. It should work out of the box using the config file included (this also goes for the 
pomodoro server, as long as you execute it). The file is `media.py`.

### Date time

This is a simple bash script that shows the current time & date, with a tooltip that displays
the calendar. Running this will create 2 extra files: `date_store.dat`and `tt_store.dat`, which
act as caches for the calendar tooltip. If these files annoy you, I recommend moving the 
file locations specified in the `clock-script.sh` to somewhere else. Deleting the logic for the
caching means this script will regenereate the calendar and color it every second, which may
be expensive for some computers.

If you have any questions, contact me on discord (@croissant676).
