import { createRouter, createWebHistory } from 'vue-router'
import store from './store'
import AdminBaseVue from './components/AdminBase.vue'

const isLogged = () => {
    if (!store.getters.getLogged) {
        console.log("is not logged")
        return false
    }
}

const isAdmin = () => {
    if (!store.getters.isAdmin) {
        console.log("is not admin")
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
            component: () => import(/* webpackChunkName: "recommend" */ './components/Welcome.vue'),
            name: 'home'
        },
        {
            path: '/books/:bookId/reviews',
            component: () => import(/* webpackChunkName: "recommend" */ './components/BookReviews.vue'),
            name: 'book-reviews',
            beforeEnter: [isLogged],
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
        {
            path: '/to-read',
            component: () => import(/* webpackChunkName: "recommend" */ './components/ToReadList.vue'),
            name: 'to-read',
            beforeEnter: [isLogged],
        },
        {
            path: '/random',
            component: () => import(/* webpackChunkName: "recommend" */ './components/RandomList.vue'),
            name: 'random',
            beforeEnter: [isLogged],
        },
        {
            path: '/history',
            component: () => import(/* webpackChunkName: "recommend" */ './components/History.vue'),
            name: 'history',
            beforeEnter: [isLogged],
        },
        {
            path: '/tags/:tagId',
            component: () => import(/* webpackChunkName: "recommend" */ './components/TagBooks.vue'),
            name: 'tag-detail',
            beforeEnter: [isLogged],
        },
        {
            path: '/authors/:authorId',
            component: () => import(/* webpackChunkName: "recommend" */ './components/AuthorBooks.vue'),
            name: 'author-detail',
            beforeEnter: [isLogged],
        },
        {
            path: '/reviews/:reviewId',
            component: () => import(/* webpackChunkName: "recommend" */ './components/ReviewDetail.vue'),
            name: 'review-detail',
        },
        {
            path: '/search',
            component: () => import(/* webpackChunkName: "recommend" */ './components/SearchResultsDisplay.vue'),
            name: 'search',
            beforeEnter: [isLogged],
        },
        {
            path: '/series/:seriesId',
            component: () => import(/* webpackChunkName: "recommend" */ './components/SeriesBooks.vue'),
            name: 'series',
            beforeEnter: [isLogged],
        },
        {
            path: '/users/:userId',
            component: () => import(/* webpackChunkName: "recommend" */ './components/UserDetail.vue'),
            name: 'user-detail',
        },
        {
            path: '/profile',
            component: AdminBaseVue,
            name: 'profile-page',
            redirect: '/profile/me',
            beforeEnter: [isLogged],
            children: [
                { path : 'me', component: () => import(/* webpackChunkName: "recommend" */ './components/ProfilePage.vue')},
                { path : 'admin/authors', component: () => import(/* webpackChunkName: "recommend" */ './components/AdminAuthors.vue')},
                { path : 'admin/users', beforeEnter: [isAdmin], component: () => import(/* webpackChunkName: "recommend" */ './components/AdminUsers.vue')},
                { path : 'users', component: () => import(/* webpackChunkName: "recommend" */ './components/UsersList.vue')},
                { path: 'imports', component: () => import(/* webpackChunkName: "recommend" */ './components/Imports.vue')},
                { path: 'settings', component: () => import(/* webpackChunkName: "recommend" */ './components/UserSettings.vue')},
                { path: 'messages', component: () => import(/* webpackChunkName: "recommend" */ './components/UserMessages.vue')},
                { path: 'stats', component: () => import(/* webpackChunkName: "recommend" */ './components/UserStats.vue')},
                { path: 'tags', component: () => import(/* webpackChunkName: "recommend" */ './components/TagsAdmin.vue')},
            ]
        },
    ],
})

router.beforeEach((to, from, next) => {
    console.log(`to : ${to.name?.toString()}`)
    console.log(to)
    console.log(`from : ${from.name?.toString()}`)
    console.log(from)
    console.log(store.getters.getLogged)
    if (from.name == undefined 
        && from.matched.length < 1) {
        console.log('undefined wanting to go to ' + to.name?.toString())
        console.log('undefined wanting to go to ' + to.query['page'])
        console.log(to.query)
        if (to.name !== 'login') {
            // store.commit('entryPoint', to.path)
            store.commit('route', to)
            // if (to.query != null && to.query != undefined) {
            //     store.commit('query', to.query)
            // }
        }
    }
    // if (to.name !== 'login' && !store.getters.getLogged) {
    //     next({ name: 'login' })
    // }
    // else next()
    next()
}
)

export default router
