<script setup lang="ts">
import { onMounted, Ref, ref } from "vue";
import dataService from "../services/DataService";
import { UserBook } from "../model/Book";
import BookCard from "./BookCard.vue";

const books: Ref<Array<UserBook>> = ref([]);

const getBooks = async () => {
  try {
    books.value = await dataService.findUserBooks();
  } catch (error) {
    console.log("failed get def books : " + error);
  }
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
          <i class="mdi mdi-bookshelf"></i> </span
        >&nbsp; My books :
      </h2>
    </div>
    <div class="level-right">
      <p class="level-item">
        <router-link :to="{ name: 'add-book' }" class="button is-primary">
          Add book
        </router-link>
      </p>
    </div>
  </div>
  <div class="columns is-variable is-2 is-multiline is-centered">
    <div class="column is-2" v-for="book in books" v-bind:key="book.id">
      <book-card :book="book"></book-card>
    </div>
  </div>
</template>

<style scoped>
/* a {
  color: #42b983;
} */

label {
  margin: 0 0.5em;
  font-weight: bold;
}

code {
  background-color: #eee;
  padding: 2px 4px;
  border-radius: 4px;
  color: #304455;
}
</style>
