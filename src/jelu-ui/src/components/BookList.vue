<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import { useRouteQuery } from '@vueuse/router';
import { computed, onMounted, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import useBulkEdition from '../composables/bulkEdition';
import usePagination from '../composables/pagination';
import useSort from "../composables/sort";
import { UserBook } from "../model/Book";
import { ReadingEventType } from "../model/ReadingEvent";
import dataService from "../services/DataService";
import BookCard from "./BookCard.vue";
import SortFilterBarVue from "./SortFilterBar.vue";

useTitle('Jelu | My books')

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const books: Ref<Array<UserBook>> = ref([]);

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('lastReadingEventDate,desc')

const { showSelect, selectAll, checkedCards, cardChecked, toggleEdit } = useBulkEdition(modalClosed)

const open = ref(false)

const getBookIsLoading: Ref<boolean> = ref(false)

const toRead: Ref<string|null> = useRouteQuery('toRead', "null")
const owned: Ref<string|null> = useRouteQuery('owned', "null")
const borrowed: Ref<string|null> = useRouteQuery('borrowed', "null")

const userId: Ref<string|null> = useRouteQuery('userId', null)

const eventTypes: Ref<Array<ReadingEventType>> = useRouteQuery('lastEventTypes', [])
const username = ref("")

const getUsername = async () => {
  if (userId.value != null) {
    username.value = await dataService.usernameById(userId.value)
  }
}

getUsername()

watch([page, eventTypes, toRead, owned, borrowed, sortQuery], (newVal, oldVal) => {
  console.log("all " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetBooks()
  }
})

const message = computed(() => {
  if (userId.value != null) {
    return t('labels.books_from_name', { name: username.value })
  } else {
    return t('nav.my_books')
  }
} )

const toReadAsBool = computed(() => {
  if (toRead.value?.toLowerCase() === "null") {
    return null
  } else if (toRead.value?.toLowerCase() === "true") {
    return true
  } else {
    return false
  }
  }
)

const ownedAsBool = computed(() => {
  if (owned.value?.toLowerCase() === "null") {
    return null
  } else if (owned.value?.toLowerCase() === "true") {
    return true
  } else {
    return false
  }
  }
)

const borrowedAsBool = computed(() => {
  if (borrowed.value?.toLowerCase() === "null") {
    return null
  } else if (borrowed.value?.toLowerCase() === "true") {
    return true
  } else {
    return false
  }
  }
)

const getBooks = () => {
  getBookIsLoading.value = true
  dataService.findUserBookByCriteria(eventTypes.value, null, userId.value, 
  toReadAsBool.value, ownedAsBool.value, borrowedAsBool.value,
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
        getBookIsLoading.value = false
        updatePageLoading(false)
    }
    )
    .catch(e => {
      getBookIsLoading.value = false
      updatePageLoading(false)
    })
  
};

// watches set above sometimes called twice
// so getBooks was sometimes called twice at the same instant
const throttledGetBooks = useThrottleFn(() => {
  getBooks()
}, 100, false)

onMounted(() => {
  console.log("Component is mounted!");
});

function modalClosed() {
  console.log("modal closed")
  throttledGetBooks()
}


try {
  getBooks();
} catch (error) {
  console.log("failed get books : " + error);
}

</script>

