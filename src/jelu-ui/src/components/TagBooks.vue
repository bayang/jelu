<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import { useRouteQuery } from "@vueuse/router";
import { computed, Ref, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import usePagination from '../composables/pagination';
import useSort from "../composables/sort";
import useBulkEdition from '../composables/bulkEdition';
import { Book } from '../model/Book';
import { LibraryFilter } from "../model/LibraryFilter";
import { Tag } from '../model/Tag';
import dataService from "../services/DataService";
import { ObjectUtils } from '../utils/ObjectUtils';
import BookCard from "./BookCard.vue";
import SortFilterBarVue from "./SortFilterBar.vue";
import { ReadingEventType } from '../model/ReadingEvent';

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const route = useRoute()

const tag: Ref<Tag> = ref({name: ""})
const tagBooks: Ref<Array<Book>> = ref([]);

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('title,asc')

const libraryFilter: Ref<LibraryFilter> = useRouteQuery('libraryFilter', 'ANY' as LibraryFilter)

const eventTypes: Ref<Array<ReadingEventType>> = useRouteQuery('lastEventTypes', [])

const { showSelect, selectAll, checkedCards, cardChecked, toggleEdit } = useBulkEdition(modalClosed)

const open = ref(false)

const getBooksIsLoading: Ref<boolean> = ref(false)

watch([() => route.params.tagId, page, sortQuery, libraryFilter, eventTypes], (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal && route.params.tagId !== undefined) {
    throttledGetBooks()
  }
})

watch(() => route.params.tagId, (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal && route.params.tagId !== undefined) {
    getTag()
  }
})

const getTag = async () => {
  try {
    tag.value = await dataService.getTagById(route.params.tagId as string)
    useTitle('Jelu | #' + tag.value.name)
  } catch (error) {
    console.log("failed get tag : " + error);
  }
};

const getBooks = () => {
    getBooksIsLoading.value = true
    dataService.getTagBooksById(route.params.tagId as string, 
      pageAsNumber.value - 1, perPage.value, sortQuery.value, 
      libraryFilter.value, eventTypes.value)
      .then(res => {
        console.log(res)
            total.value = res.totalElements
            tagBooks.value = res.content
          if (! res.empty) {
            page.value =  (res.number + 1).toString(10)
          }
          else {
            page.value = "1"
          }
          getBooksIsLoading.value = false
          updatePageLoading(false)
      }
      )
      .catch(e => {
        getBooksIsLoading.value = false
        updatePageLoading(false)
      })
};

const throttledGetBooks = useThrottleFn(() => {
  getBooks()
}, 100, false)

const convertedBooks = computed(() => tagBooks.value?.map(b => ObjectUtils.unwrapUserBook(b)))

function modalClosed() {
  console.log("modal closed")
  throttledGetBooks()
}

getTag()
getBooks()

</script>

