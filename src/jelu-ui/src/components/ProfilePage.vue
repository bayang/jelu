<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { onMounted } from 'vue'
import Avatar from 'vue-avatar-sdh'
import { useStore } from 'vuex'
import { key } from '../store'

useTitle('Jelu | User page')

const store = useStore(key)

onMounted(() => {
  console.log("Component is mounted!");
});


</script>

<template>
  <div class="w-fit">
    <h1 class="typewriter text-2xl mb-3">
      Profile :
    </h1>
    <div class="card card-side bg-base-100 shadow-2xl">
      <Avatar
        :username="store.getters.getUsername"
        class="ml-4 mt-8"
      />
      <div class="card-body">
        <p>
          Username : <strong>{{ store.getters.getUsername }}</strong>
          <br>
          Roles :  <span class="badge badge-info tag is-info">USER</span> &nbsp;<span
            v-if="store.getters.isAdmin"
            class="badge badge-warning tag is-warning"
          >ADMIN</span>
        </p>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>

</style>
