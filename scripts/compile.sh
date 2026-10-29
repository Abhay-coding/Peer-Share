#!/usr/bin/env sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
if [ -z "${JAVA_HOME:-}" ]; then
  if [ -x /usr/libexec/java_home ]; then
    JAVA_HOME=$(/usr/libexec/java_home)
  else
    JAVAC_PATH=$(command -v javac || true)
    if [ -z "$JAVAC_PATH" ]; then
      echo "Install JDK 17+ and set JAVA_HOME." >&2
      exit 1
    fi
    JAVA_HOME=$(dirname "$(dirname "$(readlink -f "$JAVAC_PATH")")")
  fi
fi
export JAVA_HOME

CP_SEP=:
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) CP_SEP=';' ;;
esac

mkdir -p out/main out/test

EXTRA_CP=""
if [ -d "lib" ]; then
  for jar in lib/*.jar; do
    if [ -f "$jar" ]; then
      if [ -z "$EXTRA_CP" ]; then
        EXTRA_CP="$jar"
      else
        EXTRA_CP="$EXTRA_CP$CP_SEP$jar"
      fi
    fi
  done
fi

MAIN_SOURCES=$(find src/main/java -name '*.java' 2>/dev/null || true)
if [ -n "$MAIN_SOURCES" ]; then
  if [ -n "$EXTRA_CP" ]; then
    "$JAVA_HOME/bin/javac" --release 17 -cp "$EXTRA_CP" -d out/main $MAIN_SOURCES
  else
    "$JAVA_HOME/bin/javac" --release 17 -d out/main $MAIN_SOURCES
  fi
fi

TEST_SOURCES=$(find src/test/java -name '*.java' 2>/dev/null || true)
if [ -n "$TEST_SOURCES" ]; then
  TEST_CP="out/main"
  if [ -n "$EXTRA_CP" ]; then
    TEST_CP="out/main$CP_SEP$EXTRA_CP"
  fi
  "$JAVA_HOME/bin/javac" --release 17 -cp "$TEST_CP" -d out/test $TEST_SOURCES
fi
