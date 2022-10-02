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

const review: Ref<Review|null> = ref(null)
const book: Ref<Book|null> = ref(null)

watch(() => route.params.reviewId, (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal && route.params.reviewId !== undefined) {
    getReview()
  }
})

const getReview = async () => {
  try {
    review.value = await dataService.findReviewById(route.params.reviewId as string)
    book.value = await dataService.findBookById(review.value.book)
    useTitle('Jelu | Review')
  } catch (error) {
    console.log("failed get review : " + error);
  }
};

getReview()
</script>

<template>
  <section class="">
    <div class="w-full flex flex-wrap justify-center">
      <book-data-card
        v-if="book != null"
        :book="book"
        :owned="null"
        :to-read="null"
        :borrowed="null"
        :book-link="false"
        :links="false"
        :add-book="false"
        class="basis-full"
      />
      <review-card
        v-if="review != null"
        :review="review"
        :show-delete="false"
        :show-edit="false"
        class="basis-5/12 my-6"
      />
    </div>
  </section>
</template>

<style lang="scss">
</style>
