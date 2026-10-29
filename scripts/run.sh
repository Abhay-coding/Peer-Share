#!/usr/bin/env sh
set -eu
. "$(dirname "$0")/compile.sh"

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

RUN_CP="out/main"
if [ -n "$EXTRA_CP" ]; then
  RUN_CP="$RUN_CP$CP_SEP$EXTRA_CP"
fi

"$JAVA_HOME/bin/java" --enable-native-access=ALL-UNNAMED -cp "$RUN_CP" com.peershare.Launcher "$@"
