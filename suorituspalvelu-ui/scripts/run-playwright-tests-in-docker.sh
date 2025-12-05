#!/bin/bash
PLAYWRIGHT_VERSION=$(node -e "console.log(require('@playwright/test/package.json').version)")

docker run --mount type=bind,source=$PWD,target=/app --user "$(id -u):$(id -g)" -w /app --ipc=host --net=host mcr.microsoft.com/playwright:v$PLAYWRIGHT_VERSION sh -c "corepack pnpm exec playwright test $@"