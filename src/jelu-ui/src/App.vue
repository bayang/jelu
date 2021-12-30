<script setup lang="ts">
import { useStore } from 'vuex'
import { computed, onBeforeMount, onMounted, ref } from 'vue'
import { key } from './store'
import { useLink, useRoute, useRouter } from 'vue-router'
import dataService from "./services/DataService";
const store = useStore(key)
const router = useRouter()
const route = useRoute()

const active = ref(false)

console.log("route " + route.fullPath + " " + route.path + " " + route.redirectedFrom)
console.log(route)
console.log(router.currentRoute.value)

store.dispatch('setupStatus')
store.dispatch('getUser')
  .then(async () => {
    console.log("then")
    // try {
    console.log("entrypoint " + store.state.entryPoint)
    await router.push({ path: store.state.entryPoint })
    console.log("ok nav")
    // } catch(e) {
    // console.log("error nav")
    // console.log(e)
    // }
  })
  .catch(() => {
    console.log("catch in App")
    // router.push({name: 'login'}).then(() => {console.log("ok nav")}).catch(() => {console.log("error nav")})
  })

const isInitialSetup = computed(() => {
  return store.state.isInitialSetup
})
const username = computed(() => {
  return store.getters.getUsername
})
const isLogged = computed(() => {
  return store.state.isLogged
})

onMounted(() => {
  console.log('Component is mounted!')
})
const toggleMenu = () => {
  active.value = !active.value;
}

const logout = () => {
  console.log("logout")
  dataService.logout()
  .then(res => {
    store.dispatch('logout')
    })
}
</script>

<template>
  <section>
    <nav class="navbar" role="navigation" aria-label="main navigation">
      <div class="navbar-brand">
        <router-link class="navbar-item" :to="{ name: 'home' }">
          <img src="./assets/jelu_logo.svg" alt="home" />
        </router-link>

        <a
          @click="toggleMenu"
          role="button"
          :class="active ? 'is-active' : ''"
          class="navbar-burger"
          aria-label="menu"
          aria-expanded="false"
          data-target="navbarBasicExample"
        >
          <span aria-hidden="true"></span>
          <span aria-hidden="true"></span>
          <span aria-hidden="true"></span>
        </a>
      </div>

      <div id="navbarBasicExample" :class="active ? 'is-active' : ''" class="navbar-menu">
        <div class="navbar-start">
          <router-link
            v-if="isLogged"
            class="navbar-item is-family-sans-serif is-uppercase"
            :to="{ name: 'my-books' }"
          >My books</router-link>
          <router-link
            v-if="isLogged"
            class="navbar-item is-family-sans-serif is-uppercase"
            :to="{ name: 'to-read' }"
          >To Read List</router-link>
          <router-link
            v-if="isLogged"
                  :to="{ name: 'add-book' }"
                  class="navbar-item is-family-sans-serif is-uppercase"
                >Import book</router-link>
          <!-- <div v-if="isLogged" class="navbar-item has-dropdown is-hoverable">
            <router-link
              :to="{ name: 'import-book' }"
              class="navbar-link is-family-sans-serif is-uppercase"
            >Import</router-link>
            <div class="navbar-dropdown is-boxed">
              <div class="navbar-item">
                <router-link
                  :to="{ name: 'add-book' }"
                  class="is-family-sans-serif is-uppercase"
                >Manual import</router-link>
              </div>
              <div class="navbar-item">
                <router-link
                  :to="{ name: 'import-book' }"
                  class="is-family-sans-serif is-uppercase"
                >Automated import</router-link>
              </div>
            </div>
          </div> -->
        </div>
        <div class="navbar-end">
          <div class="navbar-item has-dropdown is-hoverable">
            <a class="navbar-link">{{ username }}</a>
            <div class="navbar-dropdown is-boxed">
              <div v-if="isLogged" class="navbar-item">
                <a @click="logout()">logout</a>
              </div>
              <div v-if="!isLogged" class="navbar-item">
                <router-link
            class="is-family-sans-serif is-uppercase"
            :to="{ name: 'login' }"
          >Login</router-link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </nav>
    <div class="bar"></div>

    <router-view></router-view>
  </section>
</template>

<style lang="scss">
@import "./assets/style.scss";

#app {
  // height: 100vh;
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  // color: #2c3e50;
  // margin-top: 60px;
  // background-color:$link;
}
</style>
