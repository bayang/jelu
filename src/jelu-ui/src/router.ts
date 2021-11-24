import { createRouter, createWebHistory } from 'vue-router'
import store from './store'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            component: () => import(/* webpackChunkName: "recommend" */ './components/HelloWorld.vue'),
            name: 'home'
        },
        {
            path: '/books',
            component: () => import(/* webpackChunkName: "recommend" */ './components/BookList.vue'),
            name: 'my-books'
        },
        {
            path: '/login',
            component: () => import(/* webpackChunkName: "recommend" */ './components/Login.vue'),
            name: 'login'
        },
        {
            path: '/add-book',
            component: () => import(/* webpackChunkName: "recommend" */ './components/AddBook.vue'),
            name: 'add-book'
        },
    ],
})

router.beforeEach(async (to, from, next) => {
    // console.log(`to : ${to.name?.toString()}`)
    // console.log('router store')
    // console.log(store.state.isLogged)
    if (to.name !== 'login' && !store.state.isLogged) {
        next({ name: 'login' })
    }
    else next()
}
)

export default router
