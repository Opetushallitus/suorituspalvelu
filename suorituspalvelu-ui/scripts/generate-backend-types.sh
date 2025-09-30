#!/bin/sh
GIT_ROOT=$(git rev-parse --show-toplevel)
(cd $GIT_ROOT/suorituspalvelu-service && mvn scala:run -DmainClass=fi.oph.suorituspalvelu.ui.TypeScriptGenerator -Dmaven.test.skip=true)
cp $GIT_ROOT/suorituspalvelu-service/target/generated-sources/typescript/Interface.ts $GIT_ROOT/suorituspalvelu-ui/src/types/backend.ts
