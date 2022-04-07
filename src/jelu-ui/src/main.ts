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

createApp(App)
    .use(router)
    .use(store, key)
    .use(VTooltipPlugin)
    .use(Oruga, {
        iconPack: 'mdi',
    })
    .use(SidebarMenu)
    .use(VueSplide)
    .mount('#app')
