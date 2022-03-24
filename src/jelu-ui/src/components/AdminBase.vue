<script setup lang="ts">
import { useProgrammatic } from "@oruga-ui/oruga-next"
import { useTitle } from '@vueuse/core'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { key } from '../store'

useTitle('Jelu | User page')

const store = useStore(key)
const router = useRouter()
const {oruga} = useProgrammatic();

onMounted(() => {
  console.log("Component is mounted!");
});

const items = ref([{ name:"Profile", tooltip:"My profile", icon:"bx-user", href:"/profile" },
                { name:"Authors", icon:"bxs-user-account", href:"/profile/admin/authors", tooltip: "Authors management" },
                ])

const jeluBackground= "#262429"

const isOpened = ref(false)

const sideBarWidth = ref(175)

</script>

<template>
  <div class="">
    <div class="columns is-mobile">
      <div class="column is-narrow">
        <sidebar-menu
          :bg-color-primary="jeluBackground"
          :bg-color-secondary="jeluBackground"
          :items="items"
          :is-opened="isOpened"
          :width="sideBarWidth"
          class="shrinked-height"
        >
          <template #header>
            <div>
              <i
                class="bx is-size-4"
                :class="isOpened ? 'bx-menu-alt-right' : 'bx-menu'"
                @click="isOpened = !isOpened"
              />
            </div>
          </template>
        </sidebar-menu>
      </div>
      <div class="column">
        <router-view />
      </div>
    </div>
  </div>
  <footer class="footer">
    <div class="content has-text-centered">
      <p>
        <strong>Jelu</strong> <a
          href="https://github.com/bayang/jelu"
          target="_blank"
        ><i class="mdi mdi-24px mdi-github" /> version {{ store.state.serverSettings.appVersion }}</a>.
      </p>
    </div>
  </footer>
</template>

<style lang="scss" scoped>

</style>
