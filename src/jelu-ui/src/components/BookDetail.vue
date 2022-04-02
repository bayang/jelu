<script setup lang="ts">
import { computed, Ref, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { UserBook } from '../model/Book'
import { useStore } from 'vuex'
import { key } from '../store'
import dataService from "../services/DataService"
import { ObjectUtils } from '../utils/ObjectUtils'
import EditBookModal from "./EditBookModal.vue"
import ReadingEventModalVue from './ReadingEventModal.vue'
import { useProgrammatic } from "@oruga-ui/oruga-next";
import dayjs from 'dayjs'
import { CreateReadingEvent, ReadingEvent, ReadingEventType } from '../model/ReadingEvent'
import { useTitle } from '@vueuse/core'
import useDates from '../composables/dates'

const props = defineProps<{ bookId: string }>()

const store = useStore(key)
const router = useRouter()
const { oruga } = useProgrammatic();
console.log(oruga)

const { formatDate, formatDateString } = useDates()

const isAdmin = computed(() => {
  return store.getters.isAdmin
})

const book: Ref<UserBook | null> = ref(null)
const edit: Ref<boolean> = ref(false)
const showModal: Ref<boolean> = ref(false)

const getBook = async () => {
  try {
    book.value = await dataService.getUserBookById(props.bookId)
    useTitle('Jelu | ' + book.value.book.title)
  } catch (error) {
    console.log("failed get book : " + error);
  }
};

watch(() => props.bookId, (newValue, oldValue) => {
  console.log('The new bookId is: ' + props.bookId)
})

const sortedEvents = computed(() => {
  if (book.value && book.value.readingEvents) {
    return [...book.value.readingEvents].sort((a, b) => dayjs(a.modificationDate).isAfter(dayjs(b.modificationDate)) ? -1 : 1)
  }
  else {
    return []
  }
}
)

const hasExternalLink = computed(() => book.value?.book.amazonId != null
  || book.value?.book.goodreadsId != null
  || book.value?.book.googleId != null
  || book.value?.book.librarythingId != null)

function modalClosed() {
  console.log("modal closed")
  getBook()
}

const toggleEdit = () => {
  edit.value = !edit.value
  oruga.modal.open({
    parent: this,
    component: EditBookModal,
    trapFocus: true,
    active: true,
    // fullScreen: true,
    canCancel: ['x', 'button', 'outside'],
    scroll: 'clip',
    props: {
      "book": book.value,
      canAddEvent: false
    },
    onClose: modalClosed
  });
}

function toggleReadingEventModal(currentEvent: ReadingEvent, edit: boolean) {
  showModal.value = !showModal.value
  oruga.modal.open({
    // parent: this,
    component: ReadingEventModalVue,
    trapFocus: true,
    // fullScreen: true,
    // custom: true,
    active: true,
    canCancel: ['x', 'button', 'outside'],
    scroll: 'keep',
    props: {
      "readingEvent": currentEvent,
      "edit": edit
    },
    onClose: modalClosed
  });
}

const deleteBook = async () => {
  let deleteForUserOnly = true
  let abort = false
  if (isAdmin.value) {
    await ObjectUtils.swalMixin.fire({
      html: `<p>Delete book for all users or only you ?</p>`,
      showDenyButton: true,
      showCancelButton: true,
      confirmButtonText: 'Only me',
      denyButtonText: 'All users',
      cancelButtonText: `Don't delete`,
    }).then((result) => {
      if (result.isDenied) {
        deleteForUserOnly = false
      } else if (result.isDismissed) {
        abort = true
        return;
      }
    })
  }
  else {
    await ObjectUtils.swalMixin.fire({
      html: `<p>Delete this book ?</p>`,
      showCancelButton: true,
      showConfirmButton: false,
      showDenyButton: true,
      confirmButtonText: 'Delete',
      cancelButtonText: `Don't delete`,
      denyButtonText: 'Delete',
    }).then((result) => {
      if (result.isDismissed) {
        abort = true
        return;
      }
    })
  }
  if (abort) {
    return
  }
  console.log(`delete for user only ${deleteForUserOnly}`)
  let promise
  if (deleteForUserOnly) {
    if (book.value?.id) {
      promise = dataService.deleteUserBook(book.value?.id)
    }
  }
  else {
    if (book.value?.book?.id) {
      promise = dataService.deleteBook(book.value?.book?.id)
    }
  }
  promise?.then(res => {
    ObjectUtils.toast(oruga, "success", `Book was deleted`, 4000);
    router.push({ name: 'home' })
  })
    .catch(err => {
      ObjectUtils.toast(oruga, "danger", `Error deleteting ` + err.message, 4000);
    })
}

console.log('The id value is: ' + props.bookId)

const eventClass = (event: ReadingEvent) => {
  if (event.eventType === ReadingEventType.FINISHED) {
    return "bg-info";
  } else if (event.eventType === ReadingEventType.DROPPED) {
    return "bg-error";
  } else if (
    event.eventType === ReadingEventType.CURRENTLY_READING
  ) {
    return "bg-success";
  }
  else return "";
};

const iconClass = (event: ReadingEvent) => {
  if (event.eventType === ReadingEventType.FINISHED) {
    return "mdi-checkbox-marked-circle";
  } else if (event.eventType === ReadingEventType.DROPPED) {
    return "mdi-close-octagon";
  } else if (
    event.eventType === ReadingEventType.CURRENTLY_READING
  ) {
    return "mdi-book-open-page-variant";
  }
  else return "";
};

function defaultCreateEvent(): CreateReadingEvent {
  return {
    eventType: ReadingEventType.CURRENTLY_READING,
    eventDate: new Date(),
    bookId: book.value?.book.id
  }
}

getBook()

</script>

<template>
  <div class="grid grid-cols-1 justify-center justify-items-center">
    <div class="grid sm:grid-cols-3 mb-4 sm:w-10/12">
      <div />
      <div class>
        <h3 class="typewriter text-3xl">
          {{ book?.book?.title }}
        </h3>
      </div>
      <div
        v-if="book != null"
        class
      >
        <button
          class="btn btn-primary btn-outline is-light mr-2"
          @click="toggleEdit"
        >
          <span class="icon">
            <i class="mdi mdi-pencil mdi-18px" />
          </span>
          <span>Edit</span>
        </button>
        <button
          class="btn btn-error btn-outline mr-2"
          @click="deleteBook"
        >
          <span class="icon">
            <i class="mdi mdi-delete mdi-18px" />
          </span>
          <span>Delete</span>
        </button>
        <button
          class="btn btn-info btn-outline"
          @click="toggleReadingEventModal(defaultCreateEvent(), false)"
        >
          <span class="icon">
            <i class="mdi mdi-plus mdi-18px" />
          </span>
          <span>Event</span>
        </button>
      </div>
    </div>
    <div
      class="justify-center justify-items-center sm:gap-10 grid grid-cols-1 sm:grid-cols-2 sm:w-10/12"
    >
      <div class="sm:justify-self-end">
        <figure class="image is-3by4">
          <img
            v-if="book?.book?.image"
            :src="'/files/' + book.book.image"
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
          v-if="book != null && book.book != null && book.book.authors != null && book?.book?.authors?.length > 0"
        >
          <span class="font-semibold">Authors :</span>
        </p>
        <ul
          v-if="book != null && book.book != null && book.book.authors != null && book?.book?.authors?.length > 0"
        >
          <li
            v-for="author in book?.book?.authors"
            :key="author.id"
          >
            <router-link
              class="link hover:underline hover:decoration-4 hover:decoration-secondary"
              :to="{ name: 'author-detail', params: { authorId: author.id } }"
            >
              {{ author.name }}&nbsp;
            </router-link>
          </li>
        </ul>
        <p v-if="book?.book?.publisher">
          <span class="font-semibold">Publisher :</span>
          {{ book.book.publisher }}
        </p>
        <p v-if="book?.book?.isbn10">
          <span class="font-semibold">ISBN10 :</span>
          {{ book.book.isbn10 }}
        </p>
        <p v-if="book?.book?.isbn13">
          <span class="font-semibold">ISBN13 :</span>
          {{ book.book.isbn13 }}
        </p>
        <p v-if="book?.book?.pageCount">
          <span class="font-semibold">Pages :</span>
          {{ book.book.pageCount }}
        </p>
        <p v-if="book?.book?.publishedDate">
          <span class="font-semibold">Published date :</span>
          {{ formatDateString(book.book.publishedDate) }}
        </p>
        <p v-if="book?.book?.series">
          <span class="font-semibold">Series :</span>
          {{ book.book.series }}&nbsp;
          <span
            v-if="book?.book?.numberInSeries"
          >-&nbsp;{{ book.book.numberInSeries }}</span>
        </p>
        <p v-if="book?.book?.language">
          <span class="font-semibold">Language :</span>
          {{ book.book.language }}
        </p>
        <div v-if="book?.owned || book?.toRead">
          <span
            v-if="book?.owned"
            class="badge badge-info mx-1"
          >Owned</span>
          <span
            v-if="book?.toRead"
            class="badge badge-info"
          >To read</span>
        </div>
      </div>
    </div>
    <div
      v-if="book?.book?.summary"
      class="flex flex-row justify-center mt-4 prose-base dark:prose-invert sm:w-10/12"
    >
      <div
        v-if="book?.book?.summary"
        class="column is-three-fifths is-offset-one-quarter content jelu-bordered w-11/12 sm:w-9/12 p-2.5"
      >
        <p
          v-if="book?.book?.summary"
          class="font-semibold"
        >
          Summary :
        </p>
        <p
          v-if="book?.book?.summary"
          v-html="book.book.summary"
        />
      </div>
    </div>
    <div class="flex flex-row justify-center">
      <span
        v-for="tag in book?.book?.tags"
        :key="tag.id"
        class="badge badge-primary mt-3 m-0.5 hover:border-secondary hover:border-4"
      >
        <router-link :to="{ name: 'tag-detail', params: { tagId: tag.id } }">{{ tag.name }}&nbsp;</router-link>
      </span>
    </div>
    <div
      v-if="hasExternalLink"
      class="space-x-2"
    >
      <span
        v-if="book?.book.goodreadsId"
        class="badge badge-warning mt-2 hover:font-bold"
      >
        <a
          :href="'https://www.goodreads.com/book/show/' + book.book.goodreadsId"
          target="_blank"
        >goodreads</a>
      </span>
      <span
        v-if="book?.book.googleId"
        class="badge badge-warning hover:font-bold"
      >
        <a
          :href="'https://books.google.com/books?id=' + book.book.googleId"
          target="_blank"
        >google</a>
      </span>
      <span
        v-if="book?.book.amazonId"
        class="badge badge-warning hover:font-bold"
      >
        <a
          :href="'https://www.amazon.com/dp/' + book.book.amazonId"
          target="_blank"
        >amazon</a>
      </span>
      <span
        v-if="book?.book.librarythingId"
        class="badge badge-warning hover:font-bold"
      >
        <a
          :href="'https://www.librarything.com/work/' + book.book.librarythingId"
          target="_blank"
        >librarything</a>
      </span>
    </div>
    <div
      v-if="book?.personalNotes"
      class="mt-4"
    >
      <p
        v-if="book?.personalNotes"
        class="font-semibold"
      >
        Personal Notes :
      </p>
      <p v-if="book?.personalNotes">
        {{ book.personalNotes }}
      </p>
    </div>
    <!-- https://tailwindcomponents.com/component/vertical-timeline -->
    <div
      v-if="book?.readingEvents != null && book?.readingEvents?.length > 0"
      class="mt-4"
    >
      <p
        v-if="book?.readingEvents != null && book?.readingEvents?.length > 0"
        class="typewriter text-2xl mb-3"
      >
        Reading events :
      </p>
      <div class="flex flex-col md:grid grid-cols-9 mx-auto p-2 text-blue-50">
        <div class="col-start-5 bg-accent mb-3 p-2 rounded font-semibold">
          Now
        </div>

        <div
          v-for="(event, index) in sortedEvents"
          :key="event.id"
          class="flex md:contents"
          :class="{ 'flex-row-reverse': index % 2 === 0 }"
        >
          <div
            v-if="index % 2 === 0"
            class="bg-accent col-start-1 col-end-5 p-2 rounded my-4 ml-auto shadow-md"
          >
            <h3 class="font-semibold">
              {{ formatDate(event.modificationDate) }}
            </h3>
            <p>{{ event.eventType }}</p>
            <button
              class="sm:hidden btn btn-xs btn-circle btn-outline mb-0 border-0"
              @click="toggleReadingEventModal(event, true)"
            >
              <i class="mdi mdi-pencil mdi-18px" />
            </button>
          </div>
          <div
            v-if="index % 2 === 0"
            class="col-start-5 col-end-6 md:mx-auto relative mr-10"
          >
            <div class="h-full w-6 flex items-center justify-center">
              <div class="h-full w-1 bg-base-content pointer-events-none" />
            </div>
            <div
              v-tooltip="{ content: 'Double click to edit.', delay: { show: 5, hide: 2 } }"
              class="w-6 h-6 absolute top-1/2 -mt-3 rounded-full shadow"
              :class="eventClass(event)"
              @dblclick="toggleReadingEventModal(event, true)"
            >
              <i
                class="mdi"
                :class="iconClass(event)"
              />
            </div>
          </div>
          <div
            v-if="index % 2 !== 0"
            class="col-start-5 col-end-6 mr-10 md:mx-auto relative"
          >
            <div class="h-full w-6 flex items-center justify-center">
              <div class="h-full w-1 bg-base-content pointer-events-none" />
            </div>
            <div
              v-tooltip="{ content: 'Double click to edit.', delay: { show: 5, hide: 2 } }"
              class="w-6 h-6 absolute top-1/2 -mt-3 rounded-full shadow"
              :class="eventClass(event)"
              @dblclick="toggleReadingEventModal(event, true)"
            >
              <i
                class="mdi"
                :class="iconClass(event)"
              />
            </div>
          </div>
          <div
            v-if="index % 2 !== 0"
            class="bg-accent col-start-6 col-end-10 p-2 rounded my-4 mr-auto shadow-md"
          >
            <h3 class="font-semibold">
              {{ formatDate(event.modificationDate) }}
            </h3>
            <p>{{ event.eventType }}</p>
            <button
              class="sm:hidden btn btn-xs btn-circle btn-outline mb-0 border-0"
              @click="toggleReadingEventModal(event, true)"
            >
              <i class="mdi mdi-pencil mdi-18px" />
            </button>
          </div>
        </div>
        <div class="col-start-5 bg-accent mt-3 p-2 rounded font-semibold">
          Before
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
</style>