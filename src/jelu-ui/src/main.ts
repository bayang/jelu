import { createApp } from 'vue'
import App from './App.vue'
import VTooltipPlugin from 'v-tooltip'
import Oruga from '@oruga-ui/oruga-next'
import SidebarMenu from 'vuejs-sidebar-menu'
import router from './router'
import store, { key } from './store'
import VueSplide from '@splidejs/vue-splide';

import './assets/style.css'

import '@oruga-ui/oruga-next/dist/oruga.css'
import '@oruga-ui/oruga-next/dist/oruga-full-vars.css'

import 'vuejs-sidebar-menu/dist/vuejs-sidebar-menu.css'
import '@mdi/font/css/materialdesignicons.min.css'
import 'v-tooltip/dist/v-tooltip.css'
import '@splidejs/splide/dist/css/splide.min.css';

// import { setupI18n } from './i18n'
/*
 * All i18n resources specified in the plugin `include` option can be loaded
 * at once using the import syntax
 */
import { createI18n } from 'vue-i18n'
import messages from '@intlify/vite-plugin-vue-i18n/messages'
import { usePreferredLanguages } from '@vueuse/core'
import { useLocalStorage } from '@vueuse/core'

// const i18n = setupI18n({
//     legacy: false,
//     locale: 'en',
//     fallbackLocale: 'en',
//     messages: {
//       messages
//     }
//   })
const languages = usePreferredLanguages()
console.log("languages : ")
console.log(languages)
let preferredLanguage = 'en'
if (languages.value && languages.value.length > 0) {
  const candidate = languages.value[0]
  if (candidate.length > 2) {
    preferredLanguage = candidate.slice(0,2)
  } else {
    preferredLanguage = candidate
  }
}
console.log(`favourite language fetched from browser is : ${preferredLanguage}`)
const storedLanguage = useLocalStorage("jelu_language", preferredLanguage)
console.log(`favourite language fetched from storage is : ${storedLanguage.value}`)
  const i18n = createI18n({
    legacy: false,
    locale: storedLanguage.value,
    fallbackLocale: 'en',
    messages
  })

createApp(App)
    .use(i18n)
    .use(router)
    .use(store, key)
    .use(VTooltipPlugin)
    .use(Oruga, {
        iconPack: 'mdi',
    })
    .use(SidebarMenu)
    .use(VueSplide)
    .mount('#app')
