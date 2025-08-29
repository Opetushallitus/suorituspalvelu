import js from '@eslint/js';
import ts from 'typescript-eslint';
import eslintConfigPrettier from 'eslint-config-prettier';
import eslintReact from '@eslint-react/eslint-plugin';
import playwright from 'eslint-plugin-playwright';

const config = ts.config(
  js.configs.recommended,
  ts.configs.strict,
  ts.configs.stylistic,
  eslintConfigPrettier,
  {
    ignores: [
      'node_modules',
      '.next',
      'out',
      '.react-router',
      'build',
      '.lintstagedrc.js',
      'coverage',
      '**/*/types/backend.ts',
    ],
  },
  {
    languageOptions: {
      parser: ts.parser,
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
    rules: {
      '@typescript-eslint/consistent-type-definitions': 'off',
      '@typescript-eslint/no-inferrable-types': 'off',
      'no-shadow': 'error',
      'no-restricted-imports': [
        'error',
        {
          patterns: ['@mui/*/*/*', '../../*'],
          paths: [
            {
              name: '@mui/material',
              importNames: ['styled'],
              message:
                'Please use styled from @/lib/theme or @mui/material/styles instead.',
            },
          ],
        },
      ],
      '@typescript-eslint/array-type': [
        'error',
        {
          default: 'generic',
        },
      ],
    },
  },
  {
    extends: [eslintReact.configs['recommended-typescript']],
    files: ['src/**/*.{ts,tsx}'],
  },
  {
    extends: [playwright.configs['flat/recommended']],
    files: ['playwright/**/*.ts'],
    rules: {
      '@typescript-eslint/no-floating-promises': 'error',
      'playwright/expect-expect': 'off',
    },
  },
);

export default config;
