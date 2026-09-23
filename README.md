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
phone's own applications, the phone's settings, and the door to this home
screen's settings. Screens
turn sideways, with points under them and a ring round the home one. A long press on a screen opens its menu beside
the finger: add a screen, and on any screen but the home one, make it the
home screen. Add widget opens the shelf of widgets: every app that offers any, one
card each, opening in place to show each widget as its own picture, with
its name, the places it takes and what it is for; a field at the top
narrows the shelf. A widget chosen is bound (the phone may ask leave), set
up if it wants that, and set down in the first free block of its size.
Widgets move like anything else on the screens. Adding shortcuts and
folders is offered but not yet done. Home returns to the home screen.

Gestures: a pull upward anywhere on the screens draws the list of every
application after the finger, and a pull downward on the list at its top
puts it back. Back on bare screens lowers what is fresh — the applications
last opened from here and those installed or updated in the last two weeks,
each marked new or updated — and a push upward sends it away. An icon
carried to a side edge and held there turns the screens.

The settings open from that door, or from the menu of a long press on a
screen: a field for finding at the top, and the rooms one under another. Desktop: the dock, the grid of the screens, endless scrolling, the
page points, and new apps set down by themselves. All apps: lines or
pages, the grid of a page, endless scrolling, and apps left out of the
list. Look: the clock (icon styles are to come). Gestures: what a swipe up,
a swipe down, Back and Home do on bare screens; a swipe down may lower the
notifications or the quick settings. Backup and restore, and languages,
are to come. Other: restart the launcher, or reset it to how it was on
first start (asked twice).

Anything standing on a screen, an app, a folder, the door to the settings
or the clock, is moved by a long press: it is taken up, carried, and set
down in a free place of any screen; let go anywhere else, it goes back.
The first move keeps the default set-out as the owner's own.

There are no folders of one's own yet, and what is set down cannot yet be moved or taken off. They return
one at a time.

- Android 8.0 or newer
- One permission: to lower the phone's own shade on a swipe down
- No dependencies: platform APIs only, built without Gradle

## Build

```sh
KEYSTORE=/path/to/key.keystore KSPASS=... SDK=/path/to/android-33.jar \
  sh build.sh ellipse-1.14.0
```

Needs `aapt`, `javac`, `dalvik-exchange`, `zipalign` and `apksigner`.

## License

MIT
