import { createApp } from 'vue'
import App from './App.vue'
import Oruga from '@oruga-ui/oruga-next'
// import '@oruga-ui/oruga-next/dist/oruga.css'

import { bulmaConfig } from '@oruga-ui/theme-bulma'

import '@oruga-ui/theme-bulma/dist/bulma.css'
import '@mdi/font/css/materialdesignicons.min.css'
import 'bulma/bulma.sass'
import './assets/style.scss'
import router from './router'
import store from './store'


createApp(App)
    .use(router)
    .use(store)
    .use(Oruga, bulmaConfig)
    .mount('#app')
