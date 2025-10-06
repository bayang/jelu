<script setup lang="ts">
import { computed, Ref, ref } from "vue";
import { useI18n } from 'vue-i18n';
import { Series, SeriesOrder } from "../model/Series";
import dataService from "../services/DataService";
import { ObjectUtils } from "../utils/ObjectUtils";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })
const model = defineModel<Array<SeriesOrder>>()

console.log("init")
console.log(model.value)

let currentInput = ref("")

let filteredSeries: Ref<Array<Series>> = ref([]);

function getFilteredSeries(text: string) {
  currentInput.value = text
  dataService.findSeriesByCriteria(text).then((data) => filteredSeries.value = data.content)
}

function onSelect(series: Series) {
  // we receive from oruga weird events while nothing is selected
  // so try to get rid of those null data we receive
  if (series != null) {
    model.value?.push({"name": series.name, "seriesId": series.id})
    filteredSeries.value = []
  }
}

const addNew = () => {
  if (currentInput.value != null && currentInput.value.length > 0) {
    model.value?.push({"name": currentInput.value})
    filteredSeries.value = []
  }
}

const isFetching = ref(false)
const options = computed(() => {
  return filteredSeries.value.map(t => ObjectUtils.wrapForOptions(t))
})

</script>

<template>
  <div class="flex flex-col sm:flex-col gap-1 grow w-full">
    <o-autocomplete
      :input-classes="{rootClass: 'border-2 border-accent'}"
      :loading="isFetching"
      open-on-focus
      clear-on-select
      backend-filtering
      :debounce="150"
      :options="options"
      expanded
      @input="getFilteredSeries"
      @select="onSelect"
    >
      <template #default="{ value }">
        <div class="jl-taginput-item">
          {{ value.name }}
        </div>
      </template>
      <template #empty="">
        <div class="jl-taginput-empty">
          <button
            class="cursor-pointer! pointer-events-auto"
            @click="addNew"
          >
            {{ t('series.create_series') }}
          </button>
        </div>
      </template>
    </o-autocomplete>
    <div class="">
      <ul class="list bg-base-100 rounded-box shadow-md flex flex-col sm:flex-row sm:flex-wrap gap-2">
        <li
          v-for="ser,idx in model"
          :key="ser.name"
          class="list-row items-center bg-base-200"
        >
          <div>{{ ser.name }}</div>
          <o-input
            v-model="ser.numberInSeries"
            type="number"
            min="0"
            step="0.1"
            class="input focus:input-accent w-30"
          />
          <button
            class="btn btn-square btn-ghost"
            @click="model?.splice(idx, 1)"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              class="h-5 w-5 text-error"
              viewBox="0 0 20 20"
              fill="currentColor"
            >
              <path
                fill-rule="evenodd"
                d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z"
                clip-rule="evenodd"
              />
            </svg>
          </button>
        </li>
      </ul>
    </div>
  </div>
</template>

<style lang="scss" scoped>
</style>
