<script setup lang="ts">
import { onMounted, Ref, ref, watch } from "vue";
import { useMagicKeys } from '@vueuse/core'

const keys = useMagicKeys()
const shiftF = keys['Shift+F']

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

const innerOpen = ref(props.open)
console.log('open ' + props.open + " " + innerOpen.value)


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


onMounted(() => {
  console.log("Component is mounted!");
});
</script>

<template>
  <o-sidebar
    :open="props.open"
    :fullheight="true"
    :fullwidth="false"
    :overlay="false"
    :right="false"
    @close="emit('update:open', false)"
  >
    <div class="section p-5">
      <div class="field">
        <label class="label">Sort order : </label>
        <o-radio
          v-model="sortOrder"
          native-value="desc"
        >
          Descending
        </o-radio>
      </div>
      <div class="field">
        <o-radio
          v-model="sortOrder"
          native-value="asc"
        >
          Ascending
        </o-radio>
      </div>
      <slot name="sort-fields" />
      <slot name="filters" />
    </div>
  </o-sidebar>
</template>

<style scoped>

</style>