<template>
  <sort-filter-bar-vue
    :open="open"
    :order="sortOrder"
    @update:open="open = $event"
    @update:sort-order="sortOrderUpdated"
  >
    <template #sort-fields>
      <div class="field">
        <label class="label">{{ t('sorting.sort_by') }} : </label>
        <o-radio
          v-model="sortBy"
          native-value="lastReadingEventDate"
        >
          {{ t('sorting.last_reading_event_date') }}
        </o-radio>
      </div>
      <div class="field">
        <o-radio
          v-model="sortBy"
          native-value="creationDate"
        >
          {{ t('sorting.date_added') }}
        </o-radio>
      </div>
      <div class="field">
        <o-radio
          v-model="sortBy"
          native-value="title"
        >
          {{ t('sorting.title') }}
        </o-radio>
      </div>
      <div class="field">
        <o-radio
          v-model="sortBy"
          native-value="publisher"
        >
          {{ t('sorting.publisher') }}
        </o-radio>
      </div>
      <div class="field">
        <o-radio
          v-model="sortBy"
          native-value="series"
        >
          {{ t('sorting.series') }}
        </o-radio>
      </div>
      <div class="field">
        <o-radio
          v-model="sortBy"
          native-value="pageCount"
        >
          {{ t('sorting.page_count') }}
        </o-radio>
      </div>
      <div class="field">
        <o-radio
          v-model="sortBy"
          native-value="usrAvgRating"
        >
          {{ t('sorting.user_avg_rating') }}
        </o-radio>
      </div>
      <div class="field">
        <o-radio
          v-model="sortBy"
          native-value="avgRating"
        >
          {{ t('sorting.avg_rating') }}
        </o-radio>
        <o-radio
          v-model="sortBy"
          native-value="random"
        >
          {{ t('sorting.random') }}
        </o-radio>
      </div>
    </template>
    <template #filters>
      <div class="field capitalize flex flex-col gap-1">
        <label class="label">{{ t('reading_events.last_event_type') }} : </label>
        <o-checkbox
          v-model="eventTypes"
          native-value="FINISHED"
        >
          {{ t('reading_events.finished') }}
        </o-checkbox>
        <o-checkbox
          v-model="eventTypes"
          native-value="CURRENTLY_READING"
        >
          {{ t('reading_events.currently_reading') }}
        </o-checkbox>
        <o-checkbox
          v-model="eventTypes"
          native-value="DROPPED"
        >
          {{ t('reading_events.dropped') }}
        </o-checkbox>
      </div>
      <div class="field">
        <label class="label">{{ t('filtering.book_in_list') }} : </label>
        <div class="field">
          <o-radio
            v-model="toRead"
            native-value="null"
          >
            {{ t('filtering.unset') }}
          </o-radio>
        </div>
        <div class="field">
          <o-radio
            v-model="toRead"
            native-value="false"
          >
            {{ t('labels.false') }}
          </o-radio>
        </div>
        <div class="field">
          <o-radio
            v-model="toRead"
            native-value="true"
          >
            {{ t('labels.true') }}
          </o-radio>
        </div>
      </div>
      <div class="field">
        <label class="label">{{ t('filtering.owned') }} : </label>
        <div class="field">
          <o-radio
            v-model="owned"
            native-value="null"
          >
            {{ t('filtering.unset') }}
          </o-radio>
        </div>
        <div class="field">
          <o-radio
            v-model="owned"
            native-value="false"
          >
            {{ t('labels.false') }}
          </o-radio>
        </div>
        <div class="field">
          <o-radio
            v-model="owned"
            native-value="true"
          >
            {{ t('labels.true') }}
          </o-radio>
        </div>
      </div>
      <div class="field">
        <label class="label">{{ t('filtering.borrowed') }} : </label>
        <div class="field">
          <o-radio
            v-model="borrowed"
            native-value="null"
          >
            {{ t('filtering.unset') }}
          </o-radio>
        </div>
        <div class="field">
          <o-radio
            v-model="borrowed"
            native-value="false"
          >
            {{ t('labels.false') }}
          </o-radio>
        </div>
        <div class="field">
          <o-radio
            v-model="borrowed"
            native-value="true"
          >
            {{ t('labels.true') }}
          </o-radio>
        </div>
      </div>
    </template>
  </sort-filter-bar-vue>
  <div class="flex flex-row justify-between">
    <div class="flex flex-row gap-1 order-last sm:order-first">
      <o-button
        variant="success"
        outlined
        @click="open = !open"
      >
        <span class="icon text-lg">
          <i class="mdi mdi-filter-variant" />
        </span>
      </o-button>
      <button
        v-tooltip="t('bulk.toggle')"
        class="btn btn-outline btn-primary"
        @click="showSelect = !showSelect"
      >
        <span class="icon text-lg">
          <i class="mdi mdi-pencil" />
        </span>
      </button>
      <button
        v-if="showSelect"
        v-tooltip="t('bulk.select_all')"
        class="btn btn-outline btn-accent"
        @click="selectAll = !selectAll"
      >
        <span class="icon text-lg">
          <i class="mdi mdi-checkbox-multiple-marked" />
        </span>
      </button>
      <button
        v-if="showSelect && checkedCards.length > 0"
        v-tooltip="t('bulk.edit')"
        class="btn btn-outline btn-info"
        @click="toggleEdit(checkedCards)"
      >
        <span class="icon text-lg">
          <i class="mdi mdi-book-edit" />
        </span>
      </button>
    </div>
    <h2 class="text-3xl typewriter capitalize">
      <span class="icon">
        <i class="mdi mdi-bookshelf" />
      </span>
      &nbsp; {{ message }} :
    </h2>
    <div />
  </div>
  <div
    v-if="books.length > 0"
    class="grid grid-cols-2 md:grid-cols-4 xl:grid-cols-6 2xl:grid-cols-8 gap-0 my-3 shrink-0 grow-0"
  >
    <TransitionGroup name="list">
      <div
        v-for="book in books"
        :key="book.id"
        class="m-1"
      >
        <book-card
          :book="book"
          :force-select="selectAll"
          :show-select="showSelect"
          :propose-add="true"
          class="h-full"
          @update:modal-closed="modalClosed"
          @update:checked="cardChecked"
        />
      </div>
    </TransitionGroup>
  </div>
  <div
    v-else-if="getBookIsLoading"
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
    <h2 class="text-3xl typewriter capitalize">
      {{ t('labels.library_empty') }}
    </h2>
    <span class="icon">
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
  <o-loading
    v-model:active="getPageIsLoading"
    :full-page="true"
    :can-cancel="true"
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

.list-enter-active,
.list-leave-active {
  transition: all 0.2s ease;
}
.list-enter-from,
.list-leave-to {
  opacity: 0;
}

</style>
