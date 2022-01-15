import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  // publicDir: "assets",

  // base : 'http://localhost:11111/',
  plugins: [vue()],
  server : {
    proxy : {
      '/files/': 'http://localhost:11111/'
    }
  },
  build: {
    // emptyOutDir: true,
    // rollupOptions: {
    //   external: [
    //     /files/
    //   ]
    // }
  }
})
