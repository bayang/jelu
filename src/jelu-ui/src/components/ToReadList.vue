<script setup lang="ts">
import { computed, onMounted, Ref, ref } from 'vue'
import { useStore } from 'vuex'
import { UserBook } from '../model/Book'
import { key } from '../store'
import dataService from "../services/DataService";
import { ReadingEventType } from '../model/ReadingEvent';
import BookCard from "./BookCard.vue";

const store = useStore(key)

const books: Ref<Array<UserBook>> = ref([]);

const getToRead = async () => {
  try {
    books.value = await dataService.findUserBookByCriteria(null, true)
  } catch (error) {
    console.log("failed get books : " + error);
  }
};

onMounted(() => {
  console.log("Component is mounted!");
    
    }
);

getToRead()

</script>

<template>
  <h2 class="title has-text-weight-normal typewriter">To Read List : </h2>
  <div v-if="books.length > 0" class="is-flex is-flex-wrap-wrap is-justify-content-space-evenly">
    <div class="books-grid-item my-2" v-for="book in books" v-bind:key="book.id">
    <router-link v-if="book.id != undefined" :to="{ name: 'book-detail', params: { bookId: book.id } }">
      <book-card :book="book"></book-card>
      </router-link>
    </div>
  </div>
  <div v-else>
<h2 class="title has-text-weight-normal typewriter">Nothing to read </h2>
    <span class="icon is-large">
      <i class="mdi mdi-book-open-page-variant-outline mdi-48px"></i>
    </span>
  </div>
  
</template>

<style scoped>


</style>
