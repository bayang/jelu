<script setup lang="ts">
import { computed, onMounted, Ref, ref } from 'vue'
import { useStore } from 'vuex'
import { useLink, useRoute, useRouter } from 'vue-router'
import { UserBook } from '../model/Book'
import { key } from '../store'
import dataService from "../services/DataService";
import { ReadingEventType } from '../model/ReadingEvent';
import BookCard from "./BookCard.vue";
import { StringUtils } from '../utils/StringUtils'

const store = useStore(key)
const router = useRouter()

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

const visibleAdvanced: Ref<boolean> = ref(false);

const hideAdvanced = () => {
  setTimeout(() => visibleAdvanced.value = false, 500)
}

const search = (searchterm: string) => {
  console.log(searchterm)
  if (StringUtils.isNotBlank(searchterm)) {
    router.push({ path: '/search', query: { title: searchterm } })
  }
}

</script>

<template>
<div v-if="isLogged">
<div class="columns is-centered">
  <div class="column is-4">
  <o-field>
      <o-input placeholder="Search..." type="search" 
      icon="magnify" icon-clickable 
      iconPack="mdi"
      @focus="visibleAdvanced = true"
      @blur="hideAdvanced"
      @keyup.enter="search($event.target.value)"></o-input>
    </o-field>
    <router-link
          v-if="visibleAdvanced"
            class="is-family-sans-serif is-size-7"
            :to="{ name: 'search' }"
          >Advanced search</router-link>
    </div>
    </div>
  <div v-if="books.length > 0">
  <h2 class="title has-text-weight-normal typewriter">Currently reading : </h2>
  <div class="columns is-multiline is-variable is-4 is-centered">
      <div class="column is-2 is-8-mobile is-offset-2-mobile" v-for="book in books" v-bind:key="book.id">
      <router-link v-if="book.id != undefined" :to="{ name: 'book-detail', params: { bookId: book.id } }">
      <book-card :book="book"></book-card>
      </router-link>
    </div>
  </div>
  </div>
  <!-- logged, no books -->
  <div v-else>
    <h2 class="title has-text-weight-normal typewriter">Not currently reading anything </h2>
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
