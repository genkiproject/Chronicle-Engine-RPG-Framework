#!/bin/sh

APP_HOME=$(cd "${0%/*}" >/dev/null 2>&1; pwd -P)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

exec java -Xmx64m -Xms64m ${JAVA_OPTS:-} ${GRADLE_OPTS:-} -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
