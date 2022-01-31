<script setup lang="ts">
import { onMounted, Ref, ref, watch } from "vue";
import dataService from "../services/DataService";
import { UserBook } from "../model/Book";
import BookCard from "./BookCard.vue";
import usePagination from '../composables/pagination';

const books: Ref<Array<UserBook>> = ref([]);

const { total, page, pageAsNumber, perPage, updatePage } = usePagination()

watch(page, (newVal, oldVal) => {
  console.log(page.value)
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal) {
    getBooks()
  }
})

const getBooks = () => {
  dataService.findUserBooks(Number.parseInt(page.value) - 1, perPage.value)
  .then(res => {
        console.log(res)
          total.value = res.totalElements
          books.value = res.content
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
      <h2 class="title has-text-weight-normal typewriter">
        <span class="icon">
          <i class="mdi mdi-bookshelf" />
        </span>
        &nbsp; My books :
      </h2>
    </div>
  </div>
  <div
    v-if="books.length > 0"
    class="is-flex is-flex-wrap-wrap is-justify-content-center"
  >
    <div
      v-for="book in books"
      :key="book.id"
      class="books-grid-item m-1"
    >
      <router-link
        v-if="book.id != undefined"
        :to="{ name: 'book-detail', params: { bookId: book.id } }"
      >
        <book-card :book="book" />
      </router-link>
    </div>
  </div>
  <div v-else>
    <h2 class="title has-text-weight-normal typewriter">
      Library is empty
    </h2>
    <span class="icon is-large">
      <i class="mdi mdi-book-open-page-variant-outline mdi-48px" />
    </span>
  </div>

  <o-pagination
    v-if="books.length > 0"
    v-model:current="pageAsNumber"
    :total="total"
    order="centered"
    :per-page="perPage"
    @change="updatePage"
  />
</template>

<style scoped>

label {
  margin: 0 0.5em;
  font-weight: bold;
}

</style>
