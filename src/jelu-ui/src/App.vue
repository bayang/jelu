<script setup lang="ts">
// This starter template is using Vue 3 <script setup> SFCs
// Check out https://v3.vuejs.org/api/sfc-script-setup.html#sfc-script-setup
import HelloWorld from './components/HelloWorld.vue'
import BookList from './components/BookList.vue'
</script>

<template>
<router-link to="/">Go to Home</router-link>
    <router-link to="/books">Go to books</router-link>
<section class="section has-background-light">
    <div class="container toto">
      
  <!-- <HelloWorld msg="Hello Vue 3 + Test + Vite" />
  <BookList/> -->
  <router-view></router-view>
    </div>
  </section>
</template>

<style lang="scss">
// @import '@oruga-ui/theme-bulma/dist/bulma.css';
// @import 'bulma/bulma.sass';
@import "./assets/style.scss";

#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 60px;
  background-color: $my-color;
}
.toto {
  background-color: $my-color;
}
</style>
