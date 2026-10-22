#!/bin/sh
# Compila Mando Aire sin Gradle: aapt2 + javac + d8 + apksigner.
set -e
cd "$(dirname "$0")"
B=..
export JAVA_HOME=$(echo $B/jdk-*) PATH=$(echo $B/jdk-*)/bin:$PATH
BT=$B/sdk/build-tools/34.0.0
JAR=$B/sdk/platforms/android-34/android.jar
rm -rf out && mkdir -p out/classes out/dex
$BT/aapt2 link -o out/base.apk --manifest AndroidManifest.xml -I $JAR
javac -source 8 -target 8 -bootclasspath $JAR:$BT/core-lambda-stubs.jar -Xlint:-options -d out/classes $(find src -name '*.java')
$BT/d8 --release --min-api 19 --lib $JAR --output out/dex $(find out/classes -name '*.class')
cp out/base.apk out/unsigned.apk
python3 -c "import zipfile; zipfile.ZipFile('out/unsigned.apk', 'a', zipfile.ZIP_DEFLATED).write('out/dex/classes.dex', 'classes.dex')"
$BT/zipalign -f 4 out/unsigned.apk out/aligned.apk
[ -f debug.keystore ] || keytool -genkeypair -keystore debug.keystore -storepass android \
  -keypass android -alias mandoaire -keyalg RSA -validity 10000 -dname "CN=Mando Aire" >/dev/null 2>&1
$BT/apksigner sign --ks debug.keystore --ks-pass pass:android --out MandoAire.apk out/aligned.apk
$BT/apksigner verify MandoAire.apk && ls -la MandoAire.apk
