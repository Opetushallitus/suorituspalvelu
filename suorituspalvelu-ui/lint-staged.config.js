/**
 * @filename: lint-staged.config.js
 * @type {import('lint-staged').Configuration}
 */

const eslintCommand = 'eslint --fix --no-warn-ignored --max-warnings=0';
const prettierCommand = 'prettier --write -u';

export default {
  '**/*.{js,mjs,cjs,jsx,ts,tsx}': (files) => {
    const filtered = files.filter((f) => !f.endsWith('/types/backend.ts'));
    if (filtered.length === 0) return [];
    return [`${eslintCommand} ${filtered.join(' ')}`, `${prettierCommand} ${filtered.join(' ')}`];
  },
  '!**/*.{js,mjs,cjs,jsx,ts,tsx}': prettierCommand,
};
