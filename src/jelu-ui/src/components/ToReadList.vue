<script setup lang="ts">
import { onMounted, Ref, ref, watch } from 'vue'
import { UserBook } from '../model/Book'
import dataService from "../services/DataService";
import BookCard from "./BookCard.vue";
import usePagination from '../composables/pagination';

const books: Ref<Array<UserBook>> = ref([]);

const { total, page, pageAsNumber, perPage, updatePage } = usePagination()

const getToRead = async () => {
  try {
    const res = await dataService.findUserBookByCriteria(null, true, 
      Number.parseInt(page.value) - 1, perPage.value)
    total.value = res.totalElements
    books.value = res.content
    if (! res.empty) {
      page.value =  (res.number + 1).toString(10)
    }
    else {
      page.value = "1"
    }
  } catch (error) {
    console.log("failed get books : " + error);
  }
};

watch(page, (newVal, oldVal) => {
  console.log(page.value)
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
    v-model:current="pageAsNumber"
    :total="total"
    order="centered"
    :per-page="perPage"
    @change="updatePage"
  />
</template>

<style scoped>


</style>
