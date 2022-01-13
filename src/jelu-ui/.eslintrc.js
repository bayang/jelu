module.exports = {
    root: true,
    env: {
      node: true,
      'vue/setup-compiler-macros': true
    },
    plugins: [
        '@typescript-eslint',
      ],
    parser: "vue-eslint-parser",
    extends: [
        'eslint:recommended',
        'plugin:@typescript-eslint/recommended',
        'plugin:vue/vue3-recommended',
    ],
    rules: {
      
    },
    parserOptions: {
      parser: '@typescript-eslint/parser',
    },
    overrides: [
      
    ],
  }
  