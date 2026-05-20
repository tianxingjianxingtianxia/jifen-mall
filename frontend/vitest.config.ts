import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/__tests__/setup.ts'],
    deps: {
      inline: ['element-plus', '@element-plus/icons-vue']
    },
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov', 'html'],
      include: ['src/**/*.vue', 'src/**/*.ts'],
      exclude: ['src/**/*.d.ts', 'src/__tests__/**', 'src/main.ts', 'src/env.d.ts'],
      thresholds: {
        lines: 80,
        branches: 70,
        functions: 75,
        statements: 80
      }
    }
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, './src')
    }
  }
})
