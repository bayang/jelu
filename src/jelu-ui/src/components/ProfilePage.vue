<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import ProfileDetail from "./ProfileDetail.vue"
import UserShelves from "./UserShelves.vue"
import CustomLists from './CustomLists.vue'

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | User page')

const currentView = ref("PROFILE")

const changeView = (viewName: string) => {
  currentView.value = viewName
}

</script>

<template>
  <div class="w-fit sm:w-full flex flex-wrap justify-center gap-3 sm:gap-0 mb-3">
    <div
      role="tablist"
      class="tabs tabs-box tabs-lg"
    >
      <a
        role="tab"
        class="tab"
        :class="{'tab-active': currentView == 'PROFILE'}"
        @click="changeView('PROFILE')"
      >{{ t("settings.profile", 2) }}</a>
      <a
        role="tab"
        class="tab"
        :class="{'tab-active': currentView == 'SHELVES'}"
        @click="changeView('SHELVES')"
      >{{ t("settings.shelves", 2) }}</a>
      <a
        role="tab"
        class="tab capitalize"
        :class="{'tab-active': currentView == 'LISTS'}"
        @click="changeView('LISTS')"
      >{{ t("settings.custom_lists", 2) }}</a>
    </div>
  </div>
  <ProfileDetail v-if="currentView == 'PROFILE'" />
  <UserShelves v-if="currentView == 'SHELVES'" />
  <CustomLists v-if="currentView == 'LISTS'" />
</template>

<style lang="scss" scoped>
</style>
