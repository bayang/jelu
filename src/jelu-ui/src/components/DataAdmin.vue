<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { ref } from "vue"
import { useI18n } from 'vue-i18n'
import SeriesAdmin from "./SeriesAdmin.vue"
import TagsAdmin from "./TagsAdmin.vue"
import AdminAuthors from './AdminAuthors.vue'
import AuthorsAdmin from './AuthorsAdmin.vue'

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | Data page')

const currentView = ref("TAG")

const changeView = (viewName: string) => {
  currentView.value = viewName
}

</script>

<template>
  <div class="w-fit sm:w-full flex flex-wrap justify-center gap-3 sm:gap-0 mb-3">
    <div
      role="tablist"
      class="tabs tabs-boxed tabs-lg"
    >
      <a
        role="tab"
        class="tab"
        :class="{'tab-active': currentView == 'TAG'}"
        @click="changeView('TAG')"
      >{{ t("book.tag", 2) }}</a>
      <a
        role="tab"
        class="tab"
        :class="{'tab-active': currentView == 'SERIES'}"
        @click="changeView('SERIES')"
      >{{ t("book.series", 2) }}</a>
      <a
        role="tab"
        class="tab"
        :class="{'tab-active': currentView == 'AUTHOR'}"
        @click="changeView('AUTHOR')"
      >{{ t("book.author", 2) }}</a>
      <a
        role="tab"
        class="tab"
        :class="{'tab-active': currentView == 'MERGE'}"
        @click="changeView('MERGE')"
      >{{ t("authors_merge.merge_authors") }}</a>
    </div>
  </div>
  <TagsAdmin v-if="currentView == 'TAG'" />
  <SeriesAdmin v-if="currentView == 'SERIES'" />
  <AuthorsAdmin v-if="currentView == 'AUTHOR'" />
  <AdminAuthors v-if="currentView == 'MERGE'" />
</template>

<style lang="scss" scoped>
</style>
