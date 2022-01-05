<script setup lang="ts">
import { onMounted, Ref, ref, watch } from "vue";
import dataService from "../services/DataService";
import { UserBook } from "../model/Book";
import BookCard from "./BookCard.vue";

const books: Ref<Array<UserBook>> = ref([]);

const total: Ref<number> = ref(0)
const currentPageNumber: Ref<number> = ref(1)
const perPage: Ref<number> = ref(24)

watch(currentPageNumber, (newVal, oldVal) => {
  console.log(currentPageNumber.value)
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal) {
    getBooks()
  }
})

const getBooks = () => {
  dataService.findUserBooks(currentPageNumber.value - 1, perPage.value)
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
  
};

onMounted(() => {
  console.log("Component is mounted!");
  try {
    getBooks();
  } catch (error) {
    console.log("failed get books : " + error);
  }
});
</script>

<template>
  <div class="level">
    <div class="level-item">
      <h2 class="title">
        <span class="icon">
          <i class="mdi mdi-bookshelf"></i>
        </span>
          &nbsp; My books :
      </h2>
    </div>
  </div>
  <div v-if="books.length > 0" class="columns is-variable is-2 is-multiline is-centered">
    <div class="column is-2" v-for="book in books" v-bind:key="book.id">
    <router-link v-if="book.id != undefined" :to="{ name: 'book-detail', params: { bookId: book.id } }">
      <book-card :book="book"></book-card>
      </router-link>
    </div>
  </div>
  <div v-else>
<h2 class="title is-family-sans-serif">Library is empty </h2>
    <span class="icon is-large">
      <i class="mdi mdi-book-open-page-variant-outline mdi-48px"></i>
    </span>

  </div>

<o-pagination
      v-if="books.length > 0"
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

label {
  margin: 0 0.5em;
  font-weight: bold;
}

</style>
