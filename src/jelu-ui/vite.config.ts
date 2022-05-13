import path from 'path'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueI18n from '@intlify/vite-plugin-vue-i18n'

// https://vitejs.dev/config/
export default defineConfig({
  // publicDir: "assets",

  // base : 'http://localhost:11111/',
  plugins: [
    vue(),
    vueI18n({
      include: path.resolve(__dirname, './src/locales/**')
    })
  ],
  server : {
    proxy : {
      '/files/': 'http://localhost:11111/',
      '/exports/': 'http://localhost:11111/'
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
