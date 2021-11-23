import { createLogger, createStore } from 'vuex'
import { UserAuthentication, User } from './model/User'
import dataService from './services/DataService'
import router from './router'

// Create a new store instance.
const store = createStore({
  state () {
    return {
      count: 0,
      isLogged: false,
      isInitialSetup : false,
      user: null
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
    }
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
          await router.push({name: 'home'})
        } catch (error) {
          commit('login', false)
        }
      },
      async authenticate({commit, state}, payload) {
        try {
          let user: User = await dataService.authenticateUser(payload.user, payload.password)
          console.log('store authenticate')
          console.log(user)
          commit('login', true)
          commit('user', user)
          await router.push({name: 'home'})
        } catch (error) {
          commit('login', false)
          throw error
        }

        commit('user', await dataService.authenticateUser(payload.user, payload.password))
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
      }

  },
  getters : {
    getUsername(state) {
      return state.user != null ? state.user.email : 'anonymous'
    }
  }, 
  plugins : [createLogger()],
  strict: true
})

export default store
