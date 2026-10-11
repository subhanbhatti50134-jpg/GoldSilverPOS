# Gold & Silver Laboratory Management App

Native Android source project using Kotlin, Jetpack Compose, Material 3, Room/SQLite, MVVM and Coroutines/Flow. Package: `com.goldlab.management`.

## Environment inspection performed for this bundle

The build environment available during generation was Debian 13 with OpenJDK 21. Visual Studio, .NET SDK, Gradle, Android SDK (`sdkmanager`) and `adb` were not detected. No pre-existing project files were found in the inspected paths. The APK has **not** been built or device-tested in this environment.

## Open and edit

Open this folder in Visual Studio as a folder-based source project. Visual Studio is an editor here; it is not a native Kotlin/Compose project system. Use its terminal for the Android command-line build. Visual Studio Code or another Kotlin-aware editor may provide better Kotlin support, but the application remains Kotlin/Compose.

## Required build tools

- JDK 17 or 21
- Android SDK command-line tools, platform 35, build-tools 35.0.0
- Gradle 8.11.1
- Internet access for dependency resolution on first build

Set `JAVA_HOME` to the JDK and `ANDROID_HOME` to your Android SDK directory; add `$ANDROID_HOME/platform-tools` and `$ANDROID_HOME/cmdline-tools/latest/bin` to PATH. Install Android SDK packages with `sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"` after installing command-line tools.

The wrapper properties are included. Since this generation environment had no Gradle installation and no network access, the official wrapper JAR could not be bootstrapped. Install Gradle 8.11.1 once and run `gradle wrapper --gradle-version 8.11.1`; commit the generated `gradle/wrapper/gradle-wrapper.jar` and standard wrapper scripts. Then run:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Windows PowerShell:

```powershell
gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug
```

Expected debug APK path after a successful build: `app/build/outputs/apk/debug/app-debug.apk`.
Install on a connected Android device after enabling USB debugging:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Implemented in this source bundle

- Native Compose Material 3 gold/black shell and dashboard
- Room entities/DAOs for customers, purchases, payments, audit log and settings
- Offline purchase and contact storage
- Gold loose purchase, gold biscuit and silver biscuit entry forms
- Decimal-safe gram/tola/kilogram conversions and per-gram/per-tola rate calculation
- Transaction search across ID, party, company and serial number
- Purchase deletion confirmation and recent-transaction listing
- Initial paid amount record and outstanding calculations
- Basic live reports based on Room purchase records
- Unit tests for core weight/rate calculations

## Not yet complete / do not treat as production-ready

This is an **initial integrated source scaffold**, not a claim that every acceptance criterion in the long specification has been completed. In particular, multi-biscuit atomic transactions, fully reliable multiple-payment aggregation, edit/detail flows, full audit coverage, date/metal/status filters and sorting, PDF receipts/reports, CSV/XLSX export, print/share, SAF backup/restore, attachment management, persisted settings, secure PIN storage, release signing, database migration tests and end-to-end device verification remain to be implemented and tested. The UI intentionally marks Settings/Backup as not fully wired. Do not use for critical accounting until these are completed and tested.

## Financial calculation notes

- `1 tola = 11.6638 grams`; `1 kg = 1000 grams`.
- Rate basis is explicit (`per gram` or `per tola`); no automatic purity conversion is applied.
- Monetary values are persisted as decimal strings to avoid binary floating-point storage.
- Existing purchase rates are stored on each transaction and are not derived from settings.

## Release signing

Do not commit keystore passwords. Create a private keystore locally and supply credentials through environment variables or an untracked `keystore.properties`. Configure Gradle signing only after the release workflow is reviewed. The current release build type is unsigned by default; debug builds are automatically debug-signed by Android Gradle Plugin.
