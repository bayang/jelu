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
const currentPageNumber: Ref<number> = ref(1)
const perPage: Ref<number> = ref(24)

const edit: Ref<Boolean> = ref(false)
const advancedMode: Ref<Boolean> = ref(false)
const selectedField: Ref<string|null> = ref(null)
const queryTerm: Ref<string> = ref("")
const query: Ref<Map<string, string>> = ref(new Map())

console.log("query")
console.log(props.query)

const search = () => {
    console.log(queryTerm.value)
    console.log(query.value)
    if (! advancedMode.value) {
      query.value.set('title', queryTerm.value)
    }
      dataService.findBooks(query.value.get('title'), 
      query.value.get('isbn10'), query.value.get('isbn13'), 
      currentPageNumber.value - 1, perPage.value)
    .then(res => {
        console.log(res)
          total.value = res.totalElements
          books.value = res.content
        if (! res.empty) {
          currentPageNumber.value = res.number + 1
        }
        else {
          currentPageNumber.value = 1
        }
    }
    )
}

watch(currentPageNumber, (newVal, oldVal) => {
  console.log(currentPageNumber.value)
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal) {
    search()
  }
})

watch(selectedField, (newVal, oldVal) => {
  console.log(selectedField.value)
  queryTerm.value = ""
})

watch(advancedMode, (newVal, oldVal) => {
  if (advancedMode.value) {
    // query.value.set('title', queryTerm.value)
  }
  else {
    if (query.value.has('title')) {
      let title = query.value.get('title')
      query.value.clear()
      if (StringUtils.isNotBlank(title)) {
        queryTerm.value = title!
      }
    }
  }
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
  search()
}

const addToQuery = () => {
  if (StringUtils.isNotBlank(selectedField.value) && StringUtils.isNotBlank(queryTerm.value)) {
    query.value.set(selectedField.value!, queryTerm.value)
  }
}

const removeFromQuery = (field: string) => {
  query.value.delete(field)
}

console.log('props query ' + props.query)
let queryTitle = props.query
if (StringUtils.isNotBlank(queryTitle)) {
  queryTerm.value = queryTitle!
  search()
}

</script>

<template>

<div class="columns is-centered">
  <div class="column is-4 field">
    <o-checkbox v-model="advancedMode">Advanced search</o-checkbox>
  </div>
</div>
<div class="columns is-centered">
  <div v-if="!advancedMode" class="column is-4">
  <o-field>
      <o-input placeholder="Search..." type="search" 
      icon="magnify" icon-clickable 
      iconPack="mdi"
      v-model="queryTerm"
      @keyup.enter="search"></o-input>
    </o-field>
    </div>
    <div v-else class="column is-8 is-offset-4">
  <o-field grouped>
    <o-field>
      <o-select
      placeholder="Fields"
      v-model="selectedField">
        <option value="title">Title</option>
        <option value="isbn10">Isbn10</option>
        <option value="isbn13">Isbn13</option>
      </o-select>
      <o-input type="text" v-model="queryTerm"></o-input>
      <o-button @click="addToQuery" variant="success">Add to query</o-button>
      </o-field>
      <o-button @click="search" variant="success" iconPack="mdi" iconRight="magnify"></o-button>
    </o-field>
    <p class="tags has-addons">
      <div v-for="[field, term] in query">
      <span class="tag is-success">{{field}}</span>
      <span class="tag is-white">{{term}}
        <button @click="removeFromQuery(field)" class="delete is-small"></button>
      </span>
      </div>
      </p>
    </div>
    </div>
    <p>{{query}}</p>
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

<style lang="scss" scoped>


</style>
