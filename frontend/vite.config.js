import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    // Native development: the Java backend runs on :8080 and the refresh
    // cookie stays first-party because everything is served from one origin.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: false,
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.js',
    css: false,
    // userEvent replays keystroke by keystroke, which can outrun the 5s
    // default on a loaded CI runner and fail a test mid-typing.
    testTimeout: 20000,
    hookTimeout: 20000,
  },
})
