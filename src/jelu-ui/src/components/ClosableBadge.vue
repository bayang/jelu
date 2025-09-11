<script setup lang="ts">

const props = defineProps<{ 
  content: string
}>();

const emit = defineEmits<{
  (e: 'closed'): void,
}>()
</script>

<template>
  <div
    class="badge badge-xl m-0.5"
  >
    {{ content }}
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      stroke-width="1.5"
      stroke="currentColor"
      class="size-6 hover:cursor-pointer"
      @click="emit('closed')"
    >
      <path
        stroke-linecap="round"
        stroke-linejoin="round"
        d="m9.75 9.75 4.5 4.5m0-4.5-4.5 4.5M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
      />
    </svg>
  </div>
</template>

<style lang="scss" scoped>

</style>
