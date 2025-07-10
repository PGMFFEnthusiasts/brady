#!/bin/bash

DIR=$(readlink -f "$(dirname "$0")")
readonly DIR
cd "$DIR" || exit 1

if [ -f .env ]; then
  echo "✅ .env loaded"
  set -a
  source .env
  set +a
fi

java -Xms1G -Xmx1G -XX:+UseG1GC -XX:G1HeapRegionSize=4M -XX:+UnlockExperimentalVMOptions -XX:+ParallelRefProcEnabled \
 -XX:+AlwaysPreTouch -XX:MaxInlineLevel=15 -jar velocity*.jar
