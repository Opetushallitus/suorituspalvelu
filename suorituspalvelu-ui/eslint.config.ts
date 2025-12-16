import js from '@eslint/js';
import { defineConfig, globalIgnores } from 'eslint/config';
import ts from 'typescript-eslint';
import eslintConfigPrettier from 'eslint-config-prettier';
import eslintReact from '@eslint-react/eslint-plugin';
import reactHooks from 'eslint-plugin-react-hooks';
import playwright from 'eslint-plugin-playwright';

export default defineConfig(
  js.configs.recommended,
  ts.configs.strict,
  ts.configs.stylistic,
  eslintConfigPrettier,
  globalIgnores([
    'node_modules',
    '.next',
    'out',
    '.react-router',
    'build',
    '.lintstagedrc.js',
    'coverage',
    '**/*/types/backend.ts',
  ]),
  {
    basePath: 'src',
    extends: [
      eslintReact.configs['recommended-typescript'],
      reactHooks.configs.flat.recommended,
    ],
    rules: {
      'react-hooks/set-state-in-effect': 'off', // Handled by @eslint-react/hooks-extra/no-direct-set-state-in-use-effect
    },
  },
  {
    files: ['**/*.ts', '**/*.tsx'],
    languageOptions: {
      // Use TypeScript ESLint parser for TypeScript files
      parser: ts.parser,
      parserOptions: {
        // Enable project service for better TypeScript integration
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
    extends: [playwright.configs['flat/recommended']],
    basePath: './playwright',
    rules: {
      '@typescript-eslint/no-floating-promises': 'error',
      'playwright/expect-expect': 'off',
    },
  },
);
