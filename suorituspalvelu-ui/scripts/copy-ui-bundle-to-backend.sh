#!/bin/bash
GIT_ROOT=$(git rev-parse --show-toplevel)

mkdir -p $GIT_ROOT/suorituspalvelu-service/src/main/resources/static/
cp $GIT_ROOT/suorituspalvelu-ui/build/client/*.html $GIT_ROOT/suorituspalvelu-service/src/main/resources/static/
cp -R $GIT_ROOT/suorituspalvelu-ui/build/client/suorituspalvelu/assets $GIT_ROOT/suorituspalvelu-service/src/main/resources/static/