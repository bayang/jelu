<script setup lang="ts">
import { useProgrammatic } from "@oruga-ui/oruga-next"
import { useTitle } from '@vueuse/core'
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { key } from '../store'
import Avatar from 'vue-avatar-sdh'

useTitle('Jelu | User page')

const store = useStore(key)
const router = useRouter()
const { oruga } = useProgrammatic();



onMounted(() => {
  console.log("Component is mounted!");
});


</script>

<template>
  <div class="box">
    <article class="media">
      <!-- <figure class="media-left">
        <p class="image is-128x128">
          <img src="../assets/placeholder_asset.png">
        </p>
      </figure> -->
      <Avatar
        :username="store.state.user?.login!"
        class="mr-2"
      />
      <div class="media-content">
        <div class="content">
          <p>
            Username : <strong>{{ store.state.user?.login }}</strong>
            <br>
            Roles :  <span class="tag is-info">USER</span> &nbsp;<span
              v-if="store.state.user?.isAdmin"
              class="tag is-warning"
            >ADMIN</span>
          </p>
        </div>
      </div>
    </article>
  </div>
</template>

<style lang="scss" scoped>

.box {
  text-align: left;
}

</style>
