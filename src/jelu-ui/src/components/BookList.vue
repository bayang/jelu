<script setup lang="ts">
import { computed, onMounted, Ref, ref, watch } from "vue";
import { useRouteQuery } from '@vueuse/router';
import dataService from "../services/DataService";
import { UserBook } from "../model/Book";
import BookCard from "./BookCard.vue";
import usePagination from '../composables/pagination';
import { ReadingEventType } from "../model/ReadingEvent";
import { useRouteQueryArray } from "../composables/useVueRouterArray";
import { useThrottleFn } from '@vueuse/core'
import useSort from "../composables/sort";
import SortFilterBarVue from "./SortFilterBar.vue";
import { useTitle } from '@vueuse/core'

useTitle('Jelu | My books')

const books: Ref<Array<UserBook>> = ref([]);

const { total, page, pageAsNumber, perPage, updatePage } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('lastReadingEventDate,desc')

const open = ref(false)

const toRead: Ref<string|null> = useRouteQuery('toRead', "null")

const eventTypes: Ref<Array<ReadingEventType>> = useRouteQueryArray('lastEventTypes', [])

watch([page, eventTypes, toRead, sortQuery], (newVal, oldVal) => {
  console.log("all " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetBooks()
  }
})

const toReadAsBool = computed(() => toRead.value?.toLowerCase() === "null" ? null : toRead.value?.toLowerCase() === "true")

const getBooks = () => {
  dataService.findUserBookByCriteria(eventTypes.value, toReadAsBool.value, 
  pageAsNumber.value - 1, perPage.value, sortQuery.value)
  .then(res => {
        console.log(res)
          total.value = res.totalElements
          books.value = res.content
        if (! res.empty) {
          page.value =  (res.number + 1).toString(10)
        }
        else {
          page.value = "1"
        }
    }
    )
  
};

// watches set above sometimes called twice
// so getBooks was sometimes called twice at the same instant
const throttledGetBooks = useThrottleFn(() => {
  getBooks()
}, 100, false)

// const sortOrderUpdated = (newval: string) => {
//   console.log('sortOrderUpdated ' + newval)
//   sortOrder.value = newval
// }

onMounted(() => {
  console.log("Component is mounted!");
  try {
    getBooks();
  } catch (error) {
    console.log("failed get books : " + error);
  }
});
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
          native-value="lastReadingEventDate"
        >
          Last reading event date
        </o-radio>
      </div>
      <div class="field">
        <o-radio
          v-model="sortBy"
          native-value="creationDate"
        >
          Date added
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
      <div class="field">
        <label class="label">Book is in to-read list : </label>
        <div class="field">
          <o-radio
            v-model="toRead"
            native-value="null"
          >
            Unset
          </o-radio>
        </div>
        <div class="field">
          <o-radio
            v-model="toRead"
            native-value="false"
          >
            False
          </o-radio>
        </div>
        <div class="field">
          <o-radio
            v-model="toRead"
            native-value="true"
          >
            True
          </o-radio>
        </div>
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
      <span class="icon">
        <i class="mdi mdi-bookshelf" />
      </span>
      &nbsp; My books :
    </h2>
    <div />
  </div>
  <div
    v-if="books.length > 0"
    class="grid grid-cols-2 sm:grid-cols-8 gap-0"
  >
    <div
      v-for="book in books"
      :key="book.id"
      class="m-1"
    >
      <router-link
        v-if="book.id != undefined"
        :to="{ name: 'book-detail', params: { bookId: book.id } }"
      >
        <book-card :book="book" />
      </router-link>
    </div>
  </div>
  <div v-else>
    <h2 class="title has-text-weight-normal typewriter">
      Library is empty
    </h2>
    <span class="icon is-large">
      <i class="mdi mdi-book-open-page-variant-outline mdi-48px" />
    </span>
  </div>

  <o-pagination
    v-if="books.length > 0"
    :current="pageAsNumber"
    :total="total"
    order="centered"
    :per-page="perPage"
    @change="updatePage"
  />
</template>

<style scoped>

label {
  margin: 0 0.5em;
  font-weight: bold;
}

/* fields in side bar slots are shifted to the right and alignment is broken */
.field {
  margin-left: -8px;
}

</style>
