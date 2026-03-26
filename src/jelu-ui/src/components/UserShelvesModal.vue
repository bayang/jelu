<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import UserShelvesList from './UserShelvesList.vue'

useTitle('Jelu | User page')
const emit = defineEmits<{
  (e: 'close'): void,
  (e: 'shelves-changed'): void
}>()

</script>

<template>
  <div class="event-modal">
    <user-shelves-list
      class="mx-4"
      :allow-delete="false"
      :allow-create="false"
      @close="emit('close')"
    />
  </div>
</template>

<style scoped>
</style>
