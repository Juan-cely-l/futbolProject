import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import reactPlugin from 'eslint-plugin-react'

export default [
  { ignores: ['dist'] },
  js.configs.recommended,
  {
    files: ['**/*.{js,jsx}'],
    plugins: {
      'react-hooks': reactHooks,
      react: reactPlugin,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react/jsx-uses-react': 'error',
      'react/jsx-uses-vars': 'error',
    },
  },
  {
    files: ['**/*.{js,jsx}'],
    plugins: {
      'react-refresh': reactRefresh,
    },
    rules: reactRefresh.configs.vite.rules,
  },
  {
    files: ['**/context/ToastContext.jsx'],
    rules: {
      'react-refresh/only-export-components': ['error', { allowExportNames: ['useToast'] }],
    },
  },
  {
    files: ['**/*.{js,jsx}'],
    languageOptions: {
      globals: globals.browser,
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
  },
]
