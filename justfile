set dotenv-load := true
set positional-arguments := true
gradlec := "./gradlew"
app_package := "com.growse.android.io.github.hidroh.materialistic"

default:
    @just --list

[group('general')]
gradle *args='':
    {{gradlec}} $@

[group('general')]
tasks:
    {{gradlec}} tasks --all

[group('general')]
clean:
    {{gradlec}} clean

[group('build')]
build:
    @just format
    {{gradlec}} assembleDebug

[group('build')]
release:
    {{gradlec}} assembleRelease

[group('build')]
build-all:
    {{gradlec}} assembleDebug app:assembleAndroidTest app:assembleDebugUnitTest assembleRelease

[group('build')]
install: build
    adb install -r app/build/outputs/apk/debug/app-debug.apk

# Checks a fastlane changelog exists for the current versionCode (mirrors release.yaml's check)
[group('build')]
check-changelog:
    #!/usr/bin/env bash
    set -euo pipefail
    version_code=$(grep -oP '\bversionCode\s*=\s*\K[0-9]+' app/build.gradle.kts)
    changelog_file="fastlane/metadata/android/en-US/changelogs/${version_code}.txt"
    if [ ! -f "$changelog_file" ]; then
        echo "No changelog found at $changelog_file for versionCode $version_code. Add one before tagging a release." >&2
        exit 1
    fi
    echo "Found $changelog_file"

[group('format')]
format:
    {{gradlec}} ktfmtFormat

[group('format')]
lint:
    {{gradlec}} app:lintDebug

[group('test')]
unit-test:
    {{gradlec}} app:testDebugUnitTest

# Unit tests + JaCoCo report at app/build/reports/coverage/test/debug/
[group('test')]
unit-test-coverage:
    {{gradlec}} app:createDebugUnitTestCoverageReport

# Kaspresso tests on gradle-managed devices, no attached device needed: allDevices (ATD API 33 + minSdk API 23), atdApi33 or aospApi23
[group('test')]
espresso device='allDevices':
    {{gradlec}} app:{{device}}DebugAndroidTest

# Managed device tests + JaCoCo report at app/build/reports/coverage/androidTest/debug/managedDevice/
[group('test')]
espresso-coverage:
    {{gradlec}} app:createManagedDeviceDebugAndroidTestCoverageReport

# Kaspresso tests on a connected device, optionally one class relative to the app package (e.g. data.FavoriteManagerTest)
[group('test')]
espresso-connected test_class='':
    {{gradlec}} app:connectedDebugAndroidTest {{ if test_class == '' { '' } else { '-Pandroid.testInstrumentationRunnerArguments.class=' + app_package + '.' + test_class } }}

# Connected device tests + JaCoCo report at app/build/reports/coverage/androidTest/debug/connected/
[group('test')]
espresso-connected-coverage:
    {{gradlec}} app:createDebugAndroidTestCoverageReport

# Drop the managed AVDs when an image or device definition changes
[group('test')]
clean-managed-devices:
    {{gradlec}} cleanManagedDevices

# Unit + espresso coverage (needs an attached device; see espresso-coverage for device-free)
[group('test')]
coverage: unit-test-coverage espresso-connected-coverage

[group('test')]
wipe-device:
    adb uninstall {{app_package}}
