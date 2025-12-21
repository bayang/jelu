<script setup lang="ts">
import { useMagicKeys } from '@vueuse/core';
import { ref, watch } from "vue";
import { useI18n } from 'vue-i18n';

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const keys = useMagicKeys()
const shiftF = keys['Shift+F']

const scroll = ref('clip')

const props = defineProps<{
  order: string
  open: boolean
}>()

const emit = defineEmits<{
  (e: 'update:sortOrder', newval: string): void
  (e: 'update:open', open: boolean): void
}>()

const sortOrder = ref(props.order)

console.log('order ' + sortOrder.value)

watch(sortOrder, (newVal, oldVal) => {
  console.log("bar sort order " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
      emit("update:sortOrder", newVal)
  }
})

watch(shiftF, (v) => {
  if (v) {
    emit('update:open', !props.open)
  }
})

</script>

<template>
  <o-sidebar
    :active="props.open"
    :fullheight="true"
    :fullwidth="false"
    :overlay="false"
    class="jl-sidebar"
    :scroll="scroll"
    :teleport="true"
    @close="emit('update:open', false)"
  >
    <div class="p-5 flex flex-col items-start">
      <label class="label font-bold">{{ t('sorting.sort_order') }} : </label>
      <div class="field">
        <input
          v-model="sortOrder"
          type="radio"
          name="radio-10"
          class="radio radio-primary my-2"
          value="desc"
        >
        <span class="label-text">{{ t('sorting.descending') }}</span>
      </div>
      <div class="field mt-1">
        <input
          v-model="sortOrder"
          type="radio"
          name="radio-10"
          class="radio radio-primary mb-2"
          value="asc"
        >
        <span class="label-text">{{ t('sorting.ascending') }}</span>
      </div>
      <slot name="sort-fields" />
      <slot name="filters" />
    </div>
  </o-sidebar>
</template>

<style scoped>

</style>
