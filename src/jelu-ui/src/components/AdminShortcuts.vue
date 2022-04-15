<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import dataService from "../services/DataService"
import { key } from '../store'
 import { setErrors } from '@formkit/vue'
 import { ObjectUtils } from "../utils/ObjectUtils";
 import { useProgrammatic } from "@oruga-ui/oruga-next";

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | Users admin')

const store = useStore(key)
const { oruga } = useProgrammatic()

const form = ref({'login' : '', 'password' : '', 'admin': false})

</script>

<template>
  <div class="grid grid-cols-1 justify-center justify-items-center">
    <h1 class="typewriter text-2xl mb-3 capitalize">
      {{ t('settings.shortcuts') }} :
    </h1>
    <div class="flex flex-row flex-wrap justify-center basis-10/12 sm:basis-1/3">
      <p class="basis-full mt-2">
        <kbd class="kbd">shift</kbd>
        +
        <kbd class="kbd">f</kbd> : {{ t('shortcuts.toggle_bar') }}
      </p>
      <p class="basis-full mt-2">
        <kbd class="kbd">shift</kbd>
        +
        <kbd class="kbd">◀︎</kbd> : {{ t('shortcuts.page_previous') }}
      </p>
      <p class="basis-full mt-2">
        <kbd class="kbd">shift</kbd>
        +
        <kbd class="kbd">▶︎</kbd> : {{ t('shortcuts.page_next') }}
      </p>
    </div>
  </div>
</template>

<style lang="scss" scoped>
</style>
