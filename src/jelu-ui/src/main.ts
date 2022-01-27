import { createApp } from 'vue'
import App from './App.vue'
import VTooltipPlugin from 'v-tooltip'
import VueSnip from 'vue-snip'
import Oruga from '@oruga-ui/oruga-next'
// import '@oruga-ui/oruga-next/dist/oruga.css'

import { bulmaConfig } from '@oruga-ui/theme-bulma'

import '@oruga-ui/theme-bulma/dist/bulma.css'
import '@mdi/font/css/materialdesignicons.min.css'
import 'v-tooltip/dist/v-tooltip.css'
import 'bulma/bulma.sass'
import './assets/style.scss'
import router from './router'
import store, { key } from './store'


createApp(App)
    .use(router)
    .use(store, key)
    .use(VTooltipPlugin)
    .use(VueSnip)
    .use(Oruga, {
        iconPack: 'mdi',
        ...bulmaConfig
    })
    .mount('#app')
