#!/bin/sh
GIT_ROOT=$(git rev-parse --show-toplevel)
(cd $GIT_ROOT/suorituspalvelu-service && mvn exec:java -Dexec.mainClass=fi.oph.suorituspalvelu.ui.TypeScriptGenerator -Dmaven.test.skip=true)
