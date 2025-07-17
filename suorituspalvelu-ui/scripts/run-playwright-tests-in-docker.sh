#!/bin/bash
PLAYWRIGHT_VERSION=$(node -e "console.log(require('./package-lock.json').packages['node_modules/@playwright/test'].version)")
docker run --mount type=bind,source=$PWD,target=/app --user "$(id -u):$(id -g)" -w /app --ipc=host --net=host mcr.microsoft.com/playwright:v$PLAYWRIGHT_VERSION npx playwright test $@