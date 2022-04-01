<script setup lang="ts">
import { useProgrammatic } from "@oruga-ui/oruga-next";
import { useRouteQuery } from "@vueuse/router";
import { computed, onMounted, Ref, ref, watch } from 'vue';
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


const {oruga} = useProgrammatic();

const props = defineProps<{ tagId: string }>()

const tag: Ref<Tag> = ref({name: ""})
const tagBooks: Ref<Array<Book>> = ref([]);
const edit: Ref<boolean> = ref(false)

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

const getTag = async () => {
  try {
    tag.value = await dataService.getTagById(props.tagId)
    useTitle('Jelu | #' + tag.value.name)
  } catch (error) {
    console.log("failed get tag : " + error);
  }
};

const getBooks = () => {
  dataService.getTagBooksById(props.tagId, 
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
    }
    )
  
};

onMounted(() => {
  console.log("Component is mounted!");
    
    }
);

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
      Books tagged #{{ tag.name }} :
    </h2>
    <div />
  </div>
  <div class="grid grid-cols-2 sm:grid-cols-8 gap-1 is-flex is-flex-wrap-wrap is-justify-content-space-evenly">
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
</template>

<style scoped>


</style>
