<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import dayjs from "dayjs";
import { computed, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import usePagination from '../composables/pagination';
import { ReadingEventType, ReadingEventWithUserBook } from '../model/ReadingEvent';
import dataService from "../services/DataService";
import BookCard from "./BookCard.vue";
import localizedFormat from 'dayjs/plugin/localizedFormat'

const { t, d } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

useTitle('Jelu | History')

dayjs.extend(localizedFormat)

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading } = usePagination()

const nonCurrentlyReadingEvents: Array<ReadingEventType> = [ReadingEventType.DROPPED, ReadingEventType.FINISHED]

const yearEvents: Ref<Array<ReadingEventWithUserBook>> = ref([])

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

const eventsForYear = () => {
  if (currentYear.value !== null) {
    updatePageLoading(true)
    dataService.myReadingEvents(nonCurrentlyReadingEvents, undefined, undefined, undefined, `${currentYear.value}-01-01`, `${currentYear.value + 1}-01-01`, pageAsNumber.value - 1, perPage.value, 'endDate,desc')
    .then(res => {
      total.value = res.totalElements
      yearEvents.value = res.content
      if (! res.empty) {
          page.value =  (res.number + 1).toString(10)
        }
        else {
          page.value = "1"
        }
        loading.value = false
        updatePageLoading(false)
    })
    .catch(e => {
      console.log(e)
      loading.value = false
      updatePageLoading(false)
    })
  }
}

const loading = ref(false)

const years: Ref<Array<number>> = ref([])
const currentYear: Ref<number|null> = ref(null)

watch(currentYear, (newVal, oldVal) => {
  console.log("year " + newVal + " " + oldVal)
  eventsForYear()
})

const loaderFullPage = ref(false)

// watches set above sometimes called twice
// so getBooks was sometimes called twice at the same instant
const throttledGetEvents = useThrottleFn(() => {
  eventsForYear()
}, 100, false)

watch(page, (newVal, oldVal) => {
  console.log("page " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetEvents()
  }
})

const eventsByMonth: Ref<Map<number, Array<ReadingEventWithUserBook>>> = computed(() => {
  const monthEvents = new Map();
  yearEvents.value.forEach(ev => {
    let month = ev.endDate?.getMonth()
    if (month !== undefined) {
      month ++
    }
    if (!monthEvents.has(month)) {
      monthEvents.set(month, [ev])
    } else {
      const existing: Array<ReadingEventWithUserBook> = monthEvents.get(month)
      existing.push(ev)
      monthEvents.set(month, existing)
    }
  })
  console.log(monthEvents)
  return monthEvents
})

getYears()

</script>

<template>
  <div class="grid grid-cols-1 justify-center justify-items-center justify-self-center mb-3">
    <h1
      class="text-2xl typewriter w-11/12 sm:w-8/12 py-4 capitalize"
    >
      {{ t('history.years') }}
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
      <div
        v-for="[month, ev] in eventsByMonth"
        :key="month"
      >
        <div class="flex items-center mx-2">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-6 w-6 inline"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            stroke-width="2"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
            />
          </svg><h2 class="typewriter text-2xl text-left mx-2 my-2">
            {{ dayjs(`2020-${month}-1`).format('MMMM') }}
          </h2>
        </div>
        <div class="grid grid-cols-2 md:grid-cols-4 xl:grid-cols-6 2xl:grid-cols-8 gap-2 justify-center justify-items-center justify-self-center">
          <div
            v-for="event in ev"
            :key="event.id"
          >
            <book-card
              :book="event.userBook"
              class="h-full"
              :force-select="false"
              :show-select="false"
              :propose-add="true"
            >
              <template #date>
                <div class="badge badge-accent absolute bottom-0 left-1">
                  {{ d(event.endDate!!, 'short') }}
                </div>
              </template>
            </book-card>
          </div>
        </div>
        <div class="divider" /> 
      </div>
    </div>
  </div>
  <o-pagination
    v-if="yearEvents.length > 0"
    :current="pageAsNumber"
    :total="total"
    order="centered"
    :per-page="perPage"
    @change="updatePage"
  />
  <o-loading
    v-model:active="getPageIsLoading"
    :full-page="true"
    :cancelable="true"
    :overlay="loaderFullPage"
  />
</template>

<style scoped>

</style>
