#!/bin/sh
# Gradle start-up script for POSIX-compatible shells (sh, dash, ksh, zsh)

# ─── APP_HOME detection ─────────────────────────────────────────
APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

# Resolve links: $0 may be a link
PRG="$0"
while [ -h "$PRG" ]; do
  ls=$(ls -ld "$PRG")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=$(dirname "$PRG")/"$link"
  fi
done
APP_HOME=$(cd "$(dirname "$PRG")" && pwd)

# ─── JVM options ────────────────────────────────────────────────
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# ─── Classpath ──────────────────────────────────────────────────
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# ─── Java executable ────────────────────────────────────────────
if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
  if [ ! -x "$JAVACMD" ]; then
    echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME" >&2
    exit 1
  fi
else
  JAVACMD="java"
  which java > /dev/null 2>&1 || { echo "ERROR: JAVA_HOME is not set and no 'java' found in PATH." >&2; exit 1; }
fi

# ─── Execute ────────────────────────────────────────────────────
eval "\"$JAVACMD\"" $DEFAULT_JVM_OPTS \
  -classpath "\"$CLASSPATH\"" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
