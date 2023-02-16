<script setup lang="ts">
import { ref, Ref } from "vue";
import { useI18n } from 'vue-i18n';
import { Book } from "../model/Book";
import { Review } from "../model/Review";
import dataService from "../services/DataService";

const { t, d } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const props = defineProps<{ 
  review: Review,
}>();

const book: Ref<Book|null> = ref(null)

const getBook = async () => {
  try {
    const res = await dataService.findBookById(props.review.book)
    book.value = res
  } catch (error) {
    console.log("failed get book : " + error)
  }
};

getBook()

</script>

<template>
  <div class="card card-side bg-base-100 shadow-2xl shadow-base-300 review-book-card">
    <figure
      v-if="book != null"
      class="place-self-start"
    >
      <img
        :src="'/files/' + book.image"
        alt="Movie"
      >
    </figure>
    <div class="card-body p-1 m-1">
      <h2
        v-if="book != null"
        class="card-title line-clamp-2"
      >
        {{ book?.title }}
      </h2>
      <span class="italic">{{ review.rating }}/10</span>
      <p class="line-clamp-4">
        {{ review.text.substring(0, 50) }}
      </p>
      <div class="card-actions justify-end">
        <router-link
          class="link hover:underline hover:decoration-4 hover:decoration-secondary text-sm italic"
          :to="{ name: 'review-detail', params: { reviewId: props.review.id } }"
        >
          {{ d(props.review.reviewDate, 'short') }}&nbsp;
        </router-link>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>

</style>
