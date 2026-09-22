# Ellipse

A home screen for Android, built with the platform and nothing else.

This is an alpha. It is a home screen in the system's eyes: screens of
things set by hand, every application in the same tile, a drawer of all
of them drawn up from below, a clock of its own with the weather, and
settings.

| Item | Value |
|---|---|
| Android | 8.0 and later (`minSdk 26`) |
| Package | `io.github.shumtugle.ellipse` |
| Licence | MIT |
| Dependencies | Android platform APIs only — no AndroidX, no component libraries, no Maven |
| Build | `aapt`, `javac`, `dalvik-exchange`, `zipalign`, `apksigner`; one shell script |
| Permissions | network, for the weather only; a rough place and the charge of headphones, each with the owner's leave |

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
inside applications, the door to the drawer, widgets and the clock. A long
press picks a thing up; moved, it follows the finger over a grid of dots
and lands where the grid lights up. Held at the edge of a screen it turns
to the next; let go over an application it makes a folder, over a folder
it joins it.

**The card.** A long press also grows a card out of the thing, in the
thing's own hue: the card starts as the thing's outline and opens into
itself. At its head, the name and what is done to the thing itself; under
it, tiles two to a row. For an application, the shortcuts it offers into
itself, marked in the card's own ink, and the screens inside it; a press
opens one, a long press lifts it onto a screen. For a widget or the clock,
its width, its height, its row and its column, each between a minus and a
plus, and a way to the middle of its row. A long press on bare ground
offers the door, the clock, a widget, another screen, the removal of an
empty one, the main screen, and the settings.

**The clock.** The home screen's own, cut from the same plate as the tiles,
in the same material, with the same glaze. A round window with a dial, or
the weather large; a long window with the hour and the date; small windows
with the weather or an application of the owner's choosing, the phone's
charge, and the charge of headphones while they are near. Each part can be
turned off on the clock's card, down to the date alone, which then stands
as the leaf of a calendar; the rest share the room.

**The weather.** For a city chosen by name, or for wherever the phone is,
roughly, with the owner's leave; a phone that has gone further than a
day's walk asks again at once. It is asked for when the home screen is
looked at and what is known is half an hour old, never in the background.
A press on the weather on the clock opens it whole, grown out of the pane
that was pressed: this moment, with the next two hours in quarters; wind,
humidity, pressure, the sun, the air, pollen, daylight and the magnetic
sky, each on its own pill of glass, in the material's colour when it asks
something of the reader; the next twelve hours; and the days, each drawn
against the span of them all.

**Widgets.** They live in their places on the screens and run to the edge
of the glass where they reach the grid's side, if the settings say so. A
sheet of pictures offers every widget the phone has, each with the cells
it will take. A widget brought in with a layout from elsewhere waits as an
outline until pressed.

**The drawer.** As it comes up, the screens step back behind it: smaller,
darker, out of focus; its corners square as it reaches the top, and its
tiles rise into place row after row. Four tiles to a row, as pages turned
sideways or one column scrolled down, by name, by installing or by the
last update. A field at its foot finds: names that begin with what is
typed first, then names with a word that does, then names that hold it;
what is typed is also read as if typed on the other keyboard layout, and
names are also read spelled in Latin letters.

**What is fresh.** Back on bare screens lowers the applications last opened
from here and those put on the phone or changed in the last two weeks.

**Under the finger.** Tiles sink and spring back past where they were;
buttons do the same, less.

**Immersion.** The home screen keeps both of the system's bars, or puts
away the top one, the bottom one, or both.

**The settings.** Split by subject: the first screen is the contents, each
line saying how its subject stands now; a subject slides in as a screen of
its own. Tile, screens, drawer, colour, weather, files, language. A choice
between a few things is a group of cards drawn with what they do; a dial
moves only by a finger drawn along it.

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

## Weather data

Weather and air quality data by [Open-Meteo.com](https://open-meteo.com/);
places by [GeoNames](https://www.geonames.org/). Both are offered under the
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) licence. The
weather is shown rounded to whole degrees and drawn as signs. The
geomagnetic index is by the [NOAA Space Weather Prediction
Center](https://www.swpc.noaa.gov/), in the public domain. The same credit,
with links, stands in the settings, on the clock's card and at the foot of
the weather screen.

## Licence

MIT. See `LICENSE`. Every mark and shape is drawn in the source; the three
material textures in `res/drawable-nodpi` are the author's own work and
come under the same licence.
