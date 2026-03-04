/**
 * @filename: lint-staged.config.js
 * @type {import('lint-staged').Configuration}
 */

const eslintCommand = 'eslint --fix --no-warn-ignored --max-warnings=0';
const prettierCommand = 'prettier --write -u';

export default {
  '**/*.{js,mjs,cjs,jsx,ts,tsx}': [eslintCommand, prettierCommand],
  '!**/*.{js,mjs,cjs,jsx,ts,tsx}': prettierCommand,
};
