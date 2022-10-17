<script setup lang="ts">
import { useTitle } from '@vueuse/core';
import { Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import { Book } from "../model/Book";
import { Review } from "../model/Review";
import dataService from "../services/DataService";
import BookDataCard from "./BookDataCard.vue";
import ReviewCard from "./ReviewCard.vue";

const route = useRoute()

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | Reviews')
const reviews: Ref<Array<Review>> = ref([])
const book: Ref<Book> = ref({title:""})

watch(() => route.params.bookId, (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal && route.params.bookId !== undefined) {
    getBook()
    getReviews()
  }
})

const getBook = async () => {
  try {
    book.value = await dataService.findBookById(route.params.bookId as string)
  } catch (error) {
    console.log("failed get book : " + error);
  }
};

const getReviews = async () => {
  dataService.findReviews(undefined, 
    route.params.bookId as string, null, null, null, 0, 50, null)
    .then(res => {
        if (!res.empty) {
            reviews.value = res.content
        }
    })
    .catch(err => console.log(err))
};

getBook()
getReviews()

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
        v-for="review in reviews"
        :key="review.id"
        class="basis-11/12 sm:basis-2/3"
      >
        <review-card
          v-if="review != null"
          :review="review"
          :show-delete="false"
          :show-edit="false"
        />
      </div>
    </div>
  </section>
</template>

<style lang="scss">
</style>
