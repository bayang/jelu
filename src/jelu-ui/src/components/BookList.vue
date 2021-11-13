<script setup lang="ts">
import { onMounted, Ref, ref } from 'vue'
import DataService from '../services/DataService'
import Book from "../model/Book";
console.log("script setup")
const count = ref(0)
const books: Ref<Array<Book>> = ref([])
const getBooks = async () => {
    try {
      books.value = await DataService.findAll()
    } catch (error) {
      console.log("failed get def books : " +error)
    }
  }
  onMounted(() => {
            console.log('Component is mounted!')
            try {
              getBooks()
            } catch (error) {
              console.log("failed get books : " +error)
            }
        })
</script>
<script lang="ts">

import { defineComponent, onBeforeMount } from 'vue'
// export default {
//     setup() {
//     // mounted
//     onMounted(() => {
//       console.log('Component is mounted!')
//     }),
//     onBeforeMount(() => {
//       console.log('Component before mount!')
//     })
//   }
// }

console.log("setup")
// const Component = defineComponent({
//   setup() {
//       console.log("setup")
//     const year = ref(2020)
//     onMounted(() => {
//       console.log('Component is mounted!')
//       getBooks()
//     })
//   }
// })

export default {
    setup() {
      const readersNumber = ref(0)
        console.log("in setup")
        onMounted(() => {
            console.log('Component is mounted!')
            getBooks()
        })
        // getBooks()
      // expose to template
      return {
        readersNumber,
      }
    }
  }

</script>

<template>
  <h1 class="title is-1"><span class="icon">
  <i class="mdi mdi-bookshelf"></i>
</span>&nbsp; Books :</h1>
    <div class="columns is-variable is-2 is-multiline is-centered">
    <div class="column is-2" v-for="book in books" v-bind:key="book.id">
      <div class="card">
  <div class="card-image">
    <figure class="image is-3by4">
      <img src="http://lorempixel.com/g/400/200" alt="Placeholder image">
    </figure>
  </div>
  <header class="card-header">
    <p class="card-header-title has-text-primary is-capitalized">
      <span class="icon has-text-success">
  <i class="mdi mdi-book-open-blank-variant mdi-18px"></i>
</span>{{book.title}}
    </p>
    
    <button class="card-header-icon" aria-label="more options">
      <span class="icon">
        <i class="fas fa-angle-down" aria-hidden="true"></i>
      </span>
    </button>
  </header>
  <div class="card-content">
    <div class="content">
      {{book.id}}
    </div>
    <footer class="card-footer">
    <div class="tags has-addons">
  <span class="tag">Status</span>
  <span class="tag is-primary">FINISHED</span>
</div>
  </footer>
  </div>
</div>
      <!-- {{ book.title }} - {{ book.id }} -->
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
