<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { ref } from 'vue'
import { useStore } from 'vuex'
import { key } from '../store'
import { useI18n } from 'vue-i18n'
import { Provider } from '../model/User'
import BarItem from './BarItem.vue'

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

useTitle('Jelu | User page')

const store = useStore(key)

const items = ref([{ name:t('settings.profile'), tooltip:'settings.my_profile', icon:"bx-user", href:"/profile" },
                { name:t('settings.settings'), icon:"bxs-cog", href:"/profile/settings", tooltip: 'settings.settings' },
                { name:t('nav.data-admin'), icon:"bxs-data", href:"/profile/data", tooltip: 'nav.data-admin' },
                { name:t('settings.imports'), icon:"bxs-file-plus", href:"/profile/imports", tooltip: 'settings.csv_import' },
                { name:t('settings.messages'), icon:"bxs-message-alt-detail", href:"/profile/messages", tooltip:"settings.messages" },
                { name:t('settings.stats'), icon:"bxs-chart", href:"/profile/stats", tooltip: 'settings.stats' },
                { name:t('settings.users'), icon:"bxs-user-detail", href:"/profile/users", tooltip: 'settings.users' },
                { name:t('settings.api_tokens'), icon:"bxs-key", href:"/profile/api-tokens", tooltip: 'settings.api_tokens' }
                ])

if (store.getters.isAdmin && store.getters.getUser != null && store.getters.getUser.provider !== Provider.PROXY) {
  items.value.push({ name:t('settings.add_users'), icon:"bxs-user-plus", href:"/profile/admin/users", tooltip: 'settings.users_management' })
}

const isOpened = ref(false)

</script>

<template>
  <div class="flex flex-row gap-4 w-full">
    <div class="justify-self-start">
      <div class="jl-menubar">
        <div
          v-if="isOpened"
          @click="isOpened=!isOpened"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            class="lucide lucide-list-indent-decrease-icon lucide-list-indent-decrease"
          ><path d="M21 5H11" /><path d="M21 12H11" /><path d="M21 19H11" /><path d="m7 8-4 4 4 4" /></svg>
        </div>
        <div
          v-else
          @click="isOpened=!isOpened"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            class="lucide lucide-menu-icon lucide-menu"
          ><path d="M4 5h16" /><path d="M4 12h16" /><path d="M4 19h16" /></svg>
        </div>
        <bar-item
          v-for="item in items"
          :key="item.href"
          :name="item.name"
          :path="item.href"
          :is-opened="isOpened"
          :message="item.tooltip ?? ''"
        >
          <template #icon>
            <svg
              v-if="item.icon==='bx-user'"
              xmlns="http://www.w3.org/2000/svg"
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="lucide lucide-user-icon lucide-user"
            ><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" /><circle
              cx="12"
              cy="7"
              r="4"
            /></svg>
            <svg
              v-if="item.icon==='bxs-cog'"
              xmlns="http://www.w3.org/2000/svg"
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="lucide lucide-settings-icon lucide-settings"
            ><path d="M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915" /><circle
              cx="12"
              cy="12"
              r="3"
            /></svg>
            <svg
              v-if="item.icon==='bxs-data'"
              xmlns="http://www.w3.org/2000/svg"
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="lucide lucide-database-icon lucide-database"
            ><ellipse
              cx="12"
              cy="5"
              rx="9"
              ry="3"
            /><path d="M3 5V19A9 3 0 0 0 21 19V5" /><path d="M3 12A9 3 0 0 0 21 12" /></svg>
            <svg
              v-if="item.icon==='bxs-file-plus'"
              xmlns="http://www.w3.org/2000/svg"
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="lucide lucide-file-plus-icon lucide-file-plus"
            ><path d="M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z" /><path d="M14 2v5a1 1 0 0 0 1 1h5" /><path d="M9 15h6" /><path d="M12 18v-6" /></svg>
            <svg
              v-if="item.icon==='bxs-message-alt-detail'"
              xmlns="http://www.w3.org/2000/svg"
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="lucide lucide-message-square-text-icon lucide-message-square-text"
            ><path d="M22 17a2 2 0 0 1-2 2H6.828a2 2 0 0 0-1.414.586l-2.202 2.202A.71.71 0 0 1 2 21.286V5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2z" /><path d="M7 11h10" /><path d="M7 15h6" /><path d="M7 7h8" /></svg>
            <svg
              v-if="item.icon==='bxs-chart'"
              xmlns="http://www.w3.org/2000/svg"
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="lucide lucide-chart-line-icon lucide-chart-line"
            ><path d="M3 3v16a2 2 0 0 0 2 2h16" /><path d="m19 9-5 5-4-4-3 3" /></svg>
            <svg
              v-if="item.icon==='bxs-user-detail'"
              xmlns="http://www.w3.org/2000/svg"
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="lucide lucide-users-round-icon lucide-users-round"
            ><path d="M18 21a8 8 0 0 0-16 0" /><circle
              cx="10"
              cy="8"
              r="5"
            /><path d="M22 20c0-3.37-2-6.5-4-8a5 5 0 0 0-.45-8.3" /></svg>
            <svg
              v-if="item.icon==='bxs-key'"
              xmlns="http://www.w3.org/2000/svg"
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="lucide lucide-key-round-icon lucide-key-round"
            ><path d="M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z" /><circle
              cx="16.5"
              cy="7.5"
              r=".5"
              fill="currentColor"
            /></svg>
            <svg
              v-if="item.icon==='bxs-user-plus'"
              xmlns="http://www.w3.org/2000/svg"
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="lucide lucide-user-round-plus-icon lucide-user-round-plus"
            ><path d="M2 21a8 8 0 0 1 13.292-6" /><circle
              cx="10"
              cy="8"
              r="5"
            /><path d="M19 16v6" /><path d="M22 19h-6" /></svg>
          </template>
        </bar-item>
      </div>
    </div>
    <div class="w-full">
      <router-view />
    </div>
  </div>
  <footer class="footer footer-center my-10">
    <div>
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

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.7s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
