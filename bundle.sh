#!/bin/sh
# An app bundle for the store, from the same sources as build.sh.
# Needs: AAPT2 (the resource tool, second make), BUNDLETOOL (its jar),
# SDK (the platform jar), KEYSTORE and KSPASS as for build.sh.
set -e
SDK=${SDK:-$HOME/sdk/android-33.jar}
NAME=${1:-ellipse}
OUT=$PWD/out
W=build/bundle
rm -rf "$W"; mkdir -p "$W/gen" "$W/classes" "$W/linked" "$W/module/manifest" "$W/module/dex" "$OUT"

"$AAPT2" compile --dir res -o "$W/res.zip"
"$AAPT2" link --proto-format -o "$W/linked.zip" -I "$SDK" --manifest AndroidManifest.xml -A assets \
  --java "$W/gen" --auto-add-overlay "$W/res.zip"
javac -source 8 -target 8 -bootclasspath "$SDK" -classpath "$SDK" \
  -d "$W/classes" -encoding UTF-8 -nowarn $(find src "$W/gen" -name '*.java')
dalvik-exchange --dex --min-sdk-version=26 --output="$W/module/dex/classes.dex" "$W/classes"

(cd "$W/linked" && unzip -q ../linked.zip)
cp "$W/linked/AndroidManifest.xml" "$W/module/manifest/"
cp "$W/linked/resources.pb" "$W/module/"
cp -r "$W/linked/res" "$W/module/"
[ -d "$W/linked/assets" ] && cp -r "$W/linked/assets" "$W/module/"
(cd "$W/module" && zip -q -r ../base.zip .)

java -jar "$BUNDLETOOL" build-bundle --modules="$W/base.zip" --output="$OUT/$NAME.aab" --overwrite
ALIAS=$(keytool -list -keystore "$KEYSTORE" -storepass "$KSPASS" | grep -i PrivateKeyEntry | cut -d, -f1)
jarsigner -keystore "$KEYSTORE" -storepass "$KSPASS" -sigalg SHA256withRSA -digestalg SHA-256 \
  "$OUT/$NAME.aab" "$ALIAS" >/dev/null
java -jar "$BUNDLETOOL" validate --bundle="$OUT/$NAME.aab" >/dev/null
ls -la "$OUT/$NAME.aab"
