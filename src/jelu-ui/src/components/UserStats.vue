<script setup lang="ts">
import { useTitle } from '@vueuse/core';
import { BarElement, CategoryScale, Chart as ChartJS, ChartData, Legend, LinearScale, Title, Tooltip } from 'chart.js';
import dayjs from "dayjs";
import { Ref, ref, watch } from "vue";
import { Bar } from 'vue-chartjs';
import { useI18n } from 'vue-i18n';
import dataService from "../services/DataService";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

useTitle('Jelu | Stats')

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale)

const getYears = () => {
  dataService.yearsWithStats()
  .then(res => {
    years.value = res
    })
  .catch(e => {
    console.log(e)
  })
  
}

const getAllStats = () => {
  loading.value = true
  dataService.yearStats()
  .then(res => {
    let labels = res.map(r => r.year)

    const updatedChartData = {
        labels: labels,
        datasets: [
          {
            label: 'finished',
            backgroundColor: '#bbbbbb',
            data: res.map(r => r.finished)
          },
          {
            label: 'dropped',
            backgroundColor: '#f87979',
            data: res.map(r => r.dropped)
          }
        ]
      }
      chartData.value = { ...updatedChartData }
      loading.value = false
  })
  .catch(e => {
    console.log(e)
    loading.value = false
  })
}

const getYearStats = () => {
  if (currentYear.value != null) {
    loading.value = true
    dataService.monthStatsForYear(currentYear.value)
    .then(res => {
      let labels = res.map(r => r.month).map(m => dayjs(`2020-${m}-1`).format('MMMM'))
  
      const updatedChartData = {
          labels: labels,
          datasets: [
            {
              label: 'finished',
              backgroundColor: '#bbbbbb',
              data: res.map(r => r.finished)
            },
            {
              label: 'dropped',
              backgroundColor: '#f87979',
              data: res.map(r => r.dropped)
            }
          ]
        }
        yearChartData.value = { ...updatedChartData }
        loading.value = false
    })
    .catch(e => {
      loading.value = false
      console.log(e)
    })
  }
}

const loading = ref(false)
const chartData = ref<ChartData<'bar'>>({
      datasets: []
    })
const yearChartData = ref<ChartData<'bar'>>({
      datasets: []
    })

const years: Ref<Array<number>> = ref([])
const currentYear: Ref<number|null> = ref(null)

watch(currentYear, (newVal, oldVal) => {
  console.log("year " + newVal + " " + oldVal)
  getYearStats()
  
})

const loaderFullPage = ref(false)

getAllStats()
getYears()

</script>

<template>
  <div class="grid grid-cols-1 justify-center justify-items-center justify-self-center">
    <h1 class="text-2xl typewriter w-11/12 sm:w-8/12 pb-4 capitalize">
      {{ t('stats.all_time') }}
    </h1>
    <div
      class=""
    >
      <Bar
        :chart-data="chartData"
      />
    </div>
    <h1
      v-if="years != null && years !== undefined && years.length > 0"
      class="text-2xl typewriter w-11/12 sm:w-8/12 py-4 capitalize"
    >
      {{ t('stats.yearly_stats') }}
    </h1>
    <div
      v-if="years != null && years !== undefined && years.length > 0"
      class=""
    >
      <select
        v-model="currentYear"
        class="select select-bordered select-accent pt-2 mb-4"
      >
        <option
          disabled
          selected
        >
          {{ t('stats.choose_year') }}
        </option>
        <option
          v-for="year in years"
          :key="year"
          :value="year"
        >
          {{ year }}
        </option>
      </select>
      <Bar
        :chart-data="yearChartData"
      />
    </div>
  </div>
  <o-loading
    v-model:active="loading"
    :full-page="true"
    :can-cancel="true"
    :overlay="loaderFullPage"
  />
</template>

<style scoped>


</style>
