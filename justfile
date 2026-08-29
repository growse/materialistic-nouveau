set dotenv-load := true
set positional-arguments := true
gradlec := "./gradlew"

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

[group('test')]
espresso:
    {{gradlec}} app:createDebugCoverageReport -Pandroid.testInstrumentationRunnerArguments.annotation=*

[group('test')]
small-espresso:
    {{gradlec}} clean createDebugCoverageReport -Pandroid.testInstrumentationRunnerArguments.annotation=androidx.test.filters.SmallTest

# Espresso tests on a connected device + JaCoCo report at app/build/reports/coverage/androidTest/debug/connected/
[group('test')]
espresso-coverage:
    {{gradlec}} app:createDebugAndroidTestCoverageReport

# Kaspresso tests on the ATD gradle-managed device (no attached device needed)
[group('test')]
atd:
    {{gradlec}} app:atdApi33DebugAndroidTest

# ATD tests + JaCoCo report at app/build/reports/coverage/androidTest/debug/managedDevice/
[group('test')]
atd-coverage:
    {{gradlec}} app:createManagedDeviceDebugAndroidTestCoverageReport

# Drop the managed AVDs when an image or device definition changes
[group('test')]
clean-managed-devices:
    {{gradlec}} cleanManagedDevices

# Unit + espresso coverage (needs an attached device; see atd-coverage for device-free)
[group('test')]
coverage: unit-test-coverage espresso-coverage

[group('test')]
run:
    @just build
    adb install ./app/build/outputs/apk/debug/app-debug.apk

[group('test')]
wipe-device:
    adb uninstall com.growse.android.io.github.hidroh.materialistic
