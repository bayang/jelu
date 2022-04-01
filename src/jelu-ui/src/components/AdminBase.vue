<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { onMounted, ref } from 'vue'
import { useStore } from 'vuex'
import { key } from '../store'

useTitle('Jelu | User page')

const store = useStore(key)


onMounted(() => {
  console.log("Component is mounted!");
});

const items = ref([{ name:"Profile", tooltip:"My profile", icon:"bx-user", href:"/profile" },
                { name:"Settings", icon:"bxs-cog", href:"/profile/settings", tooltip: "Settings" },
                { name:"Authors", icon:"bxs-user-account", href:"/profile/admin/authors", tooltip: "Authors management" },
                { name:"Imports", icon:"bxs-file-plus", href:"/profile/imports", tooltip: "Csv import" },
                ])

const isOpened = ref(false)

const sideBarWidth = ref(175)

</script>

<template>
  <div class="flex flex-row gap-4 w-full">
    <div class="justify-self-start">
      <sidebar-menu
        :items="items"
        :is-opened="isOpened"
        :width="sideBarWidth"
      >
        <template #header>
          <div class="hover:hover:bg-accent/50 hover:rounded-lg hover:px-2">
            <i
              class="bx text-3xl"
              :class="isOpened ? 'bx-menu-alt-right' : 'bx-menu'"
              @click="isOpened = !isOpened"
            />
          </div>
        </template>
      </sidebar-menu>
    </div>
    <div class="w-full">
      <router-view />
    </div>
  </div>
  <footer class="footer footer-center my-10">
    <div class="content has-text-centered">
      <p>
        <strong>Jelu</strong> <a
          href="https://github.com/bayang/jelu"
          target="_blank"
          class="link hover:link-accent hover:decoration-4 hover:decoration-accent"
        ><i class="mdi mdi-24px mdi-github" /> version {{ store.getters.getAppVersion }}</a>.
      </p>
    </div>
  </footer>
</template>

<style lang="scss" scoped>

</style>
