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

Three screens stand side by side from the start, the middle one home.
The home screen carries the home screen's own clock across its top row, drawn
the design system's way — a scalloped dial, the hour and the date, the
phone's charge — and four everyday applications at its foot. The screen
before it holds a folder of the applications signed by the same hand as
the phone's store, named after them; the screen after it, a folder of the
phone's own applications, the phone's settings, and a door to this home
screen's settings, still to come. Screens
turn sideways, with points under them and a ring round the home one. A long press on a screen opens its menu beside
the finger: add a screen, and on any screen but the home one, make it the
home screen. Adding shortcuts, widgets and folders is offered but not yet
done. Home returns to the home screen.

Gestures: a pull upward anywhere on the screens draws the list of every
application after the finger, and a pull downward on the list at its top
puts it back. Back on bare screens lowers what is fresh — the applications
last opened from here and those installed or updated in the last two weeks,
each marked new or updated — and a push upward sends it away. An icon
carried to a side edge and held there turns the screens.

There are no settings and no folders yet, and what is set down cannot yet be moved or taken off. They return
one at a time.

- Android 8.0 or newer
- No permissions
- No dependencies: platform APIs only, built without Gradle

## Build

```sh
KEYSTORE=/path/to/key.keystore KSPASS=... SDK=/path/to/android-33.jar \
  sh build.sh ellipse-1.8.1
```

Needs `aapt`, `javac`, `dalvik-exchange`, `zipalign` and `apksigner`.

## License

MIT
