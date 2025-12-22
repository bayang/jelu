<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import { Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import usePagination from '../composables/pagination';
import useSort from "../composables/sort";
import { Review } from '../model/Review';
import dataService from "../services/DataService";
import ReviewBookCard from './ReviewBookCard.vue';

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | ' + t('nav.activity'))

const reviews: Ref<Array<Review>> = ref([]);

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination(16)

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('reviewDate,desc')

const getBookIsLoading: Ref<boolean> = ref(false)

watch([page, sortQuery], (newVal, oldVal) => {
  console.log("all " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetReviews()
  }
})

const getReviews = () => {
  getBookIsLoading.value = true
  dataService.findReviews(undefined, undefined, null, 
  null, null,
  pageAsNumber.value - 1, perPage.value, sortQuery.value)
  .then(res => {
        console.log(res)
          total.value = res.totalElements
          reviews.value = res.content
        if (! res.empty) {
          page.value =  (res.number + 1).toString(10)
        }
        else {
          page.value = "1"
        }
        getBookIsLoading.value = false
        updatePageLoading(false)
    }
    )
    .catch(e => {
      getBookIsLoading.value = false
      updatePageLoading(false)
    })
  
};

// watches set above sometimes called twice
// so getBooks was sometimes called twice at the same instant
const throttledGetReviews = useThrottleFn(() => {
  getReviews()
}, 100, false)

try {
  getReviews();
} catch (error) {
  console.log("failed get reviews : " + error);
}

</script>

<template>
  <div class="flex flex-row mb-2 justify-center">
    <h2 class="text-3xl typewriter capitalize">
      <span class="icon">
        <i class="mdi mdi-bookshelf" />
      </span>
      &nbsp; {{ t('nav.activity') }} :
    </h2>
  </div>
  <o-pagination
    v-if="reviews.length > 0 && pageCount > 1"
    :current="pageAsNumber"
    :total="total"
    order="centered"
    :per-page="perPage"
    @change="updatePage"
  />
  <div
    v-if="reviews.length > 0"
    class="flex flex-wrap gap-3 justify-center"
  >
    <TransitionGroup name="list">
      <div
        v-for="review in reviews"
        :key="review.id"
        class="m-1"
      >
        <ReviewBookCard
          :review="review"
          :book-reviews-link="true"
          :show-user-name="true"
        />
      </div>
    </TransitionGroup>
  </div>
  <div
    v-else-if="getBookIsLoading"
    class="flex flex-row justify-center justify-items-center gap-3"
  >
    <o-skeleton
      class="justify-self-center basis-36"
      height="250px"
      :animated="true"
    />
    <o-skeleton
      class="justify-self-center basis-36"
      height="250px"
      :animated="true"
    />
    <o-skeleton
      class="justify-self-center basis-36"
      height="250px"
      :animated="true"
    />
  </div>
  <div v-else>
    <h2 class="text-3xl typewriter capitalize">
      {{ t('labels.library_empty') }}
    </h2>
    <span class="icon">
      <i class="mdi mdi-book-open-page-variant-outline mdi-48px" />
    </span>
  </div>

  <o-pagination
    v-if="reviews.length > 0"
    :current="pageAsNumber"
    :total="total"
    order="centered"
    :per-page="perPage"
    @change="updatePage"
  />
  <o-loading
    v-model:active="getPageIsLoading"
    :full-page="true"
    :cancelable="true"
  />
</template>

<style scoped>

label.label {
  font-weight: bold;
}

.list-enter-active,
.list-leave-active {
  transition: all 0.2s ease;
}
.list-enter-from,
.list-leave-to {
  opacity: 0;
}

</style>
