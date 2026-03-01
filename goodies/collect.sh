#!/usr/bin/env bash

DIR=$(readlink -f "$(dirname "$0")")
readonly DIR
cd "$DIR" || exit 1
cd ..

mkdir backpush &>/dev/null

function add() {
  TARGET=$1
  cp "$TARGET"/build/libs/"$TARGET"-1.0.jar backpush/"$TARGET".jar
}

add "deps"
add "core"
add "core-pgm"
add "bot"
add "share"
add "tools"
add "bingo"
