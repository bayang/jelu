// @ts-check

import eslint from '@eslint/js';
// import /* jestPlugin */ from 'eslint-plugin-jest';
import vuePlugin from 'eslint-plugin-vue'
import tseslint from 'typescript-eslint';

import globals from 'globals'

export default tseslint.config(
  {
    // config with just ignores is the replacement for `.eslintignore`
    ignores: ['**/build/**', '**/dist/**', '*.d.ts'],
  },
{
    extends: [
      eslint.configs.recommended,
      ...tseslint.configs.recommended,
      ...vuePlugin.configs['flat/recommended'],
    ],
    files: ['**/*.{ts,vue}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: globals.browser,
      parserOptions: {
        parser: tseslint.parser,
      },
    },
    rules: {
      // your rules
    "no-unused-vars": "off",
    "@typescript-eslint/no-unused-vars": "warn",
    "prefer-const": ["warn", {"destructuring": "all"}]
    },
  },
);
