import { InjectionKey } from 'vue';
import { RouteLocationNormalized } from 'vue-router';
import { createLogger, createStore, Store } from 'vuex';
import { ServerSettings } from './model/ServerSettings';
import { Shelf } from './model/Shelf';
import { User, UserAuthentication } from './model/User';
import router from './router';
import dataService from './services/DataService';

export interface State {
  isLogged: boolean,
  isInitialSetup : boolean,
  user : User | null,
  serverSettings: ServerSettings,
  route: RouteLocationNormalized | null,
  shelves: Array<Shelf>
}

export const key: InjectionKey<Store<State>> = Symbol()

// Create a new store instance.
const store = createStore<State>({
  state () {
    return {
      isLogged: false,
      isInitialSetup : false,
      user: null,
      state: null,
      route: null,
      serverSettings: {
        metadataFetchEnabled: false,
        metadataFetchCalibreEnabled: false,
        ldapEnabled: false,
        appVersion: "",
        metadataPlugins: []
      } as ServerSettings,
      shelves: []
    }
  },
  mutations: {
    login(state, isLogged) {
        state.isLogged = isLogged
    },
    initialSetup(state, isInitialSetup) {
      state.isInitialSetup = isInitialSetup
    },
    user(state, user: User) {
      state.user = user
    },
    route(state, route: RouteLocationNormalized) {
      state.route = route
    },
    serverSettings(state, serverSettings: ServerSettings) {
      state.serverSettings = serverSettings
    },
    shelves(state, shelves: Array<Shelf>) {
      state.shelves = shelves
    },
  },
  actions: {
      async setupStatus({dispatch, commit, state}) {
        commit('initialSetup', await dataService.setupStatus())
        if (state.isInitialSetup) {
          dispatch('getServerSettings')
        }
      },
      async getUser({commit}) {
        try {
          const auth: UserAuthentication = await dataService.getUser()
          console.log('store auth')
          console.log(auth)
          commit('login', true)
          commit('user', auth.user)
        } catch (error) {
          commit('login', false)
          throw error
        }
      },
      async authenticate({dispatch, commit, state}, payload) {
        try {
          const user: User = await dataService.authenticateUser(payload.user, payload.password)
          console.log('store authenticate')
          console.log(user)
          commit('login', true)
          commit('user', user)
          dispatch('getServerSettings')
          dispatch('getUserShelves')
          if (state.route != null) {
            await router.push(state.route)
          }
          else {
            console.log("route is null")
          }
        } catch (error) {
          commit('login', false)
          throw error
        }
      }, 
      async createInitialUser({dispatch, commit, state}, payload) {
        const user: User = await dataService.createInitialUser(payload.user, payload.password)
        console.log('created')
        console.log(user)
        await dispatch('authenticate', {"user" : payload.user, "password" : payload.password})
        await dispatch('setupStatus')
      },
      logout({dispatch, commit, state}) {
        commit('login', false)
        commit('user', null)
        router.push({name: 'login'})
      },
      async getServerSettings({commit}) {
        dataService.serverSettings()
          .then(res => {
            console.log(res)
            commit('serverSettings', res)
          })
          .catch(err => {
            return err
          })
      },
      async getUserShelves({commit}) {
        dataService.shelves()
          .then(res => {
            console.log(res)
            commit('shelves', res)
          })
          .catch(err => {
            return err
          })
      },

  },
  getters : {
    getUsername(state): string {
      return state.user != null ? state.user.login : 'anonymous'
    },
    getUser(state): User|null {
      return state.user
    },
    isAdmin(state): boolean {
      return state.user != null && state.user.isAdmin
    },
    getSettings(state): ServerSettings {
      return state.serverSettings
    },
    getMetadataFetchEnabled(state): boolean {
      return state.serverSettings.metadataFetchEnabled
    },
    getLdapEnabled(state): boolean {
      return state.serverSettings.ldapEnabled
    },
    getLogged(state): boolean {
      return state.isLogged
    },
    getAppVersion(state): string {
      return state.serverSettings.appVersion
    },
    getInitialSetup(state): boolean {
      return state.isInitialSetup
    },
    getDisplayInitialSetup(state, getters): boolean {
      return (getters.getInitialSetup && !getters.getLdapEnabled)
    },
    getShelves(state): Array<Shelf> {
      return state.shelves
    }
  }, 
  plugins : [createLogger()],
  strict: true
})

export default store
