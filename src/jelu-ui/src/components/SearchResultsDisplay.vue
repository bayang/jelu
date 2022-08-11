<script setup lang="ts">
import { useTitle } from '@vueuse/core';
import { useRouteQuery } from '@vueuse/router';
import { computed, Ref, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import usePagination from '../composables/pagination';
import useSort from '../composables/sort';
import { useRouteQueryArray } from "../composables/useVueRouterArray";
import useBulkEdition from '../composables/bulkEdition';
import { Book } from '../model/Book';
import { LibraryFilter } from '../model/LibraryFilter';
import dataService from "../services/DataService";
import { ObjectUtils } from '../utils/ObjectUtils';
import BookCard from "./BookCard.vue";
import SortFilterBarVue from "./SortFilterBar.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

useTitle('Jelu | Search')

const titleQuery: Ref<string|undefined> = useRouteQuery('title', undefined)
const isbn10Query: Ref<string|undefined> = useRouteQuery('isbn10', undefined)
const isbn13Query: Ref<string|undefined> = useRouteQuery('isbn13', undefined)
const seriesQuery: Ref<string|undefined> = useRouteQuery('series', undefined)
const authorsQuery: Ref<Array<string>> = useRouteQueryArray('authors', [])
const tagsQuery: Ref<Array<string>> = useRouteQueryArray('tags', [])

const tagsArrayString: Ref<string> = ref("")
if (tagsQuery.value.length > 0) {
  tagsArrayString.value = tagsQuery.value.join(",")
}

const authorsArrayString: Ref<string> = ref("")
if (authorsQuery.value.length > 0) {
  authorsArrayString.value = authorsQuery.value.join(",")
}

const books: Ref<Array<Book>> = ref([]);

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('title,asc')

const { showSelect, selectAll, checkedCards, cardChecked, toggleEdit } = useBulkEdition(modalClosed)

const libraryFilter: Ref<LibraryFilter> = useRouteQuery('libraryFilter', 'ANY' as LibraryFilter)

const open = ref(false)

const progress: Ref<boolean> = ref(false)

const search = () => {
    progress.value = true
    updatePageLoading(true)
      dataService.findBooks(titleQuery.value, 
      isbn10Query.value, isbn13Query.value, 
      seriesQuery.value, authorsQuery.value,
      tagsQuery.value,
      pageAsNumber.value - 1, perPage.value, sortQuery.value, libraryFilter.value)
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

const arrayParam = (input: string) => {
  return input.split(",")
}

watch([page, sortQuery, libraryFilter], (newVal, oldVal) => {
  console.log(page.value)
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal) {
    search()
  }
})

watch(authorsArrayString, (newVal, oldVal) => {
  authorsQuery.value = arrayParam(authorsArrayString.value)
})

watch(tagsArrayString, (newVal, oldVal) => {
  tagsQuery.value = arrayParam(tagsArrayString.value)
})

const convertedBooks = computed(() => books.value?.map(b => ObjectUtils.toUserBook(b)))

function modalClosed() {
  console.log("modal closed")
  search()
}

if (titleQuery.value != null || 
  isbn10Query.value != null || 
  isbn13Query.value != null ||
  seriesQuery.value != null ||
  authorsQuery.value.length > 0 ||
  tagsQuery.value.length > 0) {
    search()
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
        <label class="label">{{ t('sorting.sort_by') }} : </label>
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
        <o-radio
          v-model="sortBy"
          native-value="publishedDate"
        >
          {{ t('sorting.publication_date') }}
        </o-radio>
      </div>
    </template>
    <template #filters>
      <div class="field">
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
    class="flex flex-row justify-center justify-items-center"
  >
    <div class="basis-full sm:basis-5/12">
      <div class="basis-full">
        <o-field class="title-input">
          <o-input
            v-model="titleQuery"
            :placeholder="t('labels.search_title')" 
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
      </div>
      <div class="flex flex-wrap justify-center justify-items-center search-form-extended">
        <div class="search-form-extended-input">
          <o-input
            v-model="isbn10Query"
            :placeholder="t('book.isbn10')"
            type="search"
            icon="magnify"
            icon-clickable
            icon-pack="mdi"
            class="input focus:input-accent"
            @keyup.enter="search"
          />
        </div>
        <div class="search-form-extended-input">
          <o-input
            v-model="isbn13Query"
            :placeholder="t('book.isbn13')"
            type="search"
            icon="magnify" 
            icon-clickable
            icon-pack="mdi"
            class="input focus:input-accent"
            @keyup.enter="search"
          />
        </div>
        <div class="search-form-extended-input">
          <o-input
            v-model="authorsArrayString"
            :placeholder="t('book.author', 2)"
            type="search"
            icon="magnify" 
            icon-clickable
            icon-pack="mdi"
            class="input focus:input-accent"
            @keyup.enter="search"
          />
        </div>
        <div class="search-form-extended-input">
          <o-input
            v-model="tagsArrayString"
            :placeholder="t('book.tag', 2)"
            type="search"
            icon="magnify" 
            icon-clickable
            icon-pack="mdi"
            class="input focus:input-accent"
            @keyup.enter="search"
          />
        </div>
        <div class="search-form-extended-input">
          <o-input
            v-model="seriesQuery"
            :placeholder="t('book.series')"
            type="search"
            icon="magnify" 
            icon-clickable
            icon-pack="mdi"
            class="input focus:input-accent"
            @keyup.enter="search"
          />
        </div>
      </div>
    </div>
  </div>
  <div class="grid grid-cols-2 md:grid-cols-4 xl:grid-cols-6 2xl:grid-cols-8 gap-1 my-4">
    <div
      v-for="book in convertedBooks"
      :key="book.book.id"
      class="m-1"
    >
      <book-card
        :book="book"
        :force-select="selectAll"
        :show-select="showSelect"
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


</style>
