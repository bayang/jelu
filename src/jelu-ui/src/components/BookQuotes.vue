<script setup lang="ts">
import { useTitle } from '@vueuse/core';
import { Ref, ref, watch } from "vue";
import { useRoute } from 'vue-router';
import { Book } from "../model/Book";
import { BookQuote } from "../model/BookQuote";
import dataService from "../services/DataService";
import BookDataCard from "./BookDataCard.vue";
import BookQuoteCard from "./BookQuoteCard.vue";

const route = useRoute()

useTitle('Jelu | Quotes')
const quotes: Ref<Array<BookQuote>> = ref([])
const book: Ref<Book> = ref({title:""})

watch(() => route.params.bookId, (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal && route.params.bookId !== undefined) {
    getBook()
    getBookQuotes()
  }
})

const getBook = async () => {
  try {
    book.value = await dataService.findBookById(route.params.bookId as string)
  } catch (error) {
    console.log("failed get book : " + error);
  }
};

const getBookQuotes = async () => {
  dataService.findBookQuotes(undefined, 
    route.params.bookId as string, null, 0, 50, null)
    .then(res => {
        if (!res.empty) {
            quotes.value = res.content
        }
    })
    .catch(err => console.log(err))
};

getBook()
getBookQuotes()

</script>

<template>
  <section class="mb-6">
    <book-data-card
      :book="book"
      :owned="null"
      :to-read="null"
      :borrowed="null"
      :book-link="true"
      :links="true"
      :add-book="true"
    />
    <div class="w-full flex flex-row flex-wrap place-content-center gap-3 mt-4">
      <div
        v-for="quote in quotes"
        :key="quote.id"
        class="basis-11/12 sm:basis-2/3"
      >
        <book-quote-card
          v-if="quote != null"
          :book-quote="quote"
          :show-delete="false"
          :show-edit="false"
        />
      </div>
    </div>
  </section>
</template>

<style lang="scss">
</style>
