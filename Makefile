# Simple build entry point for AuLoup.
#
#   make            build the debug APK -> app/build/outputs/apk/debug/auloup.apk
#   make install    build and install on a connected device/emulator
#   make clean       remove build outputs
#
# Gradle needs an LTS JDK (17 or 21); the common system default JDK 25 is
# rejected. This Makefile finds a compatible JDK automatically. If it can't,
# point it at one yourself:  make JAVA_HOME=/path/to/jdk-21
#
# On Windows (no `make`), run the wrapper directly; it produces the same file:
#   gradlew.bat assembleDebug

APK := app/build/outputs/apk/debug/auloup.apk

# Find a usable JDK (17 or 21): the Android Studio bundle, a system install, an
# SDKMAN candidate, or one unpacked under ~/tools. First match with a working
# `javac` wins. Override by setting JAVA_HOME in the environment or on the
# command line.
JAVA_HOME ?= $(firstword $(wildcard \
  /snap/android-studio/*/jbr \
  $(HOME)/tools/jdk-21* $(HOME)/tools/jdk-17* \
  /usr/lib/jvm/java-21-openjdk* /usr/lib/jvm/java-17-openjdk* \
  /usr/lib/jvm/jdk-21* /usr/lib/jvm/jdk-17* \
  $(HOME)/.sdkman/candidates/java/21* $(HOME)/.sdkman/candidates/java/17*))

export JAVA_HOME

.PHONY: build install clean check-jdk

build: check-jdk
	./gradlew assembleDebug
	@echo
	@echo "Built: $(APK)"

install: check-jdk
	./gradlew installDebug

clean:
	./gradlew clean

check-jdk:
ifeq ($(strip $(JAVA_HOME)),)
	@echo "No LTS JDK (17 or 21) found automatically."
	@echo "Install one (Android Studio bundles it) or run:"
	@echo "  make JAVA_HOME=/path/to/jdk-21"
	@exit 1
else
	@echo "Using JAVA_HOME=$(JAVA_HOME)"
endif
