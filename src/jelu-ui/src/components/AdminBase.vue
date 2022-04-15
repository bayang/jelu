<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { onMounted, ref } from 'vue'
import { useStore } from 'vuex'
import { key } from '../store'
import { useI18n } from 'vue-i18n'

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

useTitle('Jelu | User page')

const store = useStore(key)

const items = ref([{ name:t('settings.profile'), tooltip:t('settings.my_profile'), icon:"bx-user", href:"/profile" },
                { name:t('settings.settings'), icon:"bxs-cog", href:"/profile/settings", tooltip: t('settings.profile') },
                { name:t('settings.authors'), icon:"bxs-user-account", href:"/profile/admin/authors", tooltip: t('settings.author_management') },
                { name:t('settings.imports'), icon:"bxs-file-plus", href:"/profile/imports", tooltip: t('settings.csv_import') },
                ])

if (store.getters.isAdmin) {
  items.value.push({ name:t('settings.users'), icon:"bxs-user-plus", href:"/profile/admin/users", tooltip: t('settings.users_management') })
}

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
