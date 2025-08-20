<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next"
import { useTitle } from '@vueuse/core'
import { ref, Ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import usePagination from "../composables/pagination"
import useSort from "../composables/sort"
import { Author } from "../model/Author"
import { Book } from "../model/Book"
import { LibraryFilter } from "../model/LibraryFilter"
import { Page } from "../model/Page"
import dataService from "../services/DataService"
import { ObjectUtils } from "../utils/ObjectUtils"

const oruga = useOruga()

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | Authors page')

const { total, page, pageAsNumber, perPage, updatePage, updatePageLoading } = usePagination(12)

const { sortQuery } = useSort('name,desc')

watch([page, sortQuery], (newVal, oldVal) => {
  if (newVal !== oldVal) {
    getOrphanAuthors()
  }
})

let orphanAuthors: Ref<Array<Author>> = ref([]);
const isOrphanFetching = ref(false)

let filteredAuthors: Ref<Array<Author>> = ref([]);
const isFetching = ref(false)

const author: Ref<Author> = ref({name: ""})
const authorBooks: Ref<Page<Book>|null> = ref(null);
const getBooksIsLoading: Ref<boolean> = ref(false)

function getFilteredAuthors(text: string) {
  isFetching.value = true
  dataService.findAuthorByCriteria(text).then((data) => filteredAuthors.value = data.content)
  isFetching.value = false
}

function getOrphanAuthors() {
  isOrphanFetching.value = true
  dataService.getOrphanAuthors(pageAsNumber.value - 1, perPage.value, sortQuery.value)
  .then(
    (res) => {
      total.value = res.totalElements
      orphanAuthors.value = res.content
      if (! res.empty) {
          page.value =  (res.number + 1).toString(10)
        }
        else {
          page.value = "1"
        }
        isOrphanFetching.value = false
        updatePageLoading(false)
    }
  )
  .catch(e => {
    isOrphanFetching.value = false
      updatePageLoading(false)
    })
}

const deleteAuthor = async (target: Author) => {
  if (target.id) {
    dataService.deleteAuthor(target.id)
    .then(res => 
      {
        author.value = {"name" : ""}
        ObjectUtils.toast(oruga, "success", t('labels.operation-success'), 4000);
        getOrphanAuthors()
      }
    )
    .catch(err => {
        ObjectUtils.toast(oruga, "danger", t('labels.error_deleting', {msg : err.message}), 4000);
      })
  }
}

const promptDeleteAuthor = async (author: Author, numberOfBooks: number|undefined) => {
  let abort = false
  await ObjectUtils.swalYesNoMixin.fire({
      html: `<p>${t('labels.delete_this_author', {nb: numberOfBooks})}</p>`,
      showCancelButton: true,
      showConfirmButton: true,
      showDenyButton: false,
      confirmButtonText: t('labels.delete'),
      cancelButtonText: t('labels.dont_delete')
    }).then((result) => {
      if (result.isDismissed) {
        abort = true
      }
    })
    if (abort) {
    return
  }
  deleteAuthor(author)
}

const getAuthor = async (selected: Author) => {
  try {
    author.value = await dataService.getAuthorById(selected.id as string)
  } catch (error) {
    console.log("failed get author : " + error);
  }
}

const getBooks = (author: Author) => {
    getBooksIsLoading.value = true
    dataService.getAuthorBooksById(author.id as string, 
      0, 2, "title:desc", LibraryFilter.ANY)
      .then(res => {
          authorBooks.value = res
          getBooksIsLoading.value = false
      }
      )
      .catch(e => {
        getBooksIsLoading.value = false
      })
}

const selectAuthor = (selected: Author) => {
  getAuthor(selected)
  getBooks(selected)
}

getOrphanAuthors()

</script>

<template>
  <div class="w-fit sm:w-full flex flex-wrap justify-items-center justify-self-center gap-3 sm:gap-0">
    <div class="w-full sm:w-1/2 sm:p-3">
      <h1 class="typewriter text-2xl mb-3 capitalize">
        {{ t('labels.orphan_authors') }} :
      </h1>
      <div>
        <ul>
          <li
            v-for="orphan in orphanAuthors"
            :key="orphan.id"
            class="my-2"
          >
            <div class="alert shadow-lg w-full">
              <i class="mdi mdi-account mdi-24px" />
              <h3 class="font-bold">
                {{ orphan.name }}
              </h3>
              <button
                class="btn btn-sm"
                @click="deleteAuthor(orphan)"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  class="h-5 w-5"
                  viewBox="0 0 20 20"
                  fill="currentColor"
                >
                  <path
                    fill-rule="evenodd"
                    d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z"
                    clip-rule="evenodd"
                  />
                </svg>
              </button>
            </div>
          </li>
        </ul>
      </div>
      <o-pagination
        v-if="orphanAuthors.length > 0"
        :current="pageAsNumber"
        :total="total"
        order="centered"
        :per-page="perPage"
        @change="updatePage"
      />
    </div>
    <div class="w-full sm:w-1/2 sm:p-3">
      <h1 class="typewriter text-2xl mb-3 capitalize">
        {{ t('labels.find-authors') }} :
      </h1>
      <div class="field border-2 border-accent">
        <o-field>
          <o-autocomplete
            :data="filteredAuthors"
            :clear-on-select="true"
            field="name"
            :loading="isFetching"
            :debounce="100"
            @input="getFilteredAuthors"
            @select="selectAuthor"
          />
        </o-field>
      </div>
      <div
        v-if="author.id != null"
        class="alert shadow-lg w-full mt-3"
      >
        <div class="w-full">
          <span class="capitalize">{{ t('book.author', 1) }}</span> :
          <router-link
            class="hover:underline hover:decoration-4 hover:decoration-secondary"
            :to="{ name: 'author-detail', params: { authorId: author.id } }"
          >
            {{ author.name }}&nbsp;
          </router-link>
          {{ authorBooks?.totalElements }} {{ t('labels.associated-books') }}
        </div>
        <button
          class="btn btn-sm"
          @click="promptDeleteAuthor(author, authorBooks?.totalElements)"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-5 w-5"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fill-rule="evenodd"
              d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z"
              clip-rule="evenodd"
            />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
</style>
