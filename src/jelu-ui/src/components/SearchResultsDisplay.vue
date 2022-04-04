<script setup lang="ts">
import { computed, Ref, ref, watch } from 'vue'
import { useProgrammatic } from "@oruga-ui/oruga-next";
import { Book, UserBook } from '../model/Book'
import dataService from "../services/DataService";
import BookCard from "./BookCard.vue";
import { ObjectUtils } from '../utils/ObjectUtils';
import EditBookModal from "./EditBookModal.vue"
import SortFilterBarVue from "./SortFilterBar.vue";
import usePagination from '../composables/pagination';
import useSort from '../composables/sort';
import { LibraryFilter } from '../model/LibraryFilter';
import { useRouteQuery } from '@vueuse/router';
import { useTitle } from '@vueuse/core'
import { useRouteQueryArray } from "../composables/useVueRouterArray";

useTitle('Jelu | Search')

const {oruga} = useProgrammatic();

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

const libraryFilter: Ref<LibraryFilter> = useRouteQuery('libraryFilter', 'ANY' as LibraryFilter)

const open = ref(false)

const progress: Ref<boolean> = ref(false)

const edit: Ref<boolean> = ref(false)
const advancedMode: Ref<boolean> = ref(false)

if (isbn10Query.value !== null ||  
isbn13Query.value !== null || 
seriesQuery.value !== null || 
(tagsArrayString.value !== null && tagsArrayString.value.trim().length > 0) || 
(authorsArrayString.value !== null && authorsArrayString.value.trim().length > 0)) {
  advancedMode.value = true
}

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

const toggleEdit = (book: UserBook) => {
  edit.value = ! edit.value
  console.log("book")
  console.log(book)
  oruga.modal.open({
    // parent: this,
          component: EditBookModal,
          trapFocus: true,
          active: true,
          // fullScreen: false,
          canCancel: ['x', 'button', 'outside'],
          scroll: 'clip',
          props: {
            "book" : book,
            canAddEvent: true
          },
          onClose: modalClosed
        });
}

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
        <label class="label">Sort by : </label>
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
        <o-radio
          v-model="sortBy"
          native-value="publishedDate"
        >
          Publication date
        </o-radio>
      </div>
    </template>
    <template #filters>
      <div class="field">
        <label class="label">Books type : </label>
        <o-radio
          v-model="libraryFilter"
          native-value="ANY"
        >
          Any
        </o-radio>
        <o-radio
          v-model="libraryFilter"
          native-value="ONLY_USER_BOOKS"
        >
          Only books in my lists
        </o-radio>
        <o-radio
          v-model="libraryFilter"
          native-value="ONLY_NON_USER_BOOKS"
        >
          Only books not in my lists
        </o-radio>
      </div>
    </template>
  </sort-filter-bar-vue>
  <div class="flex flex-row sm:justify-between justify-center justify-items-center w-11/12">
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
    <o-checkbox v-model="advancedMode">
      Advanced search
    </o-checkbox>
    <div />
  </div>
  <div
    class="flex flex-row justify-center justify-items-center"
  >
    <div class="basis-full sm:basis-5/12">
      <div
        tabindex="0"
        class="collapse"
        :class="advancedMode ? 'collapse-open' : 'collapse-close'"
      > 
        <div class="collapse-title pr-5">
          <o-field>
            <o-input
              v-model="titleQuery"
              placeholder="Search title..." 
              type="search"
              icon="magnify" 
              icon-clickable
              icon-pack="mdi"
              class="input focus:input-accent"
              @keyup.enter="search"
            />
            <o-button
              v-tooltip="'Search selected query'"
              variant="success"
              icon-pack="mdi"
              icon-right="magnify"
              @click="search"
            />
          </o-field>
        </div>
        <div class="collapse-content">
          <div class="flex flex-wrap justify-center justify-items-center search-form-extended">
            <div class="search-form-extended-input">
              <o-input
                v-model="isbn10Query"
                placeholder="isbn10"
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
                placeholder="isbn13" 
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
                placeholder="authors" 
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
                placeholder="tags" 
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
                placeholder="series" 
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
    </div>
  </div>
  <div class="grid grid-cols-2 sm:grid-cols-8 gap-1 mt-4">
    <div
      v-for="book in convertedBooks"
      :key="book.book.id"
      class="m-1"
    >
      <router-link
        v-if="book.book.userBookId != undefined"
        :to="{ name: 'book-detail', params: { bookId: book.book.userBookId } }"
      >
        <book-card :book="book" />
      </router-link>
      <div v-else>
        <book-card
          v-tooltip="'This book is not yet in your books, double click to add it'"
          :book="book"
          @dblclick="toggleEdit(book)"
        >
          <template #icon>
            <span
              v-tooltip="'not in your books'"
              class="icon text-error"
            >
              <i class="mdi mdi-plus-circle mdi-18px" />
            </span>
          </template>
        </book-card>
      </div>
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

<style lang="scss" scoped>

</style>
