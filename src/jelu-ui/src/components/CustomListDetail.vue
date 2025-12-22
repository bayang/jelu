<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import { computed, Ref, ref, watch } from "vue";
import { useRoute } from 'vue-router';
import usePagination from '../composables/pagination';
import useSort from "../composables/sort";
import { Book } from "../model/Book";
import { CustomList } from '../model/custom-list';
import dataService from "../services/DataService";
import { ObjectUtils } from '../utils/ObjectUtils';
import BookCard from "./BookCard.vue";
import useBulkEdition from '../composables/bulkEdition';
import { useI18n } from 'vue-i18n';

const route = useRoute()
const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const getBookIsLoading: Ref<boolean> = ref(false)
const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('lastReadingEventDate,desc')
const { showSelect, selectAll, checkedCards, cardChecked, toggleEdit } = useBulkEdition(modalClosed)
const list: Ref<CustomList|null> = ref(null)
const books: Ref<Array<Book>> = ref([]);

function modalClosed() {
  console.log("modal closed")
}

watch(() => route.params.listId, (newVal, oldVal) => {
  if (newVal !== oldVal && route.params.listId !== undefined) {
    getList()
  }
})

watch([page, sortQuery], (newVal, oldVal) => {
  console.log("all " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetBooks()
  }
})

const getBooks = (listId: string) => {
  getBookIsLoading.value = true
  dataService.booksForList(listId,
  pageAsNumber.value - 1, perPage.value, sortQuery.value)
  .then(res => {
          total.value = res.totalElements
          books.value = res.content
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
const throttledGetBooks = useThrottleFn(() => {
  getBooks(list.value?.id as string)
}, 100, false)

const getList = async () => {
  try {
    console.log("get list " + route.params.listId)
    list.value = await dataService.findCustomListById(route.params.listId as string)
    useTitle('Jelu | ' + list.value.name)
    getBooks(list.value.id as string)
  } catch (error) {
    console.log("failed get list : " + error);
  }
}

const remove = async () => {
  let tags = list.value?.tags.split(",")
  let tagsId: Array<string> = []
  tags?.forEach(t => {
    for (let ta of books?.value[0].tags) {
      if (ta.name == t) {
        tagsId.push(ta.id as string)
      }
    }
  })
  console.log("removing tags from books")
  console.log(tagsId)
  console.log(checkedCards.value)
  dataService.removeBooksFromList({tags: tagsId, books: checkedCards.value})
  .then(res => {
    getList()
  })
  .catch(err => console.log("failed to remove books from list"))
}

const convertedBooks = computed(() => books.value?.map(b => ObjectUtils.unwrapUserBook(b)))
getList()
</script>

<template>
  <div
    v-if="getBookIsLoading && books.length < 1"
    class="flex flex-row justify-center justify-items-center gap-3"
  >
    <o-skeleton
      class="justify-self-center basis-44"
      height="250px"
      :animated="true"
    />
    <o-skeleton
      class="justify-self-center basis-44"
      height="250px"
      :animated="true"
    />
  </div>
  <o-pagination
    v-if="pageCount > 1"
    v-model:current="pageAsNumber"
    :total="total"
    order="centered"
    :per-page="perPage"
    @change="updatePage"
  />
  <h2 class="text-3xl typewriter">
    <span class="icon">
      <i class="mdi mdi-bookshelf" />
    </span>
    {{ list?.name }} : 
  </h2>
  <div
    v-if="books.length > 0"
    class="grid grid-cols-2 md:grid-cols-4 xl:grid-cols-6 2xl:grid-cols-8 gap-0 my-3 shrink-0 grow-0 mt-2"
  >
    <div
      v-for="book in convertedBooks"
      :key="book.book.id"
      class="m-1"
    >
      <book-card
        :book="book"
        :force-select="false"
        :show-select="list?.actionable as boolean"
        :propose-add="false"
        :public="true"
        class="h-full"
        @update:checked="cardChecked"
      />
    </div>
  </div>
  <div v-else>
    <p class="text-2xl">
      {{ t("lists.empty") }}
    </p>
  </div>
  <button
    v-if="list?.actionable"
    class="btn btn-accent btn-lg uppercase my-2"
    :disabled="checkedCards.length < 1"
    @click="remove"
  >
    {{ t("lists.remove") }}
  </button>
  <o-pagination
    v-model:current="pageAsNumber"
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

<style lang="scss">
</style>
