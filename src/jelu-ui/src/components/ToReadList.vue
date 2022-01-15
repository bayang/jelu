<script setup lang="ts">
import { onMounted, Ref, ref, watch } from 'vue'
import { UserBook } from '../model/Book'
import dataService from "../services/DataService";
import BookCard from "./BookCard.vue";

const books: Ref<Array<UserBook>> = ref([]);

const total: Ref<number> = ref(0)
const currentPageNumber: Ref<number> = ref(1)
const perPage: Ref<number> = ref(24)

const getToRead = async () => {
  try {
    const res = await dataService.findUserBookByCriteria(null, true, 
      currentPageNumber.value - 1, perPage.value)
    total.value = res.totalElements
    books.value = res.content
    if (! res.empty) {
      currentPageNumber.value = res.number + 1
    }
    else {
      currentPageNumber.value = 1
    }
  } catch (error) {
    console.log("failed get books : " + error);
  }
};

watch(currentPageNumber, (newVal, oldVal) => {
  console.log(currentPageNumber.value)
  if (newVal !== oldVal) {
    getToRead()
  }
})

onMounted(() => {
  console.log("Component is mounted!");
    
    }
);

getToRead()

</script>

<template>
  <h2 class="title has-text-weight-normal typewriter">
    To Read List :
  </h2>
  <div
    v-if="books.length > 0"
    class="is-flex is-flex-wrap-wrap is-justify-content-center"
  >
    <div
      v-for="book in books"
      :key="book.id"
      class="books-grid-item m-2"
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
      Nothing to read
    </h2>
    <span class="icon is-large">
      <i class="mdi mdi-book-open-page-variant-outline mdi-48px" />
    </span>
  </div>
  <o-pagination
    v-if="books.length > 0"
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

<style scoped>


</style>
