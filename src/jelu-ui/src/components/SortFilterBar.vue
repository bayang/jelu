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
    :scroll="scroll"
    :teleport="true"
    @close="emit('update:open', false)"
  >
    <div class="p-5 flex flex-col items-start">
      <div class="field">
        <label class="label font-bold">{{ t('sorting.sort_order') }} : </label>
        <o-radio
          v-model="sortOrder"
          native-value="desc"
        >
          {{ t('sorting.descending') }}
        </o-radio>
      </div>
      <div class="field">
        <o-radio
          v-model="sortOrder"
          native-value="asc"
        >
          {{ t('sorting.ascending') }}
        </o-radio>
      </div>
      <slot name="sort-fields" />
      <slot name="filters" />
    </div>
  </o-sidebar>
</template>

<style scoped>

</style>
