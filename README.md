# Ellipse

A home screen for Android, drawn by hand.

Version 1.x is a fresh start: one screen and nothing else. The wallpaper
shows through a rounded window with a grid of four by five inside it;
under it the bar at the foot is the dock, four places and a round button.
What stands on the screen is what the phone itself keeps for each everyday
role — phone, messages, gallery and camera in the dock; browser, calendar,
clock, maps, files, store, mail, music and settings on the grid. Icons are
the system's own, without masks.

The round button, marked with six dots, brings up every application, by name, one to a
line, on plain black, with a field at the foot to narrow the list. Held
long, a line gives its icon to the finger; the list closes into the
fingertip, and the icon is set down in any free place of the grid.
Back or Home puts the list away. Its round button, with three dots, opens
the list's menu: the list stands A to Z, newest first, or most recently
updated first, and either runs down in lines or across in pages of four
by five icons with their names.

Screens stand side by side and turn sideways, with points under them and
a ring round the home one. A long press on a screen opens its menu beside
the finger: add a screen, and on any screen but the home one, make it the
home screen. Adding shortcuts, widgets and folders is offered but not yet
done. Home returns to the home screen.

There are no settings and no folders yet, and what is set down cannot yet be moved or taken off. They return
one at a time.

- Android 8.0 or newer
- No permissions
- No dependencies: platform APIs only, built without Gradle

## Build

```sh
KEYSTORE=/path/to/key.keystore KSPASS=... SDK=/path/to/android-33.jar \
  sh build.sh ellipse-1.6.0
```

Needs `aapt`, `javac`, `dalvik-exchange`, `zipalign` and `apksigner`.

## License

MIT
