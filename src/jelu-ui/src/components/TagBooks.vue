<script setup lang="ts">
import { useProgrammatic } from "@oruga-ui/oruga-next";
import { useRouteQuery } from "@vueuse/router";
import { computed, Ref, ref, watch } from 'vue';
import usePagination from '../composables/pagination';
import useSort from "../composables/sort";
import { Book, UserBook } from '../model/Book';
import { LibraryFilter } from "../model/LibraryFilter";
import { Tag } from '../model/Tag';
import dataService from "../services/DataService";
import { ObjectUtils } from '../utils/ObjectUtils';
import BookCard from "./BookCard.vue";
import EditBookModal from "./EditBookModal.vue";
import SortFilterBarVue from "./SortFilterBar.vue";
import { useTitle } from '@vueuse/core'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useThrottleFn } from '@vueuse/core'

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const {oruga} = useProgrammatic();
const route = useRoute()

const tag: Ref<Tag> = ref({name: ""})
const tagBooks: Ref<Array<Book>> = ref([]);
const edit: Ref<boolean> = ref(false)

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('title,asc')

const libraryFilter: Ref<LibraryFilter> = useRouteQuery('libraryFilter', 'ANY' as LibraryFilter)

const open = ref(false)

const getBooksIsLoading: Ref<boolean> = ref(false)

watch([() => route.params.tagId, page, sortQuery, libraryFilter], (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetBooks()
  }
})

watch(() => route.params.tagId, (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal) {
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
      libraryFilter.value)
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

const convertedBooks = computed(() => tagBooks.value?.map(b => ObjectUtils.toUserBook(b)))

function modalClosed() {
  console.log("modal closed")
  getBooks()
}

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

getTag()
getBooks()

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
        <o-radio
          v-model="sortBy"
          native-value="modificationDate"
        >
          {{ t('sorting.modification_date') }}
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
  <div class="grid grid-cols-2 sm:grid-cols-8 gap-1 is-flex is-flex-wrap-wrap is-justify-content-space-evenly my-3">
    <div
      v-for="book in convertedBooks"
      :key="book.book.id"
      class="books-grid-item my-2"
    >
      <router-link
        v-if="book.book.userBookId != null"
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
