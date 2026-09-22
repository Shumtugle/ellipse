# Releasing

A release is one signed APK, one tag and one updated module. Nothing else
is generated and nothing is published automatically.

## 1. Before building

- `AndroidManifest.xml`: raise `android:versionName` and
  `android:versionCode` (the code grows by one with every build given out).
- `modules/ru.txt` and `modules/template.txt`: regenerate if the dictionary
  grew. Every key in a module must match `Words.IDS`, in order, with the
  English source as the comment above it.
- The README says what the version can do, in the same voice as the rest.
- Check the source is sterile: no personal names, no third-party product
  names, no URLs in comments, no non-Latin text outside `modules/`. The
  addresses of the weather services live only in string constants in
  `Sky.java`; their credit lives in the dictionary and the README, because
  their licence asks for it.

## 2. Build

```sh
KEYSTORE=../keys/your.keystore KSPASS=<password> SDK=$HOME/sdk/android-33.jar \
  sh build.sh ellipse-<version>
```

The script prints the signer's SHA-256; it must be the same every time.

## 3. Install and try

Install over the previous version: the package name never changes, so the
layout, the settings and the placed widgets stay. Walk the paths the
version touched, and one that it did not. Load the new language module if
the dictionary grew: a module missing a word shows that word in English.

## 4. Tag and publish

```sh
git add -A
git commit -m "<version>: <one line>"
git tag v<version>
git push origin main --tags
```

On GitHub: **Releases → Draft a new release → choose the tag**. Title
`Ellipse <version>`; the body says what changed, in English, as plain
sentences; attach `out/ellipse-<version>.apk` and the modules.

## 5. After

- Keep the signing key. An APK signed with another key cannot replace this
  one on a phone: the owner would have to remove the home screen and lose
  its layout.
- `keys/` is never committed.
