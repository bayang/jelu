<script setup lang="ts">
import { useStore } from 'vuex'
import { computed, onMounted, ref } from 'vue'
import { key } from './store'
import { useRoute, useRouter } from 'vue-router'
import dataService from "./services/DataService";
import Avatar from 'vue-avatar-sdh'
import { themeChange } from 'theme-change'

const store = useStore(key)
const router = useRouter()
const route = useRoute()

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
    router.push({ name: 'login' }).then(() => { console.log("ok nav") }).catch(() => { console.log("error nav") })
  })

const username = computed(() => {
  return store.getters.getUsername
})
const isLogged = computed(() => {
  return store.getters.getLogged
})

onMounted(() => {
  console.log('Component is mounted!')
  themeChange(false);
})

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
    <div class="navbar bg-base-100">
      <div class="navbar-start">
        <div class="dropdown">
          <label
            tabindex="0"
            class="btn btn-ghost lg:hidden"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              class="h-5 w-5"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M4 6h16M4 12h8m-8 6h16"
              />
            </svg>
          </label>
          <ul
            tabindex="0"
            class="menu menu-compact dropdown-content mt-3 p-2 shadow bg-base-100 rounded-box w-52"
          >
            <li>
              <router-link
                v-if="isLogged"
                class="font-sans"
                :to="{ name: 'my-books' }"
              >
                My books
              </router-link>
            </li>
            <li>
              <router-link
                v-if="isLogged"
                class="font-sans"
                :to="{ name: 'to-read' }"
              >
                To Read List
              </router-link>
            </li>
            <li>
              <router-link
                v-if="isLogged"
                :to="{ name: 'add-book' }"
                class="font-sans"
              >
                Add book
              </router-link>
            </li>
          </ul>
        </div>
        <router-link
          class
          :to="{ name: 'home' }"
        >
          <img
            src="./assets/jelu_logo.svg"
            alt="home"
            class="w-14"
          >
        </router-link>
      </div>
      <div class="navbar-center hidden lg:flex">
        <ul class="menu menu-horizontal p-0">
          <li>
            <router-link
              v-if="isLogged"
              class="font-sans text-xl"
              :to="{ name: 'my-books' }"
            >
              My books
            </router-link>
          </li>
          <li>
            <router-link
              v-if="isLogged"
              class="font-sans text-xl"
              :to="{ name: 'to-read' }"
            >
              To Read List
            </router-link>
          </li>
          <li>
            <router-link
              v-if="isLogged"
              :to="{ name: 'add-book' }"
              class="font-sans text-xl"
            >
              Add book
            </router-link>
          </li>
        </ul>
      </div>
      <div class="navbar-end">
        <div class="dropdown dropdown-end">
          <label
            tabindex="0"
            class="btn btn-ghost btn-circle avatar"
          >
            <Avatar
              :size="40"
              :username="username"
              class="w-10"
            />
          </label>
          <ul
            tabindex="0"
            class="mt-3 p-2 shadow menu menu-compact dropdown-content bg-base-100 rounded-box w-52"
          >
            <li v-if="isLogged">
              <router-link
                class="font-sans text-base"
                :to="{ name: 'profile-page' }"
              >
                Dashboard
              </router-link>
            </li>
            <li v-if="!isLogged">
              <router-link
                class="font-sans text-base"
                :to="{ name: 'login' }"
              >
                Login
              </router-link>
            </li>
            <li v-if="isLogged">
              <a
                class="font-sans text-base"
                @click="logout()"
              >Logout</a>
            </li>
          </ul>
        </div>
      </div>
    </div>
    <div class="divider" /> 

    <router-view />
  </section>
</template>

<style lang="css">

#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
}
</style>
