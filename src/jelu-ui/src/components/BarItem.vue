<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const props = defineProps<{
    isOpened: boolean,
    path: string,
  name: string,
    message: string
}>();
</script>

<template>
  <div
    class="jl-menu-item tooltip"
    :data-tip="t(message)"
  >
    <router-link
      class=""
      :to="{ path: path }"
    >
      <slot name="icon" />
      <Transition name="fade">
        <span v-if="isOpened">{{ name }}&nbsp;</span>
      </Transition>
    </router-link>
  </div>
</template>

<style lang="scss"></style>
