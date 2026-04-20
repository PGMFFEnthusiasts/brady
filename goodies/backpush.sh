#!/usr/bin/env bash

# see plugpush.sh instructions
# only pushes backend plugins but faster

DIR=$(readlink -f "$(dirname "$0")")
readonly DIR
cd "$DIR" || exit 1
cd ..

./goodies/collect.sh

rsync -ravz backpush/ tb:merge/plugins
