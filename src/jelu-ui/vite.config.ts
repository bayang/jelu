import path from 'path'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite'

// https://vitejs.dev/config/
export default defineConfig({
  // publicDir: "assets",

  // base : 'http://localhost:11111/',
  plugins: [
    vue(),
    VueI18nPlugin({
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
