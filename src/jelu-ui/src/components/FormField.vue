<script setup lang="ts">

const props = defineProps<{
  legend: string,
  placeholder?: string,
}>();
const model = defineModel({required: true})
</script>

<template>
  <fieldset class="fieldset">
    <legend class="fieldset-legend capitalize">
      {{ props.legend }}
    </legend>
    <input
      v-model="model"
      type="text"
      class="input w-full focus:input-accent"
      :placeholder="props.placeholder"
    >
  </fieldset>
</template>

<style lang="scss">

</style>
