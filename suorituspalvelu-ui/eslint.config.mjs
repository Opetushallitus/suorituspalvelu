// @ts-check
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import js from '@eslint/js';
import { FlatCompat } from '@eslint/eslintrc';
import ts from 'typescript-eslint';
import eslintConfigPrettier from 'eslint-config-prettier';
import playwright from 'eslint-plugin-playwright';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const compat = new FlatCompat({
  baseDirectory: __dirname,
  recommendedConfig: js.configs.recommended,
  allConfig: js.configs.all,
});

const config = ts.config(
  {
    ignores: [
      '.next/*',
      'out',
      '.lintstagedrc.js',
      'coverage',
      '**/*/types/backend.ts',
    ],
  },
  ...compat.extends('next/core-web-vitals', 'next/typescript'),
  eslintConfigPrettier,
  {
    languageOptions: {
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
    rules: {
      'no-shadow': ['error'],
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
    ...playwright.configs['flat/recommended'],
    files: ['playwright/**/*.ts'],
    rules: {
      ...playwright.configs['flat/recommended'].rules,
      '@typescript-eslint/no-floating-promises': 'error',
      'playwright/expect-expect': 'off',
    },
  },
);

export default config;
