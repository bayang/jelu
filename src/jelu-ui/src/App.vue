<script setup lang="ts">
import { useStore } from 'vuex'
import { computed } from 'vue'

const store = useStore()
store.dispatch('setupStatus')
store.dispatch('getUser')

const isInitialSetup = computed(() => {
    return store.state.isInitialSetup
  })
const username = computed(() => {
    return store.getters.getUsername
  })
const isLogged = computed(() => {
    return store.state.isLogged
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
