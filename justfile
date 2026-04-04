set dotenv-load := true
set positional-arguments := true
gradlec := "./gradlew"

default:
    @just --list

gradle *args='':
    {{gradlec}} $@

build:
    {{gradlec}} assembleDebug

build-all:
    {{gradlec}} assembleDebug app:assembleAndroidTest app:assembleDebugUnitTest assembleRelease

unit-test:
    {{gradlec}} app:testDebugUnitTest

espresso:
    {{gradlec}} app:createDebugCoverageReport -Pandroid.testInstrumentationRunnerArguments.annotation=*

small-espresso:
    {{gradlec}} clean createDebugCoverageReport -Pandroid.testInstrumentationRunnerArguments.annotation=androidx.test.filters.SmallTest

tasks:
    {{gradlec}} tasks --all

clean:
    {{gradlec}} clean
