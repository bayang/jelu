<script setup lang="ts">
import { useLocalStorage, useTitle } from '@vueuse/core';
import { BarElement, CategoryScale, ChartData, Chart as ChartJS, Legend, LinearScale, LineController, LineElement, PointElement, Title, Tooltip } from 'chart.js';
import dayjs from "dayjs";
import { Ref, ref, watch } from "vue";
import { Bar } from 'vue-chartjs';
import { useI18n } from 'vue-i18n';
import { TotalsStats } from '../model/YearStats';
import dataService from "../services/DataService";
import { ObjectUtils } from '../utils/ObjectUtils';
import useTypography from '../composables/typography';

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

useTitle('Jelu | Stats')

let currency = localStorage.getItem("JL_CURRENCY")
if (currency == null) {
  currency = "EUR"
}

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale, LineController, PointElement, LineElement)

const getYears = () => {
  dataService.yearsWithStats()
  .then(res => {
    years.value = res
    currentYear.value = res[res.length - 1]
    })
  .catch(e => {
    console.log(e)
  })

}

const storedLanguage = useLocalStorage("jelu_language", "en")

const getAllStats = () => {
  loading.value = true
  dataService.yearStats()
  .then(res => {
    let labels = res.map(r => r.year)

    const updatedChartData = {
        labels: labels,
        datasets: [
        {
            type: 'line',
            label: t('stats.pages_read'),
            backgroundColor: '#3abff8',
            borderColor: '#3abff8',
            borderWidth: 2,
            yAxisID: 'y2',
            data: res.map(r => r.pageCount)
          },
          {
            label: t('stats.finished'),
            yAxisID: 'y1',
            backgroundColor: '#bbbbbb',
            data: res.map(r => r.finished)
          },
          {
            label: t('stats.dropped'),
            yAxisID: 'y1',
            backgroundColor: '#f87979',
            data: res.map(r => r.dropped)
          },
          {
            label: t('book.price'),
            yAxisID: 'y1',
            backgroundColor: '#a5dd2c',
            data: res.map(r => r.price)
          },
        ]
      } as any // mixed charts typing is broken
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
      let labels = res.map(r => r.month).map(m => {
        const d = dayjs(`2020-${m}-1`).format('MMMM')
        return d
      })

      const updatedChartData = {
          labels: labels,
          datasets: [
          {
            type: 'line',
            label: t('stats.pages_read'),
            backgroundColor: '#3abff8',
            borderColor: '#3abff8',
            borderWidth: 2,
            yAxisID: 'y2',
            data: res.map(r => r.pageCount)
          },
            {
              label: t('stats.finished'),
              backgroundColor: '#bbbbbb',
              yAxisID: 'y1',
              data: res.map(r => r.finished)
            },
            {
              label: t('stats.dropped'),
              backgroundColor: '#f87979',
              yAxisID: 'y1',
              data: res.map(r => r.dropped)
            },
            {
              label: t('book.price'),
              yAxisID: 'y1',
              backgroundColor: '#a5dd2c',
              data: res.map(r => r.price)
            },
          ]
        } as any // mixed charts typing is broken
        yearChartData.value = { ...updatedChartData }
        loading.value = false
    })
    .catch(e => {
      loading.value = false
      console.log(e)
    })
  }
}

const totalStats = () => {
  dataService.totalsStats()
  .then( data => totals.value = data)
  .catch(e => console.log(e))
}

const loading = ref(false)
const chartData = ref<ChartData<'bar'>>({
      datasets: []
})
const yearChartData = ref<ChartData<'bar'>>({
      datasets: []
})
const numbers = ["0", "1","2", "3", "4", "5", "6", "7", "8", "9"]
const chartOptions = ref({
      responsive: true,
      maintainAspectRatio: true,
      scales : {
        y1: {
          position: 'left'
        },
        y2: {
          position: 'right'
        }
      },
      plugins: {
        tooltip: {
            callbacks: {
                label: function(context) {
                    let label = context.dataset.label || '';
                    label += ': ';
                    if (context.dataset.label === t('book.price') && context.parsed.y !== null) {
                      label += ObjectUtils.amountInLocale(context.parsed.y, storedLanguage.value, currency)
                    } else {
                      label += context.parsed.y
                    }
                    return label;
                },
                title: function(ctx) {
                  let label = ctx.length > 0 ? ctx[0].label : ""
                  if (label.length > 0 && label[0] in numbers) {
                    return label
                  }
                  label = t(`date.${label}`) ?? label
                  return label
                }
            }
        }
    }
})

const years: Ref<Array<number>> = ref([])
const currentYear: Ref<number|null> = ref(null)
const totals: Ref<TotalsStats> = ref({"read": 0, "unread": 0, "dropped": 0, "total" : 0, "price": 0})

watch(currentYear, (newVal, oldVal) => {
  console.log("year " + newVal + " " + oldVal)
  getYearStats()

})

const loaderFullPage = ref(false)

getAllStats()
getYears()
totalStats()

const { typographyClasses } = useTypography()
</script>

<template>
  <div class="grid grid-cols-1 justify-center justify-items-center justify-self-center w-full">
    <h1
      class="text-2xl w-11/12 sm:w-8/12 pb-4 capitalize"
      :class="typographyClasses"
    >
      {{ t('stats.total') }}:&nbsp;{{ totals.total }}
    </h1>
    <div class="mb-2">
      {{ t('stats.read') }}:&nbsp;{{ totals.read }} / {{ t('stats.unread') }}:&nbsp;{{ totals.unread }} / {{ t('stats.dropped') }}:&nbsp;{{ totals.dropped }} / {{ t('stats.prices_sum') }}&nbsp;{{ totals.price }}&nbsp;{{ currency }}
    </div>
    <h1
      class="text-2xl w-11/12 sm:w-8/12 pb-4 capitalize"
      :class="typographyClasses"
    >
      {{ t('stats.all_time') }}
    </h1>
    <div
      class="w-11/12 sm:w-8/12"
    >
      <Bar
        :data="chartData"
        :options="chartOptions"
      />
    </div>
    <h1
      v-if="years != null && years !== undefined && years.length > 0"
      class="text-2xl w-11/12 sm:w-8/12 py-4 capitalize"
      :class="typographyClasses"
    >
      {{ t('stats.yearly_stats') }}
    </h1>
    <div
      v-if="years != null && years !== undefined && years.length > 0"
      class="w-11/12 sm:w-8/12"
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
        :data="yearChartData"
        :options="chartOptions"
      />
    </div>
  </div>
  <o-loading
    v-model:active="loading"
    :full-page="true"
    :cancelable="true"
    :overlay="loaderFullPage"
  />
</template>

<style scoped>

</style>
