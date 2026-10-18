#!/usr/bin/env sh
set -eu
: "${JAVA_HOME:?Set JAVA_HOME to your JDK folder.}"

mkdir -p out/main out/test

EXTRA_CP=""
if [ -d "lib" ]; then
  for jar in lib/*.jar; do
    if [ -f "$jar" ]; then
      if [ -z "$EXTRA_CP" ]; then
        EXTRA_CP="$jar"
      else
        EXTRA_CP="$EXTRA_CP:$jar"
      fi
    fi
  done
fi

MAIN_SOURCES=$(find src/main/java -name '*.java' 2>/dev/null || true)
if [ -n "$MAIN_SOURCES" ]; then
  if [ -n "$EXTRA_CP" ]; then
    "$JAVA_HOME/bin/javac" -cp "$EXTRA_CP" -d out/main $MAIN_SOURCES
  else
    "$JAVA_HOME/bin/javac" -d out/main $MAIN_SOURCES
  fi
fi

TEST_SOURCES=$(find src/test/java -name '*.java' 2>/dev/null || true)
if [ -n "$TEST_SOURCES" ]; then
  TEST_CP="out/main"
  if [ -n "$EXTRA_CP" ]; then
    TEST_CP="out/main:$EXTRA_CP"
  fi
  "$JAVA_HOME/bin/javac" -cp "$TEST_CP" -d out/test $TEST_SOURCES
fi
