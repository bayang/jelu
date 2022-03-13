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

const { total, page, pageAsNumber, perPage, updatePage } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('title,asc')

const libraryFilter: Ref<LibraryFilter> = useRouteQuery('libraryFilter', 'ANY' as LibraryFilter)

const open = ref(false)

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
    }
    )
  
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
  <div class="columns is-multiline box">
    <div class="column is-full">
      <h2 class="title has-text-weight-normal typewriter">
        {{ author.name }}
        <span
          class="icon is-medium has-text-light"
          @click="editAuthor"
        >
          <i class="background-on-hover mdi mdi-24px mdi-pencil" />
        </span>
      </h2>
    </div>
    <div class="column is-one-fifth is-offset-one-fifth">
      <figure class="image">
        <img
          v-if="author.image"
          :src="'/files/' + author.image"
          alt="cover image"
        >
        <img
          v-else
          src="../assets/placeholder_asset.png"
          alt="cover placeholder"
        >
      </figure>
    </div>
    <div class="column is-three-fifths content">
      <p
        v-if=" author.biography != null"
        class="has-text-left"
      >
        <span class="has-text-weight-semibold">Biography :</span>
      </p><p v-html="author.biography" />
      <p
        v-if="author.dateOfBirth"
        class="has-text-left block"
      >
        <span class="has-text-weight-semibold">Birth :</span>
        {{ formatDate(author.dateOfBirth) }}
      </p>
      <p
        v-if="author.dateOfDeath"
        class="has-text-left block"
      >
        <span class="has-text-weight-semibold">Death :</span>
        {{ formatDate(author.dateOfDeath) }}
      </p>
    </div>
  </div>
  <div class="level">
    <div class="level-left mobile-level-right">
      <div class="level-item">
        <o-button
          variant="primary"
          outlined
          @click="open = !open"
        >
          <span class="icon">
            <i class="mdi mdi-filter-variant" />
          </span>
        </o-button>
      </div>
    </div>
    <div class="level-item">
      <h4 class="subtitle has-text-weight-normal typewriter is-4">
        Books from {{ author.name }} :
      </h4>
    </div>
  </div>
  <div class="is-flex is-flex-wrap-wrap is-justify-content-space-evenly">
    <div
      v-for="book in convertedBooks"
      :key="book.book.id"
      class="books-grid-item my-2"
    >
      <router-link
        v-if="book.book.userBookId != null"
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
            <o-tooltip
              label="not in your books"
              variant="danger"
            >
              <span class="icon has-text-danger">
                <i class="mdi mdi-plus-circle mdi-18px" />
              </span>
            </o-tooltip>
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
</template>

<style lang="scss" scoped>

.columns {
  margin-top: 10px;
}


</style>
