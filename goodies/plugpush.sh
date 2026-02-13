#!/usr/bin/env bash

# HOW TO USE
# Add something like this to your ~/.ssh/config:

# Host tb
#   HostName = xxx.xxx.xxx.xxx
#   User = xxxxx

# Horray!

DIR=$(readlink -f "$(dirname "$0")")
readonly DIR
cd "$DIR" || exit 1
cd ..

function plug_push() {
  TARGET=$1
  SERVER=$2

  echo "Pushing $TARGET to $SERVER"
  rsync -tz "$TARGET"/build/libs/"$TARGET"-1.0.jar tb:"$SERVER"/plugins/"$TARGET"-1.0.jar
}

function normal_push() {
  TARGET=$1
  SERVER=$2

  echo "Pushing $TARGET to $SERVER"
  rsync -tz "$TARGET"/build/libs/"$TARGET"-1.0.jar tb:"$SERVER"/"$TARGET".jar
}

function backend_plug_push() {
  SERVER=$1
  normal_push "deps" "$SERVER"
  normal_push "core" "$SERVER"
  normal_push "core-pgm" "$SERVER"
  normal_push "bot" "$SERVER"
  normal_push "share" "$SERVER"
  normal_push "tools" "$SERVER"
  normal_push "cps" "$SERVER"
}

if [ "$#" -eq 2 ]; then
    normal_push "$1" "$2"
    exit 0
fi

case "$1" in
  proxy)
    echo "Pushing proxy:"
    plug_push "broxy" "proxy"
    ;;
  backend)
    echo "Pushing backend:"
    backend_plug_push "merge/plugins"
    ;;
  all)
    plug_push "broxy" "proxy"
    backend_plug_push "merge/plugins"
    ;;
  cdn)
    normal_push "deps" "caddy/cdn/deps"
    normal_push "core" "caddy/cdn/deps"
    normal_push "core-pgm" "caddy/cdn/deps"
    normal_push "bot" "caddy/cdn/deps"
    normal_push "share" "caddy/cdn/deps"
    normal_push "tools" "caddy/cdn/deps"
    normal_push "cps" "caddy/cdn/deps"
    normal_push "broxy" "caddy/cdn/deps"
    ssh tb "cd caddy/cdn/deps && ~/bin/gen-manifest.sh https://tombrady.fireballs.me/cdn/deps"
    ;;
  *)
    echo "usage: $0 [proxy|backend|all|cdn]"
    ;;
esac


