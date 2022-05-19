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
import { useI18n } from 'vue-i18n'

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

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
          component: EditBookModal,
          trapFocus: true,
          active: true,
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
  <div class="grid grid-cols-2 sm:grid-cols-8 gap-1 my-4">
    <div
      v-for="book in convertedBooks"
      :key="book.book.id"
      class="m-1"
    >
      <router-link
        v-if="book.book.userBookId != undefined"
        :to="{ name: 'book-detail', params: { bookId: book.book.userBookId } }"
      >
        <book-card
          :book="book"
          class="h-full"
        />
      </router-link>
      <div v-else>
        <book-card
          v-tooltip="t('labels.book_not_yet_in_books')"
          :book="book"
          class="h-full"
          @dblclick="toggleEdit(book)"
        >
          <template #icon>
            <span
              v-tooltip="t('labels.not_in_your_books')"
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