<template>
  <sort-filter-bar-vue
    :open="open"
    :order="sortOrder"
    @update:open="open = $event"
    @update:sort-order="sortOrderUpdated"
  >
    <template #sort-fields>
      <div class="flex flex-col gap-1">
        <label class="label">{{ t('sorting.sort_by') }} : </label>
        <div class="">
          <input
            v-model="sortBy"
            type="radio"
            name="radio-22"
            class="radio radio-primary mb-2"
            value="title"
          >
          <span class="label-text">{{ t('sorting.title') }}</span>
        </div>
        <div class="">
          <input
            v-model="sortBy"
            type="radio"
            name="radio-22"
            class="radio radio-primary mb-2"
            value="publisher"
          >
          <span class="label-text">{{ t('sorting.publisher') }}</span>
        </div>
        <div class="">
          <input
            v-model="sortBy"
            type="radio"
            name="radio-22"
            class="radio radio-primary mb-2"
            value="series"
          >
          <span class="label-text">{{ t('sorting.series') }}</span>
        </div>
        <div class="">
          <input
            v-model="sortBy"
            type="radio"
            name="radio-22"
            class="radio radio-primary mb-2"
            value="publishedDate"
          >
          <span class="label-text">{{ t('sorting.publication_date') }}</span>
        </div>
        <div class="">
          <input
            v-model="sortBy"
            type="radio"
            name="radio-22"
            class="radio radio-primary mb-2"
            value="modificationDate"
          >
          <span class="label-text">{{ t('sorting.modification_date') }}</span>
        </div>
        <div class="">
          <input
            v-model="sortBy"
            type="radio"
            name="radio-22"
            class="radio radio-primary"
            value="random"
          >
          <span class="label-text">{{ t('sorting.random') }}</span>
        </div>
        <div class="">
          <input
            v-model="sortBy"
            type="radio"
            name="radio-22"
            class="radio radio-primary mb-2"
            value="pageCount"
          >
          <span class="label-text">{{ t('sorting.page_count') }}</span>
        </div>
      </div>
    </template>
    <template #filters>
      <div class="flex flex-col gap-1">
        <label class="label">{{ t('filtering.books_type') }} : </label>
        <div class="">
          <input
            v-model="libraryFilter"
            type="radio"
            name="radio-51"
            class="radio radio-primary my-2"
            value="ANY"
          >
          <span class="label-text">{{ t('filtering.any') }}</span>
        </div>
        <div class="">
          <input
            v-model="libraryFilter"
            type="radio"
            name="radio-51"
            class="radio radio-primary my-2"
            value="ONLY_USER_BOOKS"
          >
          <span class="label-text">{{ t('filtering.only_in_my_list') }}</span>
        </div>
        <div class="">
          <input
            v-model="libraryFilter"
            type="radio"
            name="radio-51"
            class="radio radio-primary my-2"
            value="ONLY_NON_USER_BOOKS"
          >
          <span class="label-text">{{ t('filtering.only_not_in_my_list') }}</span>
        </div>
      </div>
      <div class="capitalize flex flex-col gap-1">
        <label class="label">{{ t('reading_events.last_event_type') }} : </label>
        <label class="label">
          <input
            v-model="eventTypes"
            type="checkbox"
            class="checkbox checkbox-primary"
            value="FINISHED"
          >
          {{ t('reading_events.finished') }}
        </label>
        <label class="label">
          <input
            v-model="eventTypes"
            type="checkbox"
            class="checkbox checkbox-primary"
            value="CURRENTLY_READING"
          >
          {{ t('reading_events.currently_reading') }}
        </label>
        <label class="label">
          <input
            v-model="eventTypes"
            type="checkbox"
            class="checkbox checkbox-primary"
            value="DROPPED"
          >
          {{ t('reading_events.dropped') }}
        </label>
      </div>
    </template>
  </sort-filter-bar-vue>
  <div class="flex flex-row justify-between mb-2">
    <div class="flex flex-row gap-1 order-last sm:order-first">
      <button
        class="btn btn-outline btn-success"
        @click="open = !open"
      >
        <span class="icon text-lg">
          <i class="mdi mdi-filter-variant" />
        </span>
      </button>
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
    <h2 class="text-3xl typewriter">
      <span class="icon">
        <i class="mdi mdi-bookshelf" />
      </span>
      {{ t('labels.books_tagged_name', { tag : tag.name}) }} :
    </h2>
    <div />
  </div>
  <div
    v-if="getBooksIsLoading && tagBooks.length < 1"
    class="flex flex-row justify-center justify-items-center gap-3"
  >
    <o-skeleton
      class="justify-self-center basis-44"
      height="250px"
      :animated="true"
    />
    <o-skeleton
      class="justify-self-center basis-44"
      height="250px"
      :animated="true"
    />
  </div>
  <o-pagination
    v-if="pageCount > 1"
    v-model:current="pageAsNumber"
    :total="total"
    order="centered"
    :per-page="perPage"
    @change="updatePage"
  />
  <div class="grid grid-cols-2 md:grid-cols-4 xl:grid-cols-6 2xl:grid-cols-8 gap-1 my-3">
    <div
      v-for="book in convertedBooks"
      :key="book.book.id"
      class="books-grid-item my-2"
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

label {
  font-weight: bold;
}

</style>
