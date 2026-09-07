<script setup lang="ts">

const props = defineProps<{
    isLogged: boolean,
    name: string,
    destination: string
}>();
</script>

<template>
  <button
    class="is-drawer-close:tooltip is-drawer-close:tooltip-right"
    :data-tip="props.name"
  >
    <router-link
      v-if="isLogged"
      class="font-sans text-base capitalize flex items-center gap-1"
      :to="{ name: props.destination }"
    >
      <slot name="icon" />
      <span class="is-drawer-close:hidden">{{ props.name }}</span>
    </router-link>
  </button>
</template>

<style lang="scss"></style>
