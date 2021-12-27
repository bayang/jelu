<script setup lang="ts">
import { computed, onMounted, Ref, ref } from 'vue'
import { useStore } from 'vuex'
import { UserBook } from '../model/Book'
import { key } from '../store'
import dataService from "../services/DataService";
import { ReadingEventType } from '../model/ReadingEvent';
import BookCard from "./BookCard.vue";

const store = useStore(key)

const isLogged = computed(() => {
    return store.state.isLogged
  })

const books: Ref<Array<UserBook>> = ref([]);

const getCurrentlyReading = async () => {
  try {
    books.value = await dataService.findUserBookByCriteria(ReadingEventType.CURRENTLY_READING, null)
  } catch (error) {
    console.log("failed get books : " + error);
  }
};

onMounted(() => {
  console.log("Component is mounted!");
    if (isLogged) {
  try {
      getCurrentlyReading()
  } catch (error) {
    console.log("failed get books : " + error);
  }
    }
});
</script>

<template>
<div v-if="isLogged">
  <div v-if="books.length > 0">
  <h2 class="title is-family-sans-serif">Currently reading : </h2>
  <div class="columns is-multiline is-centered">
      <div class="column is-2" v-for="book in books" v-bind:key="book.id">
      <router-link v-if="book.id != undefined" :to="{ name: 'book-detail', params: { bookId: book.id } }">
      <book-card :book="book"></book-card>
      </router-link>
    </div>
  </div>
  </div>
  <!-- logged, no books -->
  <div v-else>
    <h2 class="title is-family-sans-serif">Not currently reading anything </h2>
    <span class="icon is-large">
      <i class="mdi mdi-book-open-page-variant-outline mdi-48px"></i>
    </span>
  </div>
</div>
<!-- not logged -->
<div v-else>
  <p class="is-capitalized">Please log in first</p>
</div>

  
</template>

<style lang="scss" scoped>


</style>
