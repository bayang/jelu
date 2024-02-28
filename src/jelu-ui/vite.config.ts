import path from 'path'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { VitePWA } from 'vite-plugin-pwa'
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite'

// https://vitejs.dev/config/
export default defineConfig({
  // publicDir: "assets",

  // base : 'http://localhost:11111/',
  plugins: [
    vue(),
    VueI18nPlugin({
      include: path.resolve(__dirname, './src/locales/**')
    }),
    VitePWA({
        includeAssets: ['favicon.ico', 'apple-touch-icon.png'],  
        registerType: 'autoUpdate',
        useCredentials: true,
        strategies: "generateSW",
        injectRegister: "script-defer",
        manifest: {
          name: 'Jelu',
          short_name: 'Jelu',
          description: 'Jelu read tracker app',
          theme_color: '#f7f5d1',
          icons: [
            {
              src: 'android-chrome-192x192.png',
              sizes: '192x192',
              type: 'image/png',
            },
            {
              src: 'android-chrome-512x512.png',
              sizes: '512x512',
              type: 'image/png',
            }
          ]
        }
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
