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
updated first, and the settings are a touch away. Lines or pages (of four
by five icons with their names) are chosen in the settings.

Three screens stand side by side from the start, the middle one home.
The home screen carries the home screen's own clock across its top row, with the weather of a chosen place and the charge of headphones near, drawn
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
Widgets move like anything else on the screens. A widget whose own
layout will not come down to the places it has is laid out as large as it
needs and drawn smaller, the same both ways, so nothing is cut off; one
that fits is left as it is.

A long press on anything opens its menu, pointing at it, under a head
with its name and a round button. The first face is what the app itself
offers, its own shortcuts with their pictures; the gear turns the card to
the second face, the home screen's own: Uninstall, Rename, Resize, and,
apart, Remove in the colour of taking away; there the round button opens
the app's information. Moving the finger on
closes the menu and carries the thing. The round button at the dock's end
becomes a bin while something is carried from a place; it is taken off (a widget lets go of its
place, the clock is switched off in the settings). A widget or a folder
held and let go where it stood is offered to reshape: a frame whose sides
are taken anywhere along them, a corner taking two, drawn across whole
places, never over what stands beside it; a touch outside ends it. A folder larger than one place opens up on the
screen as a card with its apps in it, two small icons to a place; the
last place shows how many more there are. Apps can be set into the
dock, and taken out of it. Add folder makes a folder of one's own, named
at once; apps are dropped into it. Add shortcut offers the door to this home
screen's own settings first, then every app's own shortcut makers. A screen with nothing on it can be removed. Other apps
can ask to put a shortcut or a widget on the home screen: a card asks,
and it is set down in the first free place. Apps of a work profile are
listed with their badge. An app that leaves the phone leaves no trace. Home returns to the home screen.

Gestures: a pull upward anywhere on the screens draws the list of every
application after the finger, and a pull downward on the list at its top
puts it back. Back on bare screens lowers what is fresh — the applications
last opened from here and those installed or updated in the last two weeks,
each marked new or updated — and a push upward sends it away. An icon
carried to a side edge and held there turns the screens.

The settings open from that door, or from the menu of a long press on a
screen: the rooms one under another.

Every screen that lists things — all apps, the shelf of widgets, the
makers of shortcuts, the settings — has the same bar at its foot: a field
saying what it finds, and a round button with three marks for the
screen's menu; open, the marks become a cross, and the cross leads home. Desktop: the dock, the grid of the screens, endless scrolling, the
page points, and new apps set down by themselves. All apps: lines or
pages, the grid of a page, endless scrolling, and apps left out of the
list. Look: the clock (icon styles are to come). Gestures: what a swipe up,
a swipe down, a double tap (lock the phone), Back and Home do on bare screens; a swipe down may lower the
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
- Services the owner turns on, or not: an accessibility service that only locks the phone on a double tap and reads nothing; a notification listener that only knows which apps have a notification, for the dots
- Permissions: to lower the phone's own shade on a swipe down; to ask for an app to be uninstalled; for the clock, the phone's rough place and the network (the weather) and nearby devices (the headphones' charge) — each asked for once, and the clock does without what is refused
- No dependencies: platform APIs only, built without Gradle

## Build

```sh
KEYSTORE=/path/to/key.keystore KSPASS=... SDK=/path/to/android-33.jar \
  sh build.sh ellipse-1.29.2
```

Needs `aapt`, `javac`, `dalvik-exchange`, `zipalign` and `apksigner`.

## License

MIT

## Colour and the weather as pages

Look → Colour and text opens the colour page. The accent follows the phone
at first — its system colour where it has one, else the wallpaper's — or
is mixed by hand: a hue, how rich it is, how
bright the accent, how solid the cards stand, how much colour the ground
takes, and the size of words, all mixed under the thumb, with a window at
the head of the page onto the wallpaper and the home screen's own things
on it — the clock, a row of the owner's apps, the dock, a menu — drawn
anew while a slider moves, before anything is kept. The whole home screen follows it.

A touch on the clock's weather opens the weather, whole: this moment with
the next two hours in quarters, the small facts as capsules (wind, damp,
pressure, the sun, the air, pollen, light, geomagnetic activity), the next
hours and the days. Its place is found by name, or taken once from where
the phone stands, with leave; nothing follows the phone about. The
addresses asked live in the package, not in the source.

## Icons

Look → Icons cuts every icon to one outline: the phone's own, a circle, a
squircle (a superellipse of the fifth power), a rounded square, a drop
square on one corner (any of the four), or the paper tile — a wide tile of
grained paper in a thin grey rim, on which every icon lies, flat ones too.
Flat icons are set into any outline: spread to fill it when they are solid
to their edges, otherwise standing on a ground of the colour their edges
carry. Folder faces, the door to every app in the dock, the settings' own
door and pinned shortcuts wear the same outline. In the same room: how
large the icons stand, how much of the outline the picture fills, names
under the icons on the screens and in the list (each on or off), how large
the names are, and the typeface every word of the home screen is set in —
one of the phone's own families. The window at the head follows each
slider while it moves. Icons can keep their own colours, or be laid in the
accent: an app's own one-colour picture where it drew one, on a deep
ground of the same hue — or, for every icon, one made from its picture;
on the paper tile the ink is the accent darkened, on white. Icons
that come in layers are laid out as the platform lays them and cut by the
outline; each outline is drawn a little smaller the more of its square it
fills, so a grid weighs the same whatever the outline. A window at the
head of the room shows the owner's own icons in the outline chosen.
