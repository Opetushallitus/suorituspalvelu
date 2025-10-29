#!/bin/bash

# Install dependencies and maven artifacts if --build option is provided
if [[ " $* " == *--build* ]]; then
    mvn clean install -DskipTests
    cd suorituspalvelu-ui && npm ci
fi

if [[ "$1" == up* ]]; then
    set -- "$@" --abort-on-container-exit
fi

export USERID=$(id -u)
export GROUPID=$(id -g)
docker compose $*
