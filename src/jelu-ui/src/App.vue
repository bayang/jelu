<script setup lang="ts">
import { useStore } from 'vuex'
import { computed, onBeforeMount, onMounted } from 'vue'
import { key } from './store'
import { useLink, useRoute, useRouter } from 'vue-router'
// const props = defineProps()
const store = useStore(key)
const router = useRouter()
const route = useRoute()
// const { href, isActive, isExactActive, navigate } = useLink(props)

console.log("route " + route.fullPath + " " + route.path + " " + route.redirectedFrom)
console.log(route)
console.log(router.currentRoute.value)

store.dispatch('setupStatus')
store.dispatch('getUser')
  .then(async () => {
    console.log("then")
    try {
      console.log("entrypoint " + store.state.entryPoint)
      await router.push({ path: store.state.entryPoint })
      console.log("ok nav")
    } catch(e) {
      console.log("error nav")
      console.log(e)
    }
  })
  .catch(() => {
    console.log("catch")
    router.push({name: 'login'}).then(() => {console.log("ok nav")}).catch(() => {console.log("error nav")})
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

onBeforeMount(() => {
  console.log("onbeforemount script setup")
})
onMounted(() => {
            console.log('Component is mounted!')
        })
</script>

<template>
<section class="section has-background-light">
  <nav class="level">
  <p class="level-item has-text-centered">
    <router-link :to="{ name: 'home'}">Home</router-link>
  </p>
  <p class="level-item has-text-centered">
    <router-link :to="{ name: 'my-books'}">My books</router-link>
  </p>
  <p class="level-item has-text-centered">
    <router-link :to="{ name: 'login'}">Login</router-link>
  </p>
  <p class="level-item has-text-centered">
    {{username}}
  </p>
</nav>
      
  <router-view></router-view>
  <p>setup : {{isInitialSetup}}, logged : {{isLogged}}</p>
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
  color: #2c3e50;
  // margin-top: 60px;
  // background-color:$link;
}
.toto {
  background-color: $my-color;
}
</style>
