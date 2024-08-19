<script setup lang="ts">
import { Ref, ref } from "vue";
import { Series, SeriesOrder } from "../model/Series";
import dataService from "../services/DataService";

const props = defineProps<{ 
    series: SeriesOrder,
    parentSeries: Array<SeriesOrder> | undefined
}>()

const localData: Ref<SeriesOrder> = ref(
    {
        "name" : props.series.name, 
        "numberInSeries": props.series.numberInSeries,
        "seriesId" : props.series.seriesId
    })

const previousSelected: Ref<SeriesOrder> = ref(
    {
        "name" : props.series.name, 
        "numberInSeries": props.series.numberInSeries,
        "seriesId" : props.series.seriesId
    })

const emit = defineEmits<{
  (e: 'update-series', series: SeriesOrder): void
}>()

let filteredSeries: Ref<Array<Series>> = ref([]);

function getFilteredSeries(text: string) {
  dataService.findSeriesByCriteria(text).then((data) => filteredSeries.value = data.content)
}

function onSelect(series: Series, event: UIEvent) {
  console.log(series)
  console.log(event)
  // we receive from oruga weird events while nothing is selected
  // so try to get rid of those null data we receive
  if (series != null && event != null && series.id != null) {
    console.log('update')
    localData.value.name = series.name
    localData.value.seriesId = series.id
    previousSelected.value.name = series.name
    previousSelected.value.seriesId = series.id
    emit('update-series', localData.value)
  }
}

const isFetching = ref(false)
const onUpdate = (inp: string) => {
    console.log("inp")
    console.log(inp)
    if (localData.value.name !== previousSelected.value.name) {
        localData.value.seriesId = ""
    }
    console.log('after')
    console.log(localData.value)
}
</script>

<template>
  <div class="flex flex-col sm:flex-row gap-1 grow w-full">
    <o-autocomplete
      v-model="localData.name"
      :input-classes="{rootClass: 'border-2 border-accent'}"
      :data="filteredSeries"
      :clear-on-select="false"
      field="name"
      :loading="isFetching"
      :debounce="150"
      @input="getFilteredSeries"
      @select="onSelect"
      @update:model-value="onUpdate"
    />
    <o-input
      v-model="localData.numberInSeries"
      type="number"
      min="0"
      step="0.1"
      class="input focus:input-accent"
      @update:model-value="emit('update-series', localData)"
    />
  </div>
</template>

<style lang="scss" scoped>
</style>
