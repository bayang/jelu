<script setup lang="ts">
import { computed, onMounted, Ref, ref, watch } from 'vue';
import { useProgrammatic } from "@oruga-ui/oruga-next";
import { useStore } from 'vuex';
import { Book, BookWithUserBook, UserBook } from '../model/Book';
import { Tag, TagWithBooks } from '../model/Tag';
import dataService from "../services/DataService";
import { key } from '../store';
import BookCard from "./BookCard.vue";
import EditBookModal from "./EditBookModal.vue"
import { ObjectUtils } from '../utils/ObjectUtils';

const store = useStore(key)
const {oruga} = useProgrammatic();

const props = defineProps<{ tagId: string }>()

const total: Ref<number> = ref(0)
const currentPageNumber: Ref<number> = ref(1)
const perPage: Ref<number> = ref(24)

const tag: Ref<Tag> = ref({name: ""})
const tagBooks: Ref<Array<BookWithUserBook>> = ref([]);
const edit: Ref<Boolean> = ref(false)

watch(currentPageNumber, (newVal, oldVal) => {
  console.log(currentPageNumber.value)
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal) {
    getBooks()
  }
})

const getTag = async () => {
  try {
    tag.value = await dataService.getTagById(props.tagId)
  } catch (error) {
    console.log("failed get tag : " + error);
  }
};

const getBooks = () => {
  dataService.getTagBooksById(props.tagId, 
    currentPageNumber.value - 1, perPage.value)
    .then(res => {
        console.log(res)
          total.value = res.totalElements
          tagBooks.value = res.content
        if (! res.empty) {
          currentPageNumber.value = res.number + 1
        }
        else {
          currentPageNumber.value = 1
        }
    }
    )
  
};

onMounted(() => {
  console.log("Component is mounted!");
    
    }
);

const convertedBooks = computed(() => tagBooks.value?.map(b => ObjectUtils.toUserBook(b)))

function modalClosed(args: any) {
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
            "book" : book
          },
          onClose: modalClosed
        });
}

getTag()
getBooks()

</script>

<template>
  <h2 class="title has-text-weight-normal typewriter">Books tagged #{{tag.name}} : </h2>
  <div class="columns is-multiline is-variable is-4 is-centered">
      <div class="column is-2" v-for="book in convertedBooks" v-bind:key="book.id">
      <router-link v-if="book.id != undefined" :to="{ name: 'book-detail', params: { bookId: book.id } }">
        <book-card :book="book"></book-card>
      </router-link>
      <div v-else>
        <book-card @dblclick="toggleEdit(book)" :book="book"
        v-tooltip="'This book is not yet in your books, double click to add it'"></book-card>
      </div>
    </div>

  </div>
  <o-pagination
      :total="total"
      v-model:current="currentPageNumber"
      order='centered'
      :per-page="perPage"
      aria-next-label="Next page"
      aria-previous-label="Previous page"
      aria-page-label="Page"
      aria-current-label="Current page"
    >
    </o-pagination>
</template>

<style scoped>


</style>
