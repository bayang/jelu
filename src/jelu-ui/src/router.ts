import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
    // 4. Provide the history implementation to use. We are using the hash history for simplicity here.
    history: createWebHistory(),
    routes: [
        { path: '/', component: () => import(/* webpackChunkName: "recommend" */ './components/HelloWorld.vue') },
        { path: '/books', component: () => import(/* webpackChunkName: "recommend" */ './components/BookList.vue') },
    ],
})

export default router
