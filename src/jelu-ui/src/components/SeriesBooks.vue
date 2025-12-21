<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import { useRouteQuery } from "@vueuse/router";
import { computed, Ref, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import useBulkEdition from '../composables/bulkEdition';
import usePagination from '../composables/pagination';
import useSort from "../composables/sort";
import { Book } from '../model/Book';
import { LibraryFilter } from "../model/LibraryFilter";
import dataService from "../services/DataService";
import { ObjectUtils } from '../utils/ObjectUtils';
import BookCard from "./BookCard.vue";
import SortFilterBarVue from "./SortFilterBar.vue";
import { Series } from '../model/Series';
import SeriesModalVue from './SeriesModal.vue'
import { useOruga } from "@oruga-ui/oruga-next"

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const route = useRoute()
const oruga = useOruga();

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('numberInSeries,asc')

const libraryFilter: Ref<LibraryFilter> = useRouteQuery('libraryFilter', 'ANY' as LibraryFilter)

const { showSelect, selectAll, checkedCards, cardChecked, toggleEdit } = useBulkEdition(modalClosed)

const open = ref(false)

const series: Ref<Series> = ref({name: ""})
const books: Ref<Array<Book>> = ref([]);

const getBooksIsLoading: Ref<boolean> = ref(false)

watch([() => route.params.seriesId, page, sortQuery, libraryFilter], (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal && route.params.seriesId !== undefined) {
    throttledGetBooks()
  }
})

watch(() => route.params.seriesId, (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal && route.params.seriesId !== undefined) {
    getSeries()
  }
})

const getSeries = async () => {
  try {
    series.value = await dataService.getSeriesById(route.params.seriesId as string)
    useTitle('Jelu | ' + series.value.name)
  } catch (error) {
    console.log("failed get series : " + error);
  }
};

const getBooks = () => {
    getBooksIsLoading.value = true
    dataService.getSeriesBooksById(route.params.seriesId as string, 
      pageAsNumber.value - 1, perPage.value, sortQuery.value, 
      libraryFilter.value)
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

const convertedBooks = computed(() => books.value?.map(b => ObjectUtils.toUserBook(b)))

function modalClosed() {
  console.log("modal closed")
  throttledGetBooks()
}

function seriesModalClosed() {
  console.log("series modal closed")
  getSeries()
}

function toggleSeriesModal(series: Series, edit: boolean) {
    oruga.modal.open({
      component: SeriesModalVue,
      trapFocus: true,
      active: true,
      canCancel: ['x', 'button', 'outside'],
      scroll: 'keep',
      props: {
        "series": series,
        "edit" : edit,
      },
      onClose: seriesModalClosed
    });
}

getSeries()
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
      <div class="field flex flex-col items-start gap-1">
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
            value="numberInSeries"
          >
          <span class="label-text">{{ t('sorting.series_number') }}</span>
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
            class="radio radio-primary mb-2"
            value="pageCount"
          >
          <span class="label-text">{{ t('sorting.page_count') }}</span>
        </div>
      </div>
    </template>
    <template #filters>
      <div class="field flex flex-col items-start gap-1">
        <label class="label">{{ t('filtering.books_type') }} : </label>
        <div class="">
          <input
            v-model="libraryFilter"
            type="radio"
            name="radio-51"
            class="radio radio-primary my-1"
            value="ANY"
          >
          <span class="label-text">{{ t('filtering.any') }}</span>
        </div>
        <div class="">
          <input
            v-model="libraryFilter"
            type="radio"
            name="radio-51"
            class="radio radio-primary my-1"
            value="ONLY_USER_BOOKS"
          >
          <span class="label-text">{{ t('filtering.only_in_my_list') }}</span>
        </div>
        <div class="">
          <input
            v-model="libraryFilter"
            type="radio"
            name="radio-51"
            class="radio radio-primary my-1"
            value="ONLY_NON_USER_BOOKS"
          >
          <span class="label-text">{{ t('filtering.only_not_in_my_list') }}</span>
        </div>
      </div>
    </template>
  </sort-filter-bar-vue>
  <div class="flex flex-row justify-between">
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
      {{ series.name }} : 
      <button
        v-tooltip="t('series.edit_series')"
        class="btn btn-circle btn-outline border-none"
        @click="toggleSeriesModal(series, true)"
      >
        <span class="icon text-lg">
          <i class="mdi mdi-pencil" />
        </span>
      </button>
    </h2>
    <div />
  </div>
  <div class="mb-2">
    <div class="flex justify-center">
      <v-md-preview
        v-if="series.description != null"
        class="text-justify text-base"
        :text="series.description"
      />
    </div>
    <div v-if="series.avgRating != null || series.userRating != null">
      {{ t('labels.avg_rating', {rating : series.avgRating}) }} / {{ t('labels.user_rating', {rating : series.userRating}) }}
    </div>
  </div>
  <div
    v-if="getBooksIsLoading && books.length < 1"
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
        :series-id="series.id"
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
