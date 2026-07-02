#!/bin/sh

# Gradle wrapper launcher.

APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$APP_HOME/.gradle}"
JAR_DIR="$APP_HOME/gradle/wrapper"
MAIN_JAR="$JAR_DIR/gradle-wrapper.jar"
SHARED_JAR="$JAR_DIR/gradle-wrapper-shared.jar"
GRADLE_LIBS="/usr/share/java/gradle/lib/*"

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
else
    JAVA_CMD=java
fi

exec "$JAVA_CMD" -classpath "$MAIN_JAR:$SHARED_JAR:$GRADLE_LIBS" org.gradle.wrapper.GradleWrapperMain "$@"
