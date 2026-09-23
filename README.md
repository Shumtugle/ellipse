# Ellipse

A home screen for Android, drawn by hand.

Version 1.x is a fresh start: one screen and nothing else. The wallpaper
shows through a rounded window; a grid of four by five stands inside it,
a dock of five under it, and a bar at the foot with a search field and a
round button. What stands on the screen is what the phone itself keeps
for each everyday role — phone, messages, browser, camera, gallery in the
dock; calendar, clock, maps, files, store, mail, music and settings on the
grid. Icons are the system's own, without masks.

A tap on the search field brings up every application, by name, one to a
line, on plain black; Back or Home puts it away.

There are no settings, no folders and no saved layout yet. They return
one at a time.

- Android 8.0 or newer
- No permissions
- No dependencies: platform APIs only, built without Gradle

## Build

```sh
KEYSTORE=/path/to/key.keystore KSPASS=... SDK=/path/to/android-33.jar \
  sh build.sh ellipse-1.1.0
```

Needs `aapt`, `javac`, `dalvik-exchange`, `zipalign` and `apksigner`.

## License

MIT
