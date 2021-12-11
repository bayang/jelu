import { createRouter, createWebHistory } from 'vue-router'
import store from './store'

const isLogged = () => {
    if (!store.state.isLogged) {
        console.log("is not logged")
        return false
    }
}

const router = createRouter({
    history: createWebHistory(),
    linkActiveClass: 'is-active',
    linkExactActiveClass: 'is-active',
    routes: [
        {
            path: '/',
            component: () => import(/* webpackChunkName: "recommend" */ './components/HelloWorld.vue'),
            name: 'home'
        },
        {
            path: '/books/:bookId',
            component: () => import(/* webpackChunkName: "recommend" */ './components/BookDetail.vue'),
            name: 'book-detail',
            props: true,
            beforeEnter: [isLogged],
        },
        {
            path: '/books',
            component: () => import(/* webpackChunkName: "recommend" */ './components/BookList.vue'),
            name: 'my-books',
            beforeEnter: [isLogged],
        },
        {
            path: '/login',
            component: () => import(/* webpackChunkName: "recommend" */ './components/Login.vue'),
            name: 'login'
        },
        {
            path: '/add-book',
            component: () => import(/* webpackChunkName: "recommend" */ './components/AddBook.vue'),
            name: 'add-book',
            beforeEnter: [isLogged],
        },
    ],
})

router.beforeEach((to, from, next) => {
    console.log(`to : ${to.name?.toString()}`)
    console.log(to)
    console.log(`from : ${from.name?.toString()}`)
    console.log(from)
    console.log('router store')
    console.log(store.state.isLogged)
    if (from.name == undefined 
        && from.matched.length < 1) {
        console.log('undefined wanting to go to ' + to.name?.toString())
        if (to.name !== 'login') {
            store.commit('entryPoint', to.path)
        }
    }
    // if (to.name !== 'login' && !store.state.isLogged) {
    //     next({ name: 'login' })
    // }
    // else next()
    next()
}
)

export default router
