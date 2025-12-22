<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import { useRouteQuery } from '@vueuse/router';
import { onMounted, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import usePagination from '../composables/pagination';
import useSort from "../composables/sort";
import { Author } from '../model/Author';
import { Role } from '../model/Role';
import dataService from "../services/DataService";
import SortFilterBarVue from "./SortFilterBar.vue";

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | ' + t('book.author', 2))

const authors: Ref<Array<Author>> = ref([])

const search_query = ref('')

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('name,asc')

const open = ref(false)

const getBookIsLoading: Ref<boolean> = ref(false)

// Filters
const role: Ref<Role> = useRouteQuery('role', Role.ANY)

// --- LocalStorage keys ---
const SORT_BY_KEY = "authorSortBy";
const SORT_ORDER_KEY = "authorSortOrder";

// Restore saved settings
onMounted(() => {
  const savedSortBy = localStorage.getItem(SORT_BY_KEY);
  const savedSortOrder = localStorage.getItem(SORT_ORDER_KEY);
  if (savedSortBy) sortBy.value = savedSortBy;
  if (savedSortOrder) sortOrder.value = savedSortOrder;
});

// Persist changes
watch(sortBy, (newVal) => {
  localStorage.setItem(SORT_BY_KEY, newVal);
});
watch(sortOrder, (newVal) => {
  localStorage.setItem(SORT_ORDER_KEY, newVal);
});

watch([page, role, sortQuery, search_query], (newVal, oldVal) => {
  console.log("all " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetAuthors()
  }
})

const getAuthors = () => {
  getBookIsLoading.value = true
  dataService.findAuthorByCriteria(role.value, search_query.value, pageAsNumber.value -1, perPage.value, sortQuery.value)
  .then(res => {
        console.log(res)
          total.value = res.totalElements
          authors.value = res.content
        if (! res.empty) {
          page.value =  (res.number + 1).toString(10)
        }
        else {
          page.value = "1"
        }
        getBookIsLoading.value = false
        updatePageLoading(false)
  })
  .catch(e => {
      getBookIsLoading.value = false
      updatePageLoading(false)
  })
}

// watches set above sometimes called twice
// so getBooks was sometimes called twice at the same instant
const throttledGetAuthors = useThrottleFn(() => {
  getAuthors()
}, 100, false)

onMounted(() => {
  console.log("Component is mounted!");
});

try {
  getAuthors();
} catch (error) {
  console.log("failed get authors : " + error);
}

</script>

<template>
  <sort-filter-bar-vue
    :open="open"
    :order="sortOrder"
    @update:open="open = $event"
    @update:sort-order="sortOrderUpdated"
  >
    <template #sort-fields>
      <label class="label">{{ t('sorting.sort_by') }} : </label>
      <div class="field">
        <input
          v-model="sortBy"
          type="radio"
          name="radio-20"
          class="radio radio-primary my-2"
          value="name"
        >
        <span class="label-text capitalize">{{ t('sorting.name') }}</span>
      </div>
      <div class="field">
        <input
          v-model="sortBy"
          type="radio"
          name="radio-20"
          class="radio radio-primary mb-2"
          value="creationDate"
        >
        <span class="label-text">{{ t('sorting.date_added') }}</span>
      </div>
      <div class="field">
        <input
          v-model="sortBy"
          type="radio"
          name="radio-20"
          class="radio radio-primary"
          value="random"
        >
        <span class="label-text">{{ t('sorting.random') }}</span>
      </div>
      <!-- </div> -->
    </template>
    <template #filters>
      <div class="field capitalize flex flex-col gap-1">
        <div class="field flex flex-col items-start">
          <label class="label">{{ t('filtering.role') }} : </label>
          <div class="">
            <input
              v-model="role"
              type="radio"
              name="radio-53"
              class="radio radio-primary my-1"
              value="ANY"
            >
            <span class="label-text">{{ t('filtering.any') }}</span>
          </div>
          <div class="">
            <input
              v-model="role"
              type="radio"
              name="radio-53"
              class="radio radio-primary my-1"
              value="AUTHOR"
            >
            <span class="label-text">{{ t('book.author') }}</span>
          </div>
          <div class="">
            <input
              v-model="role"
              type="radio"
              name="radio-53"
              class="radio radio-primary my-1"
              value="TRANSLATOR"
            >
            <span class="label-text">{{ t('book.translator') }}</span>
          </div>
          <div class="">
            <input
              v-model="role"
              type="radio"
              name="radio-53"
              class="radio radio-primary my-1"
              value="NARRATOR"
            >
            <span class="label-text">{{ t('book.narrator') }}</span>
          </div>
        </div>
      </div>
      <label class="label capitalize">{{ t('filtering.filter') }} : </label>
      <label class="input">
        <svg
          class="h-[1em] opacity-50"
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 24 24"
        >
          <g
            stroke-linejoin="round"
            stroke-linecap="round"
            stroke-width="2.5"
            fill="none"
            stroke="currentColor"
          >
            <circle
              cx="11"
              cy="11"
              r="8"
            />
            <path d="m21 21-4.3-4.3" />
          </g>
        </svg>
        <input
          v-model="search_query"
          type="search"
          :placeholder="t('filtering.type_to_filter')"
        >
      </label>
    </template>
  </sort-filter-bar-vue>
  <div class="flex flex-row justify-between mb-2">
    <div class="flex flex-row gap-1 order-last sm:order-first">
      <button
        class="btn btn-outline btn-success"
        @click="open = !open"
      >
        <span class="icon text-lg">
          <i class="mdi mdi-filter-variant" />
        </span>
      </button>
    </div>
    <h2 class="text-3xl typewriter capitalize">
      <span class="icon">
        <i class="mdi mdi-bookshelf" />
      </span>
      &nbsp; {{ t('book.author', 2) }} :
    </h2>
    <div />
  </div>
  <o-pagination
    v-if="authors.length > 0 && pageCount > 1"
    :current="pageAsNumber"
    :total="total"
    order="centered"
    :per-page="perPage"
    @change="updatePage"
  />
  <div
    v-if="authors.length > 0"
    class="grid grid-cols-2 md:grid-cols-4 xl:grid-cols-6 2xl:grid-cols-8 gap-0 my-3 shrink-0 grow-0 mt-2"
  >
    <TransitionGroup name="list">
      <div
        v-for="author in authors"
        :key="author.id"
        class="m-1"
      >
        <div class="card bg-base-100 shadow-sm">
          <figure>
            <img
              v-if="author.image"
              :src="'/files/' + author.image"
              class="max-h-80"
              alt="author image"
            >
            <img
              v-else
              src="../assets/placeholer_author.jpg"
              alt="cover placeholder"
              class="max-h-80"
            >
          </figure>
          <div class="card-body">
            <router-link
              class="card-title link hover:underline hover:decoration-4 hover:decoration-secondary"
              :to="{ name: 'author-detail', params: { authorId: author.id } }"
            >
              {{ author.name }}&nbsp;
            </router-link>
          </div>
        </div>
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
      {{ t('labels.list_empty') }}
    </h2>
    <span class="icon">
      <i class="mdi mdi-book-open-page-variant-outline mdi-48px" />
    </span>
  </div>

  <o-pagination
    v-if="authors.length > 0"
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
