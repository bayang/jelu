<script setup lang="ts">
import { computed, onMounted, Ref, ref } from 'vue';
import { useProgrammatic } from "@oruga-ui/oruga-next";
import { useStore } from 'vuex';
import { Book, BookWithUserBook, UserBook } from '../model/Book';
import { TagWithBooks } from '../model/Tag';
import dataService from "../services/DataService";
import { key } from '../store';
import BookCard from "./BookCard.vue";
import EditBookModal from "./EditBookModal.vue"

const store = useStore(key)
const {oruga} = useProgrammatic();

const props = defineProps<{ tagId: string }>()

const tagBooks: Ref<TagWithBooks|null> = ref(null);
const edit: Ref<Boolean> = ref(false)

const getBooks = async () => {
  try {
    tagBooks.value = await dataService.getTagById(props.tagId)
  } catch (error) {
    console.log("failed get books for tag : " + error);
  }
};

onMounted(() => {
  console.log("Component is mounted!");
    
    }
);

const toUserBook = (book: BookWithUserBook):UserBook => {
  let {userBooks, ...rest} = book
    if (userBooks && userBooks.length > 0) {
      let converted =  {
        book: rest,
        ...userBooks[0]
      } as UserBook
      console.log('after full')
      console.log(converted)
      return converted
    }
    else {
      let converted =  {
        book: rest
      } as UserBook
      console.log('after')
      console.log(converted)
      return converted
    }
}

const convertedBooks = computed(() => tagBooks.value?.books.map(b => toUserBook(b)))

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

getBooks()

</script>

<template>
  <h2 class="title is-family-sans-serif">Books tagged {{tagBooks?.name}} : </h2>
  <div class="columns is-multiline is-centered">
      <div class="column is-2" v-for="book in convertedBooks" v-bind:key="book.id">
      <router-link v-if="book.id != undefined" :to="{ name: 'book-detail', params: { bookId: book.id } }">
        <book-card :book="book"></book-card>
      </router-link>
      <div v-else>
        <o-tooltip label="This book is not yet in your books, double click to add it" multiline>
        <book-card @dblclick="toggleEdit(book)" :book="book"></book-card>
        </o-tooltip>
      </div>
    </div>

  </div>
</template>

<style scoped>


</style>
