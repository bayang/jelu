<script setup lang="ts">
import { useProgrammatic } from "@oruga-ui/oruga-next";
import { useTitle } from '@vueuse/core';
import { useRouteQuery } from "@vueuse/router";
import { computed, onMounted, Ref, ref, watch } from 'vue';
import usePagination from '../composables/pagination';
import useSort from "../composables/sort";
import { Author } from "../model/Author";
import { Book, UserBook } from '../model/Book';
import { LibraryFilter } from "../model/LibraryFilter";
import dataService from "../services/DataService";
import { ObjectUtils } from '../utils/ObjectUtils';
import BookCard from "./BookCard.vue";
import EditAuthorModalVue from "./EditAuthorModal.vue";
import EditBookModal from "./EditBookModal.vue";
import SortFilterBarVue from "./SortFilterBar.vue";
import useDates from '../composables/dates'

const { formatDate, formatDateString } = useDates()

const {oruga} = useProgrammatic();

const props = defineProps<{ authorId: string }>()

const author: Ref<Author> = ref({name: ""})
const authorBooks: Ref<Array<Book>> = ref([]);
const edit: Ref<boolean> = ref(false)
const authorEdit: Ref<boolean> = ref(false)

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('title,asc')

const libraryFilter: Ref<LibraryFilter> = useRouteQuery('libraryFilter', 'ANY' as LibraryFilter)

const open = ref(false)

const getBooksIsLoading: Ref<boolean> = ref(false)

watch([page, sortQuery, libraryFilter], (newVal, oldVal) => {
  console.log(page.value)
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal) {
    getBooks()
  }
})

const getAuthor = async () => {
  try {
    author.value = await dataService.getAuthorById(props.authorId)
    useTitle('Jelu | ' + author.value.name)
  } catch (error) {
    console.log("failed get author : " + error);
  }
};

const getBooks = () => {
  getBooksIsLoading.value = true
  dataService.getAuthorBooksById(props.authorId, 
    pageAsNumber.value - 1, perPage.value, sortQuery.value, 
    libraryFilter.value)
    .then(res => {
        console.log(res)
          total.value = res.totalElements
          authorBooks.value = res.content
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

onMounted(() => {
  console.log("Component is mounted!");
    
    }
);

const convertedBooks = computed(() => authorBooks.value?.map(b => ObjectUtils.toUserBook(b)))

function modalClosed() {
  console.log("modal closed")
  getBooks()
}

function authorModalClosed() {
  console.log("author modal closed")
  getAuthor()
}

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

const editAuthor = () => {
  console.log(author.value)
  authorEdit.value = !authorEdit.value
  oruga.modal.open({
    parent: this,
    component: EditAuthorModalVue,
    trapFocus: true,
    active: true,
    // fullScreen: false,
    canCancel: ['x', 'button', 'outside'],
    scroll: 'clip',
    props: {
      "author": author.value
    },
    onClose: authorModalClosed
  });
}

getAuthor()
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
  <div class="grid columns is-multiline box">
    <div class="grid items-center justify-center justify-items-center justify-self-center sm:grid-cols-3 mb-4 sm:w-10/12 column is-full">
      <div />
      <div class="level-item">
        <h2 class="text-2xl inline mr-2 title has-text-weight-normal typewriter">
          {{ author.name }}
        </h2>
        <button
          class="btn btn-circle btn-xs bg-transparent border-0 hover:bg-accent/30"
          @click="editAuthor"
        >
          <i class="mdi mdi-24px mdi-pencil" />
        </button>
      </div>
      <div>
        <a
          v-if="author.officialPage"
          :href="author.officialPage"
          target="_blank"
          class="link link-accent"
        ><i class="mdi mdi-24px mdi-web" /></a>
        <a
          v-if="author.wikipediaPage"
          :href="author.wikipediaPage"
          target="_blank"
          class="link link-accent"
        ><i class="mdi mdi-24px mdi-wikipedia" /></a>
        <a
          v-if="author.goodreadsPage"
          :href="author.goodreadsPage"
          target="_blank"
          class="link link-accent"
        ><i class="mdi mdi-24px mdi-goodreads" /></a>
        <a
          v-if="author.twitterPage"
          :href="author.twitterPage"
          target="_blank"
          class="link link-accent"
        ><i class="mdi mdi-24px mdi-twitter" /></a>
        <a
          v-if="author.facebookPage"
          :href="author.facebookPage"
          target="_blank"
          class="link link-accent"
        ><i class="mdi mdi-24px mdi-facebook" /></a>
        <a
          v-if="author.instagramPage"
          :href="author.instagramPage"
          target="_blank"
          class="link link-accent"
        ><i class="mdi mdi-24px mdi-instagram" /></a>
      </div>
    </div>
    <div class="grid grid-cols-1 sm:grid-cols-2 sm:gap-10 column is-one-fifth is-offset-one-fifth">
      <div class="justify-self-center sm:justify-self-end">
        <figure class="image">
          <img
            v-if="author.image"
            :src="'/files/' + author.image"
            alt="cover image"
            class="max-h-80"
          >
          <img
            v-else
            src="../assets/placeholer_author.jpg"
            alt="cover placeholder"
          >
        </figure>
      </div>
    
      <div class="column is-three-fifths content text-left w-11/12 sm:w-full justify-self-center sm:justify-self-start">
        <p
          v-if=" author.biography != null"
          class="has-text-left"
        >
          <span class="font-semibold">Biography :</span>
        </p>
        <p
          class="has-text-left prose-base"
          v-html="author.biography"
        />
        <p
          v-if="author.dateOfBirth"
          class="has-text-left block"
        >
          <span class="font-semibold">Birth :</span>
          {{ formatDate(author.dateOfBirth) }}
        </p>
        <p
          v-if="author.dateOfDeath"
          class="has-text-left block"
        >
          <span class="font-semibold">Death :</span>
          {{ formatDate(author.dateOfDeath) }}
        </p>
        <p
          v-if=" author.notes != null"
          class="has-text-left"
        >
          <span class="font-semibold">Additional notes :</span>
        </p>
        <p
          class="has-text-left prose-base"
          v-html="author.notes"
        />
      </div>
    </div>
  </div>
  <div class="flex flex-row justify-between mt-4">
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
    <h2 class="text-xl typewriter">
      <span class="icon">
        <i class="mdi mdi-bookshelf" />
      </span>
      Books from {{ author.name }} :
    </h2>
    <div />
  </div>
  <div class="grid grid-cols-2 sm:grid-cols-8 gap-2 justify-center justify-items-center justify-self-center">
    <div
      v-for="book in convertedBooks"
      :key="book.book.id"
      class="my-2"
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
          v-tooltip="'This book is not yet in your books, double click to add it'"
          :book="book"
          class="h-full"
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
  <div
    v-if="getBooksIsLoading && authorBooks.length < 1"
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
