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
    {{gradlec}} assembleDebug

[group('build')]
build-all:
    {{gradlec}} assembleDebug app:assembleAndroidTest app:assembleDebugUnitTest assembleRelease

[group('format')]
format:
    {{gradlec}} ktfmtFormat

[group('format')]
lint:
    {{gradlec}} app:lintDebug

[group('test')]
unit-test:
    {{gradlec}} app:testDebugUnitTest

[group('test')]
instrumented-test:
    {{gradlec}} connectedDebugAndroidTest

[group('test')]
espresso:
    {{gradlec}} app:createDebugCoverageReport -Pandroid.testInstrumentationRunnerArguments.annotation=*

[group('test')]
small-espresso:
    {{gradlec}} clean createDebugCoverageReport -Pandroid.testInstrumentationRunnerArguments.annotation=androidx.test.filters.SmallTest
