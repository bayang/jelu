import { createLogger, createStore, Store } from 'vuex'
import { UserAuthentication, User } from './model/User'
import dataService from './services/DataService'
import router from './router'
import { InjectionKey } from 'vue'

export interface State {
  count: number,
  isLogged: boolean,
  isInitialSetup : boolean,
  user : User | null,
  entryPoint: string
}

export const key: InjectionKey<Store<State>> = Symbol()

// Create a new store instance.
const store = createStore<State>({
  state () {
    return {
      count: 0,
      isLogged: false,
      isInitialSetup : false,
      user: null,
      entryPoint: '/'
    }
  },
  mutations: {
    increment (state) {
      state.count++
    },
    login(state, isLogged) {
        state.isLogged = isLogged
    },
    initialSetup(state, isInitialSetup) {
      state.isInitialSetup = isInitialSetup
    },
    user(state, user: User) {
      state.user = user
    },
    entryPoint(state, entryPoint: string) {
      state.entryPoint = entryPoint
    },
  },
  actions: {
      async setupStatus({commit, state}) {
        commit('initialSetup', await dataService.setupStatus())
      },
      async getUser({commit}) {
        try {
          let auth: UserAuthentication = await dataService.getUser()
          console.log('store auth')
          console.log(auth)
          commit('login', true)
          commit('user', auth.user)
          // await router.push({name: 'home'})
        } catch (error) {
          commit('login', false)
          throw error
        }
      },
      async authenticate({commit, state}, payload) {
        try {
          let user: User = await dataService.authenticateUser(payload.user, payload.password)
          console.log('store authenticate')
          console.log(user)
          commit('login', true)
          commit('user', user)
          await router.push({path: state.entryPoint})
        } catch (error) {
          commit('login', false)
          throw error
        }
      }, 
      async createInitialUser({dispatch, commit, state}, payload) {
        try {
          let user: User = await dataService.createUser(payload.user, payload.password)
          console.log('created')
          console.log(user)
          await dispatch('authenticate', {"user" : payload.user, "password" : payload.password})
          await dispatch('setupStatus')
        } catch (error) {
          throw error
        }
      },
      logout({dispatch, commit, state}) {
        commit('login', false)
        commit('user', null)
        router.push({name: 'login'})
      }

  },
  getters : {
    getUsername(state) {
      return state.user != null ? state.user.login : 'anonymous'
    },
    isAdmin(state) {
      return state.user != null && state.user.isAdmin
    }
  }, 
  plugins : [createLogger()],
  strict: true
})

export default store
