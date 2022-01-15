<script setup lang="ts">
import { computed, Ref, ref, watch } from 'vue'
import { useProgrammatic } from "@oruga-ui/oruga-next";
import { BookWithUserBook, UserBook } from '../model/Book'
import dataService from "../services/DataService";
import BookCard from "./BookCard.vue";
import { StringUtils } from '../utils/StringUtils'
import { ObjectUtils } from '../utils/ObjectUtils';
import EditBookModal from "./EditBookModal.vue"

const {oruga} = useProgrammatic();
const props = defineProps<{ query: string|null }>()

const books: Ref<Array<BookWithUserBook>> = ref([]);
const total: Ref<number> = ref(0)
const currentPageNumber: Ref<number> = ref(1)
const perPage: Ref<number> = ref(24)

const edit: Ref<boolean> = ref(false)
const advancedMode: Ref<boolean> = ref(false)
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
      query.value.get('series'), 
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
      if (title != null && StringUtils.isNotBlank(title)) {
        queryTerm.value = title
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

function modalClosed() {
  console.log("modal closed")
  search()
}

const addToQuery = () => {
  if (selectedField.value != null && StringUtils.isNotBlank(selectedField.value) && StringUtils.isNotBlank(queryTerm.value)) {
    query.value.set(selectedField.value, queryTerm.value)
  }
}

const removeFromQuery = (field: string) => {
  query.value.delete(field)
}

console.log('props query ' + props.query)

if (props.query != null && StringUtils.isNotBlank(props.query)) {
  queryTerm.value = props.query?.slice()
  search()
}

</script>

<template>
  <div class="columns is-centered">
    <div class="column is-4 field">
      <o-checkbox v-model="advancedMode">
        Advanced search
      </o-checkbox>
    </div>
  </div>
  <div
    v-if="!advancedMode"
    class="columns is-centered is-multiline"
  >
    <div class="column is-4">
      <o-field>
        <o-input
          v-model="queryTerm"
          placeholder="Search..." 
          type="search"
          icon="magnify" 
          icon-clickable
          icon-pack="mdi"
          @keyup.enter="search"
        />
      </o-field>
    </div>
  </div>
  <div
    v-else
    class="columns is-centered is-multiline"
  >
    <div class="column is-8 is-offset-4-desktop">
      <o-field
        group-multiline
        class="tablet-up"
      >
        <o-select
          v-model="selectedField"
          placeholder="Fields"
        >
          <option value="title">
            Title
          </option>
          <option value="isbn10">
            Isbn10
          </option>
          <option value="isbn13">
            Isbn13
          </option>
          <option value="series">
            Series
          </option>
        </o-select>
        <o-input
          v-model="queryTerm"
          type="text"
        />
        <o-button
          v-tooltip="'Add to query params'"
          variant="warning"
          icon-pack="mdi"
          icon-right="magnify-plus-outline"
          @click="addToQuery"
        />
        <o-button
          v-tooltip="'Search selected query params'"
          variant="success"
          icon-pack="mdi"
          icon-right="magnify"
          @click="search"
        />
      </o-field>
    </div>
    <div class="column is-full is-offset-8-desktop">
      <div class="tags has-addons">
        <div
          v-for="[field, term] in query"
          :key="field"
        >
          <span class="tag is-success">{{ field }}</span>
          <span class="tag is-white">{{ term }}
            <button
              class="delete is-small"
              @click="removeFromQuery(field)"
            />
          </span>
        </div>
      </div>
    </div>
  </div>
  <div class="is-flex is-flex-wrap-wrap is-justify-content-space-evenly">
    <div
      v-for="book in convertedBooks"
      :key="book.id"
      class="books-grid-item my-2"
    >
      <router-link
        v-if="book.id != undefined"
        :to="{ name: 'book-detail', params: { bookId: book.id } }"
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
    v-model:current="currentPageNumber"
    :total="total"
    order="centered"
    :per-page="perPage"
    aria-next-label="Next page"
    aria-previous-label="Previous page"
    aria-page-label="Page"
    aria-current-label="Current page"
  />
</template>

<style lang="scss" scoped>


</style>
