<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next"
import { until } from '@vueuse/core'
import { computed, Ref, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import useDates from '../composables/dates'
import { Book, UserBook } from '../model/Book'
import dataService from "../services/DataService"
import { ObjectUtils } from '../utils/ObjectUtils'
import EditBookModal from "./EditBookModal.vue"

const { t, d } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const props = defineProps<{ 
  book: Book,
  owned: boolean|null,
  toRead: boolean|null,
  borrowed: boolean|null,
  links: boolean|null,
  bookLink: boolean|null,
  addBook: boolean
}>()

const router = useRouter()
const oruga = useOruga();
console.log(oruga)

const { formatDate, formatDateString } = useDates()

const getBookIsLoading: Ref<boolean> = ref(false)

const hasExternalLink = computed(() => props.book.amazonId != null
  || props.book.goodreadsId != null
  || props.book.googleId != null
  || props.book.librarythingId != null)

const userbookId: Ref<string|null> = ref(null)
const bookCanBeAdded: Ref<boolean> = ref(false)

const getUserbookId = async () => {
  bookCanBeAdded.value = false
  await until(props.book.id).not.toBeNull()
  console.log("book id " + props.book.id)
  dataService.findUserBookByCriteria(null, props.book.id, null, null, null, null, 0, 10)
  .then(res => {
    if (!res.empty) {
      if (res.content.length > 0 && res.content[0].id != null) {
        userbookId.value = res.content[0].id
      } else {
        bookCanBeAdded.value = true
      }
    } else {
      bookCanBeAdded.value = true
    }
  })
}

watch(() => props.book.id, (newVal, oldVal) => {
  console.log("props.book.id ")
  console.log(newVal + " " + oldVal)
  bookCanBeAdded.value = false
  if (newVal !== oldVal && props.book.id != null) {
    getUserbookId()
  }
})

const toggleEdit = (book: UserBook) => {
  if (book.id == null) {
    console.log("book")
    console.log(book)
    oruga.modal.open({
            component: EditBookModal,
            trapFocus: true,
            active: true,
            canCancel: ['x', 'button', 'outside'],
            scroll: 'clip',
            props: {
              "book" : book,
              canAddEvent: true
            },
            onClose: modalClosed
          });
  }
}

function modalClosed() {
  console.log("modal closed from data card")
  router.push({ name: 'book-reviews', params: { bookId: props.book.id } })
}

</script>

<template>
  <div class="grid grid-cols-1 justify-center justify-items-center">
    <div class="grid sm:grid-cols-3 mb-4 sm:w-10/12">
      <div />
      <div class="flex justify-center gap-10">
        <router-link
          v-if="props.bookLink != null && props.bookLink === true && userbookId != null"
          class="link hover:underline hover:decoration-4 hover:decoration-secondary text-3xl typewriter"
          :to="{ name: 'book-detail', params: { bookId: userbookId } }"
        >
          {{ props.book.title }}&nbsp;
        </router-link>
        <h3
          v-else
          class="typewriter text-3xl"
        >
          {{ props.book.title }}
        </h3>
        <button
          v-if="props.addBook && bookCanBeAdded"
          v-tooltip="t('labels.add_to_my_books')"
          class="btn btn-info btn-outline uppercase"
          @click="toggleEdit(ObjectUtils.toUserBook(props.book))"
        >
          <span class="icon">
            <i class="mdi mdi-plus mdi-18px" />
          </span>
          <span>{{ t('labels.add') }}</span>
        </button>
      </div>
    </div>
    <div
      class="justify-center justify-items-center sm:gap-10 grid grid-cols-1 sm:grid-cols-2 sm:w-10/12"
    >
      <div class="sm:justify-self-end">
        <figure>
          <img
            v-if="props.book.image"
            :src="'/files/' + props.book.image"
            alt="cover image"
            class="max-h-96"
          >
          <img
            v-else
            src="../assets/placeholder_asset.jpg"
            alt="cover placeholder"
          >
        </figure>
      </div>
      <div class="text-left sm:justify-self-start">
        <p
          v-if="props.book != null && props.book.authors != null && props.book.authors?.length > 0"
        >
          <span class="font-semibold capitalize">{{ t('book.author', 2) }} :</span>
        </p>
        <ul
          v-if="props.book != null && props.book.authors != null && props.book.authors?.length > 0"
        >
          <li
            v-for="author in props.book.authors"
            :key="author.id"
          >
            <router-link
              v-if="links != null && links === true"
              class="link hover:underline hover:decoration-4 hover:decoration-secondary"
              :to="{ name: 'author-detail', params: { authorId: author.id } }"
            >
              {{ author.name }}&nbsp;
            </router-link>
            <p v-else>
              {{ author.name }}
            </p>
          </li>
        </ul>
        <p v-if="props.book.publisher">
          <span class="font-semibold capitalize">{{ t('book.publisher') }} :</span>
          {{ props.book.publisher }}
        </p>
        <p v-if="props.book.isbn10">
          <span class="font-semibold uppercase">{{ t('book.isbn10') }} :</span>
          {{ props.book.isbn10 }}
        </p>
        <p v-if="props.book.isbn13">
          <span class="font-semibold uppercase">{{ t('book.isbn13') }} :</span>
          {{ props.book.isbn13 }}
        </p>
        <p v-if="props.book.pageCount">
          <span class="font-semibold capitalize">{{ t('book.page', 2) }} :</span>
          {{ props.book.pageCount }}
        </p>
        <p v-if="props.book.publishedDate">
          <span class="font-semibold capitalize">{{ t('book.published_date') }} :</span>
          {{ formatDateString(props.book.publishedDate) }}
        </p>
        <div v-if="props.book.series && props.book.series.length > 0">
          <span class="font-semibold capitalize">{{ t('book.series') }} :&nbsp;</span>
          <ul>
            <li
              v-for="seriesItem in props.book.series"
              :key="seriesItem.seriesId"
            >
              <router-link
                v-if="links != null && links === true"
                class="link hover:underline hover:decoration-4 hover:decoration-secondary"
                :to="{ name: 'series', params: { seriesId: seriesItem.seriesId } }"
              >
                {{ seriesItem.name }}&nbsp;
                <span
                  v-if="seriesItem.numberInSeries"
                >-&nbsp;{{ seriesItem.numberInSeries }}</span>
              </router-link>
              <div
                v-else
                class="inline-block"
              >
                <span>{{ seriesItem.name }}&nbsp;</span>
                <span
                  v-if="seriesItem.numberInSeries"
                >-&nbsp;{{ seriesItem.numberInSeries }}</span>
              </div>
            </li>
          </ul>
        </div>
        <p v-if="props.book.language">
          <span class="font-semibold capitalize">{{ t('book.language') }} :</span>
          {{ props.book.language }}
        </p>
        <div v-if="owned || toRead || borrowed">
          <span
            v-if="owned"
            class="badge badge-info"
          >{{ t('book.owned') }}</span>
          <span
            v-if="toRead"
            class="badge badge-info mx-1"
          >{{ t('book.to_read') }}</span>
          <span
            v-if="borrowed"
            class="badge badge-info"
          >{{ t('book.borrowed') }}</span>
        </div>
      </div>
    </div>
    <div
      v-if="props.book.summary"
      class="flex flex-row justify-center mt-4 prose-base dark:prose-invert sm:w-10/12"
    >
      <div
        v-if="props.book.summary"
        class="jelu-bordered w-11/12 sm:w-9/12 p-2.5"
      >
        <p
          v-if="props.book.summary"
          class="font-semibold capitalize"
        >
          {{ t('book.summary') }} :
        </p>
        <p
          v-if="props.book.summary"
          class="text-justify"
          v-html="props.book.summary"
        />
      </div>
    </div>
    <div class="flex flex-row justify-center">
      <span
        v-for="tag in props.book.tags"
        :key="tag.id"
        class="badge badge-primary mt-3 m-0.5 hover:font-bold hover:border-4"
      >
        <router-link
          v-if="links != null && links === true"
          :to="{ name: 'tag-detail', params: { tagId: tag.id } }"
        >{{ tag.name }}&nbsp;</router-link>
        <p v-else>{{ tag.name }}</p>
      </span>
    </div>
    <div
      v-if="hasExternalLink"
      class="space-x-2"
    >
      <span
        v-if="props.book?.goodreadsId"
        class="badge badge-warning mt-2 hover:font-bold"
      >
        <a
          :href="'https://www.goodreads.com/book/show/' + props.book.goodreadsId"
          target="_blank"
        >goodreads</a>
      </span>
      <span
        v-if="props.book?.googleId"
        class="badge badge-warning hover:font-bold"
      >
        <a
          :href="'https://books.google.com/books?id=' + props.book.googleId"
          target="_blank"
        >google</a>
      </span>
      <span
        v-if="props.book?.amazonId"
        class="badge badge-warning hover:font-bold"
      >
        <a
          :href="'https://www.amazon.com/dp/' + props.book.amazonId"
          target="_blank"
        >amazon</a>
      </span>
      <span
        v-if="props.book?.librarythingId"
        class="badge badge-warning hover:font-bold"
      >
        <a
          :href="'https://www.librarything.com/work/' + props.book.librarythingId"
          target="_blank"
        >librarything</a>
      </span>
    </div>
  </div>
  <o-loading
    v-model:active="getBookIsLoading"
    :full-page="true"
    :cancelable="true"
  />
</template>

<style lang="scss" scoped>
</style>
