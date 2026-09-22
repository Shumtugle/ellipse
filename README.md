# Ellipse

A home screen for Android, built with the platform and nothing else.

This is an alpha. It is a home screen in the system's eyes: screens of
things set by hand, every application in the same tile, a drawer of all
of them drawn up from below, a dock along the foot, and
settings.

| Item | Value |
|---|---|
| Android | 8.0 and later (`minSdk 26`) |
| Package | `io.github.shumtugle.ellipse` |
| Licence | MIT |
| Dependencies | Android platform APIs only — no AndroidX, no component libraries, no Maven |
| Build | `aapt`, `javac`, `dalvik-exchange`, `zipalign`, `apksigner`; one shell script |
| Permissions | none |

## What there is

**The tile.** Every icon is seen through the same window. A tile is built
in three layers: a plate of the rim's material, lit from above and shaded
below; the icon, seen through a window cut in it, either following the
tile's shape or round, a medallion; and, if asked for, the curved glaze of
glass across the top. The shape is a superellipse, one formula with a
roundness and a proportion, so it changes without a picture being redrawn
by hand. The plates are metal, gold, wood, sequins, black, steel, the
seed's colour, or none; the rim runs from a thread to a frame. Or no mask
at all: every icon as its own application draws it. Icons made of two
layers fill the window; flat old icons stand on white paper.

**The screens.** Side by side, turned like pages, each a grid; they stop at
the ends or go round. On them: applications, folders, shortcuts, screens
inside applications, the door to the drawer and widgets. A long
press picks a thing up; moved, it follows the finger over a grid of dots
and lands where the grid lights up. Held at the edge of a screen it turns
to the next; let go over an application it makes a folder, over a folder
it joins it.

**The card.** A long press also grows a card out of the thing, in the
thing's own hue: the card starts as the thing's outline and opens into
itself. At its head, the name and what is done to the thing itself; under
it, tiles two to a row. For an application, the shortcuts it offers into
itself, marked in the card's own ink, and the screens inside it; a press
opens one, a long press lifts it onto a screen. A widget lifted also
wears a frame with a handle on each of
its four sides: a handle drawn outwards takes another row or column of the
grid, drawn inwards gives one back, and the thing is laid out again under
the finger. A handle that cannot move, because the cells beyond it are
taken or the grid ends there, does not move. For a widget,
its width, its height, its row and its column, each between a minus and a
plus, and a way to the middle of its row. The door's card leads to its
settings. A long press on bare ground
offers the door, a widget, another screen, the removal of an
empty one, the main screen, and the settings.

**Default.** One switch at the head of the settings sets the home screen
as the design system's own guidelines have it, and as a new phone first
sees it: a grid four across and five down, two rows low on the first
screen of what every phone of the kind has,
a camera, pictures, a browser, maps, a calendar, alarms, a shop and the
system's settings, and a dock holding the door, the phone's calls, its
messages and its contacts, each found by the part it plays on that phone
and never by its name;
icons in the system's own shape with no plate over them, colour from the
wallpaper, the drawer scrolling down in the order of names, screens that
stop at their ends. The default is drawn the design system's own way:
the settings and the shelf flat, in the colours of the scheme's
roles and the corners of its shape scale, the titles in the system's sans,
the dials as its sliders; and the door a blue
disc. What was the owner's own, its layout with it, is set aside whole and
comes back with the same switch.

**The dock.** One row along the foot, on a shelf of its own apart from the
ground, the grid's own columns standing under the grid, the same whichever
screen is in view. Anything one place
large is carried into it and out of it like anywhere else; folders are
made in it the same way. It steps back with the screens when the drawer
opens over them. It is turned on in the settings of the
screens.

**Falls.** When something in the home screen breaks, the trace is written
at once, with the version, the phone, the settings and the last of the
home screen's own log, and carried into the folder the owner gave, or,
without one, into a folder of its own among the phone's downloads. The
last of them is kept by the home screen as well, which offers, at its next
start, to hand the report on wherever the owner likes.

**The file.** One readable text file carries the layout of the screens,
every setting, and the language in use. It goes into a folder the owner
gives once and comes back from a list of what lies there. The same import
reads a language module, and a backup of another home screen: a zip whose
database has one of two known shapes, told apart by its tables and not by
any name.

**Languages.** English lives inside; every other language is a text module,
loaded from the settings. A module carries, above every word, the English it
was made from; a word it lacks stays English.

## Building

```sh
KEYSTORE=../keys/your.keystore KSPASS=<password> SDK=$HOME/sdk/android-33.jar \
  sh build.sh ellipse-<version>
```

## Licence

MIT. See `LICENSE`. Every mark and shape is drawn in the source; the three
material textures in `res/drawable-nodpi` are the author's own work and
come under the same licence.
