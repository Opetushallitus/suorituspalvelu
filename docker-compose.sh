#!/bin/bash

if [[ "$1" == up* ]]; then
    set -- "$@" --abort-on-container-exit
fi

export USERID=$(id -u)
export GROUPID=$(id -g)
docker compose $*
