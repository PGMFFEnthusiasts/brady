#!/bin/bash

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
  plug_push "deps" "$SERVER"
  plug_push "core" "$SERVER"
  plug_push "bot" "$SERVER"
  plug_push "share" "$SERVER"
  plug_push "tools" "$SERVER"
}

if [ "$#" -eq 2 ]; then
    plug_push "$1" "$2"
    exit 0
fi

case "$1" in
  proxy)
    echo "Pushing proxy:"
    plug_push "broxy" "proxy"
    ;;
  backend)
    echo "Pushing backend:"
    backend_plug_push "primary"
    backend_plug_push "secondary"
    ;;
  deps)
    echo "Pushing deps:"
    plug_push "deps" "primary"
    plug_push "deps" "secondary"
    ;;
  dev)
    echo "Pushing dev:"
    plug_push "core" "primary"
    plug_push "bot" "primary"
    plug_push "share" "primary"
    plug_push "tools" "primary"
    ;;
  all)
    plug_push "broxy" "proxy"

    backend_plug_push "primary"
    backend_plug_push "secondary"
    ;;
  cdn)
    normal_push "deps" "caddy/cdn/deps"
    normal_push "core" "caddy/cdn/deps"
    normal_push "bot" "caddy/cdn/deps"
    normal_push "share" "caddy/cdn/deps"
    normal_push "tools" "caddy/cdn/deps"
    ;;
  *)
    echo "usage: $0 [proxy|backend|deps|dev|all|cdn]"
    ;;
esac


