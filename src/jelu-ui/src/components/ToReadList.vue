<script setup lang="ts">
import { onMounted, Ref, ref, watch } from 'vue'
import { UserBook } from '../model/Book'
import dataService from "../services/DataService";
import BookCard from "./BookCard.vue";
import SortFilterBarVue from "./SortFilterBar.vue";
import usePagination from '../composables/pagination';
import useSort from '../composables/sort';
import { useThrottleFn } from '@vueuse/core'
import { ReadingEventType } from '../model/ReadingEvent';
import { useRouteQueryArray } from '../composables/useVueRouterArray';
import { useTitle } from '@vueuse/core'

useTitle('Jelu | To read')

const books: Ref<Array<UserBook>> = ref([]);

const { total, page, pageAsNumber, perPage, updatePage } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('creationDate,desc')

const eventTypes: Ref<Array<ReadingEventType>> = useRouteQueryArray('lastEventTypes', [])

const open = ref(false)

const getToReadIsLoading: Ref<boolean> = ref(false)

const getToRead = async () => {
  getToReadIsLoading.value = true
  try {
    const res = await dataService.findUserBookByCriteria(eventTypes.value, 
    true, pageAsNumber.value - 1, perPage.value, sortQuery.value)
    total.value = res.totalElements
    books.value = res.content
    if (! res.empty) {
      page.value =  (res.number + 1).toString(10)
    }
    else {
      page.value = "1"
    }
    getToReadIsLoading.value = false
  } catch (error) {
    console.log("failed get books : " + error);
    getToReadIsLoading.value = false
  }
};

// watches set above sometimes called twice
// so getBooks was sometimes called twice at the same instant
const throttledGetToRead = useThrottleFn(() => {
  getToRead()
}, 100, false)

watch([page, eventTypes, sortQuery], (newVal, oldVal) => {
  console.log("all " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetToRead()
  }
})

onMounted(() => {
  console.log("Component is mounted!");
    
    }
);

getToRead()

const pageleft =  () => {
  console.log("left")
}

</script>

<template>
  <sort-filter-bar-vue
    :open="open"
    :order="sortOrder"
    class="sort-filter-bar"
    @update:open="open = $event"
    @update:sort-order="sortOrderUpdated"
  >
    <template #sort-fields>
      <div class="field">
        <label class="label">Sort by : </label>
        <o-radio
          v-model="sortBy"
          native-value="creationDate"
        >
          Date added to list
        </o-radio>
      </div>
      <div class="field">
        <o-radio
          v-model="sortBy"
          native-value="lastReadingEventDate"
        >
          Last reading event date
        </o-radio>
      </div>
      <div class="field">
        <o-radio
          v-model="sortBy"
          native-value="title"
        >
          Title
        </o-radio>
      </div>
      <div class="field">
        <o-radio
          v-model="sortBy"
          native-value="publisher"
        >
          Publisher
        </o-radio>
      </div>
      <div class="field">
        <o-radio
          v-model="sortBy"
          native-value="series"
        >
          Series
        </o-radio>
      </div>
    </template>
    <template #filters>
      <div class="field">
        <label class="label">Last event type : </label>
        <o-checkbox
          v-model="eventTypes"
          native-value="FINISHED"
        >
          Finished
        </o-checkbox>
        <o-checkbox
          v-model="eventTypes"
          native-value="CURRENTLY_READING"
        >
          Currently reading
        </o-checkbox>
        <o-checkbox
          v-model="eventTypes"
          native-value="DROPPED"
        >
          Dropped
        </o-checkbox>
      </div>
    </template>
  </sort-filter-bar-vue>
  <div class="flex flex-row justify-between">
    <o-button
      variant="success"
      outlined
      class="order-last sm:order-first"
      @click="open = !open"
    >
      <span class="icon">
        <i class="mdi mdi-filter-variant" />
      </span>
    </o-button>
    <h2 class="text-3xl typewriter">
      To Read List :
    </h2>
    <div />
  </div>
  <div
    v-if="books.length > 0"
    class="is-flex is-flex-wrap-wrap is-justify-content-center grid grid-cols-2 sm:grid-cols-8 gap-0"
  >
    <div
      v-for="book in books"
      :key="book.id"
      class="books-grid-item m-2"
    >
      <router-link
        v-if="book.id != undefined"
        :to="{ name: 'book-detail', params: { bookId: book.id } }"
      >
        <book-card :book="book" />
      </router-link>
    </div>
  </div>
  <div
    v-else-if="getToReadIsLoading"
    class="flex flex-row justify-center justify-items-center gap-3"
  >
    <o-skeleton
      class="justify-self-center basis-36"
      height="250px"
      :animated="true"
    />
    <o-skeleton
      class="justify-self-center basis-36"
      height="250px"
      :animated="true"
    />
    <o-skeleton
      class="justify-self-center basis-36"
      height="250px"
      :animated="true"
    />
  </div>
  <div v-else>
    <h2 class="text-3xl typewriter">
      Nothing to read
    </h2>
    <span class="icon is-large">
      <i class="mdi mdi-book-open-page-variant-outline mdi-48px" />
    </span>
  </div>
  <o-pagination
    v-if="books.length > 0"
    v-model:current="pageAsNumber"
    :total="total"
    order="centered"
    :per-page="perPage"
    @change="updatePage"
    @keyup.left="pageleft"
  />
</template>

<style scoped>


</style>
