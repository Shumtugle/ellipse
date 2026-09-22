# Language modules

The home screen speaks English. Every other language is a module: one plain
text file that gives each word of the interface in that language. The home
screen carries no translations of its own.

A module is taken from **Settings → Language → Load a module**, or by the
import of the settings' files, which recognises a module by its first line.
A word left empty stays English, so a half-finished module is still usable.
A module made for an earlier version still loads; the words added since
show in English until a newer module is loaded.

## The files here

- `template.txt` — every key with its English source and an empty value.
  This is the form to fill.
- `ru.txt` — a complete module, as an example of tone and length.

## Rules for a module

**Encoding and lines.** UTF-8, lines ending in LF. One value per line. No
tabs inside a value. No markup, no quotation marks around values.

**The first line stays exactly as it is:**

```
# a language for this home screen
```

**The module line** names the language in the language itself:

```
module Suomi
```

**Each entry** is a comment with the English source, then the key, a tab,
and the value:

```
# Settings
settings	Asetukset
```

- Never change or translate a key.
- Keep every key, in the order of the template.
- Keep the comment lines as they are; they are the source, not decoration.
- A value may be left empty; the English is shown then.

**Tokens.** A few values carry tokens in braces, such as `{n}`, `{m}` or
`{w}`. They stay as they are and are filled in by the home screen.

**Length.** Most values sit on cards, capsules and tiles: keep them as short
as the language allows. Values whose key ends in `_what` are one-line
explanations and read as sentences.

**Days.** `sunday` to `saturday` are the names of the days as they stand
alone, on the weather's list of days.

## Checking a module

- the first line is unchanged;
- the number of key lines equals the template's;
- every key matches the template, in the same order;
- every token in braces is still there;
- no value contains a tab.

In the home screen, Settings → Language shows how many words are filled.
