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
    books.value = await dataService.findUserBooksByEventType(ReadingEventType.CURRENTLY_READING)
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
  <h2 class="title is-family-sans-serif">Your are currently reading : </h2>
  <div v-if="isLogged" class="columns is-multiline is-centered">
      <div class="column is-2" v-for="book in books" v-bind:key="book.id">
      <book-card :book="book"></book-card>
    </div>

  </div>
  
</template>

<style scoped>
a {
  color: #42b983;
}

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
