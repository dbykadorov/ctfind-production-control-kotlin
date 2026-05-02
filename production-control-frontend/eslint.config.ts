import antfu from '@antfu/eslint-config'
import vuejsAccessibility from 'eslint-plugin-vuejs-accessibility'

export default antfu(
  {
    vue: true,
    typescript: true,
    formatters: {
      css: true,
      html: true,
    },
    stylistic: {
      indent: 2,
      quotes: 'single',
      semi: false,
    },
    ignores: [
      'dist',
      'build',
      'coverage',
      'node_modules',
      '*.min.js',
      'src/api/types/legacy.generated.ts',
      'src/components/ui/**',
    ],
  },
  {
    files: ['**/*.vue'],
    plugins: {
      'vuejs-accessibility': vuejsAccessibility,
    },
    rules: {
      'vuejs-accessibility/anchor-has-content': 'warn',
      'vuejs-accessibility/click-events-have-key-events': 'warn',
      'vuejs-accessibility/form-control-has-label': 'warn',
      'vuejs-accessibility/heading-has-content': 'warn',
      'vuejs-accessibility/label-has-for': 'warn',
      'vuejs-accessibility/no-autofocus': 'warn',
    },
  },
)
