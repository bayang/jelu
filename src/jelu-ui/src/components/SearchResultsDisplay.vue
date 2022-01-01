<script setup lang="ts">
import { computed, onMounted, Ref, ref, watch } from 'vue'
import { useProgrammatic } from "@oruga-ui/oruga-next";
import { useStore } from 'vuex'
import { BookWithUserBook, UserBook } from '../model/Book'
import { key } from '../store'
import dataService from "../services/DataService";
import BookCard from "./BookCard.vue";
import { StringUtils } from '../utils/StringUtils'
import { ObjectUtils } from '../utils/ObjectUtils';
import EditBookModal from "./EditBookModal.vue"

const store = useStore(key)
const {oruga} = useProgrammatic();
const props = defineProps<{ query: string|null }>()

const books: Ref<Array<BookWithUserBook>> = ref([]);
const total: Ref<number> = ref(0)
const current: Ref<number> = ref(1)
const perPage: Ref<number> = ref(2)

const edit: Ref<Boolean> = ref(false)

console.log("query")
console.log(props.query)

const search = (searchterm: string|null) => {
    console.log(searchterm)
  if (StringUtils.isNotBlank(searchterm)) {
      dataService.findBooks(searchterm!)
    .then(res => {
        console.log(res)
        if (! res.empty) {
            if (res.totalElements) {
                total.value = res.totalElements
            }
            if (res.number) {
                current.value = res.number
            }
            books.value = res.content
        }
    }
    )
  }
}

watch(current, (oldVal, newVal) => {
  console.log(current.value)
  
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
            "book" : book
          },
          onClose: modalClosed
        });
}

function modalClosed(args: any) {
  console.log("modal closed")
  search(props.query)
}

search(props.query)

</script>

<template>

<div class="columns is-centered">
  <div class="column is-4">
  <o-field>
      <o-input placeholder="Search..." type="search" 
      icon="magnify" icon-clickable 
      iconPack="mdi"
      @keyup.enter="search($event.target.value)"></o-input>
    </o-field>
    </div>
    </div>
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
    <o-pagination
      :total="total"
      v-model:current="current"
      order='centered'
      :per-page="perPage"
      aria-next-label="Next page"
      aria-previous-label="Previous page"
      aria-page-label="Page"
      aria-current-label="Current page"
    >
    </o-pagination>
  
</template>

<style lang="scss" scoped>


</style>
