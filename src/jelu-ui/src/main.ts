import { createApp } from 'vue'
import App from './App.vue'
import FloatingVue from 'floating-vue'
import Oruga from '@oruga-ui/oruga-next'
import SidebarMenu from 'vuejs-sidebar-menu'
import router from './router'
import store, { key } from './store'
import VueSplide from '@splidejs/vue-splide';
import { plugin, defaultConfig } from '@formkit/vue'
import { ar, hr, cs, da, nl, fi, fy, he, id, it, ko, fa, pl, pt, ru, es, tr, vi, de, fr, zh } from '@formkit/i18n'
import { generateClasses } from '@formkit/tailwindcss'
import formkitTheme from './formkit-theme'
import VueMarkdownEditor from '@kangc/v-md-editor';
import VMdPreview from '@kangc/v-md-editor/lib/preview';

import './assets/style.css'

import '@oruga-ui/theme-oruga/dist/oruga.css'

import 'vuejs-sidebar-menu/dist/vuejs-sidebar-menu.css'
import '@mdi/font/css/materialdesignicons.min.css'
import 'floating-vue/dist/style.css'
import '@splidejs/splide/dist/css/splide.min.css';

import '@kangc/v-md-editor/lib/style/base-editor.css';
import githubTheme from '@kangc/v-md-editor/lib/theme/github.js';
import '@kangc/v-md-editor/lib/theme/style/github.css';
VueMarkdownEditor.use(githubTheme)
import enUS from '@kangc/v-md-editor/lib/lang/en-US';
VueMarkdownEditor.lang.use('en-US', enUS);
VMdPreview.use(githubTheme);


/*
 * All i18n resources specified in the plugin `include` option can be loaded
 * at once using the import syntax
 */
import { createI18n } from 'vue-i18n'
import messages from '@intlify/unplugin-vue-i18n/messages'
import { datetimeFormats } from './datetimeFormat'
import { usePreferredLanguages } from '@vueuse/core'
import { useLocalStorage } from '@vueuse/core'

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
    messages,
    datetimeFormats: datetimeFormats
  })

createApp(App)
    .use(i18n)
    .use(router)
    .use(store, key)
    .use(FloatingVue)
    .use(Oruga, {
        iconPack: 'mdi',
    })
    .use(SidebarMenu)
    .use(VueSplide)
    .use(plugin, defaultConfig({
      config : {
        classes: generateClasses(formkitTheme)
      },
      // Define additional locales
      locales: { ar, hr, cs, da, nl, fi, fy, he, id, it, ko, fa, pl, pt, ru, es, tr, vi, de, fr, zh },
      // Define the active locale
      locale: storedLanguage.value,
    }))
    .use(VueMarkdownEditor)
    .use(VMdPreview)
    .mount('#app')
