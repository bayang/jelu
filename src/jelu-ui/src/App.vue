<script setup lang="ts">
import { useStore } from 'vuex'
import { computed, onMounted, ref } from 'vue'
import { key } from './store'
import { useRoute, useRouter } from 'vue-router'
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
    // await router.push({ path: store.state.entryPoint })
    if (store.state.route != null) {
      await router.push(store.state.route)
    }
    console.log("ok nav")
    store.dispatch('getServerSettings')
    // } catch(e) {
    // console.log("error nav")
    // console.log(e)
    // }
  })
  .catch(() => {
    console.log("catch in App")
    router.push({name: 'login'}).then(() => {console.log("ok nav")}).catch(() => {console.log("error nav")})
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
    <nav
      class="navbar"
      role="navigation"
      aria-label="main navigation"
    >
      <div class="navbar-brand">
        <router-link
          class="navbar-item"
          :to="{ name: 'home' }"
        >
          <img
            src="./assets/jelu_logo.svg"
            alt="home"
          >
        </router-link>

        <a
          role="button"
          :class="active ? 'is-active' : ''"
          class="navbar-burger"
          aria-label="menu"
          aria-expanded="false"
          data-target="navbarBasicExample"
          @click="toggleMenu"
        >
          <span aria-hidden="true" />
          <span aria-hidden="true" />
          <span aria-hidden="true" />
        </a>
      </div>

      <div
        id="navbarBasicExample"
        :class="active ? 'is-active' : ''"
        class="navbar-menu"
      >
        <div class="navbar-start">
          <router-link
            v-if="isLogged"
            class="navbar-item is-family-sans-serif is-uppercase"
            :to="{ name: 'my-books' }"
          >
            My books
          </router-link>
          <router-link
            v-if="isLogged"
            class="navbar-item is-family-sans-serif is-uppercase"
            :to="{ name: 'to-read' }"
          >
            To Read List
          </router-link>
          <router-link
            v-if="isLogged"
            :to="{ name: 'add-book' }"
            class="navbar-item is-family-sans-serif is-uppercase"
          >
            Add book
          </router-link>
        </div>
        <div class="navbar-end">
          <div class="navbar-item has-dropdown is-hoverable">
            <a class="navbar-link">{{ username }}</a>
            <div class="navbar-dropdown is-boxed">
              <div
                v-if="isLogged"
                class="navbar-item"
              >
                <a @click="logout()">logout</a>
              </div>
              <div
                v-if="!isLogged"
                class="navbar-item"
              >
                <router-link
                  class="is-family-sans-serif is-uppercase"
                  :to="{ name: 'login' }"
                >
                  Login
                </router-link>
              </div>
              <div
                v-if="isLogged"
                class="navbar-item"
              >
                <router-link
                  class="is-family-sans-serif"
                  :to="{ name: 'import' }"
                >
                  Import books
                </router-link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </nav>
    <div class="bar" />

    <router-view />
  </section>
</template>

<style lang="scss">
@import "./assets/style.scss";

#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
}
</style>
