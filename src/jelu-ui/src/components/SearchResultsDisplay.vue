<script setup lang="ts">
import { useTitle } from '@vueuse/core';
import { useRouteQuery } from '@vueuse/router';
import { computed, Ref, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import usePagination from '../composables/pagination';
import useSort from '../composables/sort';
import useBulkEdition from '../composables/bulkEdition';
import { Book } from '../model/Book';
import { LibraryFilter } from '../model/LibraryFilter';
import dataService from "../services/DataService";
import { ObjectUtils } from '../utils/ObjectUtils';
import BookCard from "./BookCard.vue";
import SortFilterBarVue from "./SortFilterBar.vue";
import { ReadingEventType } from '../model/ReadingEvent';

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

useTitle('Jelu | Search')

const searchQuery: Ref<string|undefined> = useRouteQuery('q', undefined)

const books: Ref<Array<Book>> = ref([]);

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('title,asc')

const { showSelect, selectAll, checkedCards, cardChecked, toggleEdit } = useBulkEdition(modalClosed)

const libraryFilter: Ref<LibraryFilter> = useRouteQuery('libraryFilter', 'ANY' as LibraryFilter)

const open = ref(false)

const progress: Ref<boolean> = ref(false)

const eventTypes: Ref<Array<ReadingEventType>> = useRouteQuery('lastEventTypes', [])
const toRead: Ref<string|null> = useRouteQuery('toRead', "null")
const owned: Ref<string|null> = useRouteQuery('owned', "null")
const borrowed: Ref<string|null> = useRouteQuery('borrowed', "null")

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

const search = () => {
    progress.value = true
    updatePageLoading(true)
      dataService.findBooks(
        searchQuery.value, 
      pageAsNumber.value - 1, perPage.value, 
      sortQuery.value, libraryFilter.value,
      eventTypes.value, toReadAsBool.value, 
      ownedAsBool.value, borrowedAsBool.value
      )
    .then(res => {
      progress.value = false
      updatePageLoading(false)
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
    .catch(e => {
      progress.value = false
      updatePageLoading(false)
    })
}

watch([page, sortQuery, libraryFilter, eventTypes, toRead, owned, borrowed], (newVal, oldVal) => {
  console.log(page.value)
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal) {
    search()
  }
})

const convertedBooks = computed(() => books.value?.map(b => ObjectUtils.toUserBook(b)))

function modalClosed() {
  console.log("modal closed")
  search()
}

const append = (text: string) => {
  if (searchQuery.value == null) {
    searchQuery.value = ''
  }
  searchQuery.value += text
  //@ts-ignore
  document.getElementById("search_helper").close()
}

const terms = ["tag", "author", "translator", "narrator", "series", "language", 
"published_date", "publisher", "summary", "googleId", "goodreadsId", "amazonId", 
"librarythingId", "noosfereId", "isfdbId", "inventaireId", "openlibraryId"]

const operators = ["AND", "OR", "NOT"]

if (searchQuery.value != null) {
    search()
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
      <div class="field flex flex-col items-start gap-1">
        <label class="label">{{ t('sorting.sort_by') }} : </label>
        <o-radio
          v-model="sortBy"
          native-value="title"
        >
          {{ t('sorting.title') }}
        </o-radio>
        <o-radio
          v-model="sortBy"
          native-value="publisher"
        >
          {{ t('sorting.publisher') }}
        </o-radio>
        <o-radio
          v-model="sortBy"
          native-value="series"
        >
          {{ t('sorting.series') }}
        </o-radio>
        <o-radio
          v-model="sortBy"
          native-value="publishedDate"
        >
          {{ t('sorting.publication_date') }}
        </o-radio>
        <o-radio
          v-model="sortBy"
          native-value="pageCount"
        >
          {{ t('sorting.page_count') }}
        </o-radio>
      </div>
    </template>
    <template #filters>
      <div class="field flex flex-col items-start gap-1">
        <label class="label">{{ t('filtering.books_type') }} : </label>
        <o-radio
          v-model="libraryFilter"
          native-value="ANY"
        >
          {{ t('filtering.any') }}
        </o-radio>
        <o-radio
          v-model="libraryFilter"
          native-value="ONLY_USER_BOOKS"
        >
          {{ t('filtering.only_in_my_list') }}
        </o-radio>
        <o-radio
          v-model="libraryFilter"
          native-value="ONLY_NON_USER_BOOKS"
        >
          {{ t('filtering.only_not_in_my_list') }}
        </o-radio>
      </div>
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
        <o-checkbox
          v-model="eventTypes"
          native-value="NONE"
        >
          {{ t('reading_events.none') }}
        </o-checkbox>
      </div>
      <div class="field flex flex-col items-start">
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
      <div class="field flex flex-col items-start">
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
      <div class="field flex flex-col items-start">
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
  <div class="flex flex-row sm:justify-between justify-center justify-items-center w-11/12 pb-2">
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
    <div />
  </div>
  <div
    class="flex flex-row justify-center justify-items-center mb-2"
  >
    <div class="basis-full sm:basis-5/12">
      <div class="basis-full content-center items-center">
        <o-field class="title-input">
          <o-input
            v-model="searchQuery"
            :placeholder="t('labels.search_query')" 
            type="search"
            icon="magnify" 
            icon-clickable
            icon-pack="mdi"
            class="input focus:input-accent"
            @keyup.enter="search"
          />
          <o-button
            v-tooltip="t('labels.search_selected')"
            variant="success"
            icon-pack="mdi"
            icon-right="magnify"
            @click="search"
          />
        </o-field>
        <div class="flex justify-center">
          <a
            class="link hover:link-accent tooltip self-center"
            :data-tip="t('labels.search_documentation')"
            href="https://bayang.github.io/jelu-web/usage/search/"
            target="_blank"
          ><svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke-width="1.5"
            stroke="currentColor"
            class="w-6 h-6"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M9.879 7.519c1.171-1.025 3.071-1.025 4.242 0 1.172 1.025 1.172 2.687 0 3.712-.203.179-.43.326-.67.442-.745.361-1.45.999-1.45 1.827v.75M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9 5.25h.008v.008H12v-.008z"
            />
          </svg></a>
          <button
            class="btn mx-2"
            onclick="search_helper.showModal()"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
              class="size-6"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M14.25 6.087c0-.355.186-.676.401-.959.221-.29.349-.634.349-1.003 0-1.036-1.007-1.875-2.25-1.875s-2.25.84-2.25 1.875c0 .369.128.713.349 1.003.215.283.401.604.401.959v0a.64.64 0 0 1-.657.643 48.39 48.39 0 0 1-4.163-.3c.186 1.613.293 3.25.315 4.907a.656.656 0 0 1-.658.663v0c-.355 0-.676-.186-.959-.401a1.647 1.647 0 0 0-1.003-.349c-1.036 0-1.875 1.007-1.875 2.25s.84 2.25 1.875 2.25c.369 0 .713-.128 1.003-.349.283-.215.604-.401.959-.401v0c.31 0 .555.26.532.57a48.039 48.039 0 0 1-.642 5.056c1.518.19 3.058.309 4.616.354a.64.64 0 0 0 .657-.643v0c0-.355-.186-.676-.401-.959a1.647 1.647 0 0 1-.349-1.003c0-1.035 1.008-1.875 2.25-1.875 1.243 0 2.25.84 2.25 1.875 0 .369-.128.713-.349 1.003-.215.283-.4.604-.4.959v0c0 .333.277.599.61.58a48.1 48.1 0 0 0 5.427-.63 48.05 48.05 0 0 0 .582-4.717.532.532 0 0 0-.533-.57v0c-.355 0-.676.186-.959.401-.29.221-.634.349-1.003.349-1.035 0-1.875-1.007-1.875-2.25s.84-2.25 1.875-2.25c.37 0 .713.128 1.003.349.283.215.604.401.96.401v0a.656.656 0 0 0 .658-.663 48.422 48.422 0 0 0-.37-5.36c-1.886.342-3.81.574-5.766.689a.578.578 0 0 1-.61-.58v0Z"
              />
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
  
  <dialog
    id="search_helper"
    ref="search_helper"
    class="modal"
  >
    <div class="modal-box space-x-3 space-y-3">
      <span
        v-for="term in terms"
        :key="term"
        class="badge badge-outline"
        @click="append(` ${term}:`)"
      >{{ term }}</span>
      <br>
      <span
        v-for="op in operators"
        :key="op"
        class="badge badge-outline badge-accent"
        @click="append(` ${op} `)"
      >{{ op }}</span>
    </div>
    <form
      method="dialog"
      class="modal-backdrop"
    >
      <button>close</button>
    </form>
  </dialog>
  <o-pagination
    v-if="pageCount > 1"
    v-model:current="pageAsNumber"
    :total="total"
    order="centered"
    :per-page="perPage"
    @change="updatePage"
  />
  <div class="grid grid-cols-2 md:grid-cols-4 xl:grid-cols-6 2xl:grid-cols-8 gap-1 my-4">
    <div
      v-for="book in convertedBooks"
      :key="book.book.id"
      class="m-1"
    >
      <book-card
        :book="book"
        :force-select="selectAll"
        :public="false"
        :show-select="showSelect"
        :propose-add="true"
        class="h-full"
        @update:modal-closed="modalClosed"
        @update:checked="cardChecked"
      />
    </div>
  </div>
  <o-pagination
    v-model:current="pageAsNumber"
    :total="total"
    order="centered"
    :per-page="perPage"
    @change="updatePage"
  />
  <o-loading
    v-model:active="getPageIsLoading"
    :full-page="true"
    :cancelable="true"
  />  
</template>

<style scoped>

label.label {
  font-weight: bold;
}

</style>
