#!/usr/bin/env bash

# see plugpush.sh instructions
# only pushes backend plugins but faster

DIR=$(readlink -f "$(dirname "$0")")
readonly DIR
cd "$DIR" || exit 1
cd ..

mkdir backpush

function add() {
  TARGET=$1
  cp "$TARGET"/build/libs/"$TARGET"-1.0.jar backpush/"$TARGET".jar
}

add "deps"
add "core"
add "bot"
add "share"
add "tools"
add "cps"

rsync -ravz backpush/ tb:merge/plugins
