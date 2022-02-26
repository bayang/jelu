<script setup lang="ts">
import { computed, Ref, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { UserBook } from '../model/Book'
import { useStore } from 'vuex'
import { key } from '../store'
import dataService from "../services/DataService"
import { DateUtils } from "../utils/DateUtils"
import { ObjectUtils } from '../utils/ObjectUtils'
import EditBookModal from "./EditBookModal.vue"
import ReadingEventModalVue from './ReadingEventModal.vue'
import { useProgrammatic } from "@oruga-ui/oruga-next";
import dayjs from 'dayjs'
import { CreateReadingEvent, ReadingEvent, ReadingEventType } from '../model/ReadingEvent'

const props = defineProps<{ bookId: string }>()

const store = useStore(key)
const router = useRouter()
const {oruga} = useProgrammatic();
console.log(oruga)

const isAdmin = computed(() => {
  return store.getters.isAdmin
})

const book : Ref<UserBook|null> = ref(null)
const edit: Ref<boolean> = ref(false)
const showModal: Ref<boolean> = ref(false)

const getBook = async () => {
  try {
    book.value = await dataService.getUserBookById(props.bookId)
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

const format = (dateString: string|Date|null|undefined) => {
  if (dateString != null) {
    return DateUtils.formatDate(dateString)
  }
  return ''
}

function modalClosed() {
  console.log("modal closed")
  getBook()
}

const toggleEdit = () => {
  edit.value = ! edit.value
  oruga.modal.open({
    parent: this,
          component: EditBookModal,
          trapFocus: true,
          active: true,
          // fullScreen: false,
          canCancel: ['x', 'button', 'outside'],
          scroll: 'clip',
          props: {
            "book" : book.value,
            canAddEvent: false
          },
          onClose: modalClosed
        });
}

// const toggleEditEventModal = (currentEvent: ReadingEvent) => {
//   showModal.value = !showModal.value
//   oruga.modal.open({
//     parent: this,
//     component: EditReadingEventModalVue,
//     trapFocus: true,
//     fullScreen: true,
//     custom:true,
//     active: true,
//     canCancel: ['x', 'button', 'outside'],
//     scroll: 'keep',
//     props: {
//       "readingEvent" : currentEvent
//     },
//     onClose: modalClosed
//   });
// }

function toggleReadingEventModal(currentEvent: ReadingEvent, edit: boolean) {
  showModal.value = !showModal.value
  oruga.modal.open({
    // parent: this,
    component: ReadingEventModalVue,
    trapFocus: true,
    // fullScreen: true,
    custom:true,
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
    router.push({name: 'home'})
  })
  .catch (err => {
      ObjectUtils.toast(oruga, "danger", `Error deleteting ` + err.message, 4000);
    })
}

console.log('The id value is: ' + props.bookId)

const eventClass = (event: ReadingEvent) => {
  if (event.eventType === ReadingEventType.FINISHED) {
    return "is-info";
  } else if (event.eventType === ReadingEventType.DROPPED) {
    return "is-danger";
  } else if (
    event.eventType === ReadingEventType.CURRENTLY_READING
  ) {
    return "is-success";
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
  <div class="columns is-multiline box">
    <div class="column is-centered is-three-fifths">
      <h3 class="subtitle is-3 is-capitalized has-text-weight-normal typewriter">
        {{ book?.book?.title }}
      </h3>
    </div>
    <div
      v-if="book != null"
      class="column is-two-fifth"
    >
      <button
        class="button is-primary is-light mr-2"
        @click="toggleEdit"
      >
        <span class="icon">
          <i class="mdi mdi-pencil" />
        </span>
        <span>Edit</span>
      </button>
      <button
        class="button is-danger is-light mr-2"
        @click="deleteBook"
      >
        <span class="icon">
          <i class="mdi mdi-delete" />
        </span>
        <span>Delete</span>
      </button>
      <button
        class="button is-info is-light"
        @click="toggleReadingEventModal(defaultCreateEvent(), false)"
      >
        <span class="icon">
          <i class="mdi mdi-plus" />
        </span>
        <span>Event</span>
      </button>
    </div>
    <div class="column is-one-fifth is-offset-one-fifth">
      <figure class="image is-3by4">
        <img
          v-if="book?.book?.image"
          :src="'/files/' + book.book.image"
          alt="cover image"
        >
        <img
          v-else
          src="../assets/placeholder_asset.png"
          alt="cover placeholder"
        >
      </figure>
    </div>
    <div class="column is-three-fifths content">
      <p
        v-if="book != null && book.book != null && book.book.authors != null && book?.book?.authors?.length > 0"
        class="has-text-left"
      >
        <span class="has-text-weight-semibold">Authors : </span>
      </p>
      <ul
        v-if="book != null && book.book != null && book.book.authors != null && book?.book?.authors?.length > 0"
        class="has-text-left block"
      >
        <li
          v-for="author in book?.book?.authors"
          :key="author.id"
        >
          {{ author.name }}
        </li>
      </ul>
      <p
        v-if="book?.book?.publisher"
        class="has-text-left block"
      >
        <span class="has-text-weight-semibold">Publisher : </span>{{ book.book.publisher }}
      </p>
      <p
        v-if="book?.book?.isbn10"
        class="has-text-left block"
      >
        <span class="has-text-weight-semibold">ISBN10 : </span>{{ book.book.isbn10 }}
      </p>
      <p
        v-if="book?.book?.isbn13"
        class="has-text-left block"
      >
        <span class="has-text-weight-semibold">ISBN13 : </span>{{ book.book.isbn13 }}
      </p>
      <p
        v-if="book?.book?.pageCount"
        class="has-text-left block"
      >
        <span class="has-text-weight-semibold">Pages : </span>{{ book.book.pageCount }}
      </p>
      <p
        v-if="book?.book?.publishedDate"
        class="has-text-left block"
      >
        <span class="has-text-weight-semibold">Published date : </span>{{ format(book.book.publishedDate) }}
      </p>
      <p
        v-if="book?.book?.series"
        class="has-text-left block"
      >
        <span class="has-text-weight-semibold">Series : </span>{{ book.book.series }}
      </p>
      <p
        v-if="book?.book?.numberInSeries"
        class="has-text-left block"
      >
        <span class="has-text-weight-semibold">Number in series : </span>{{ book.book.numberInSeries }}
      </p>
      <p
        v-if="book?.book?.language"
        class="has-text-left block"
      >
        <span class="has-text-weight-semibold">Language : </span>{{ book.book.language }}
      </p>
      <div
        v-if="book?.owned || book?.toRead"
        class="field has-text-left"
      >
        <span
          v-if="book?.owned"
          class="tag is-info mx-1"
        >Owned</span>
        <span
          v-if="book?.toRead"
          class="tag is-info"
        >To read</span>
      </div>
    </div>
    <div
      v-if="book?.book?.summary"
      class="column is-three-fifths is-offset-one-quarter content jelu-bordered"
    >
      <p
        v-if="book?.book?.summary"
        class="has-text-left has-text-weight-semibold"
      >
        Summary :
      </p>
      <p
        v-if="book?.book?.summary"
        class="has-text-left"
        v-html="book.book.summary"
      />
    </div>
    <div class="column is-full is-offset-one-quarter content tags has-text-left  has-text-weight-semibold">
      <span
        v-for="tag in book?.book?.tags"
        :key="tag.id"
        class="tag is-primary is-light"
      ><router-link :to="{ name: 'tag-detail', params: { tagId: tag.id } }">{{ tag.name }}&nbsp;</router-link></span>
    </div>
    <div
      v-if="hasExternalLink"
      class="column is-full is-offset-one-quarter content tags has-text-left has-text-weight-semibold"
    >
      <span
        v-if="book?.book.goodreadsId"
        class="tag is-warning"
      ><a
        :href="'https://www.goodreads.com/book/show/' + book.book.goodreadsId"
        target="_blank"
      >goodreads</a></span>
      <span
        v-if="book?.book.googleId"
        class="tag is-warning"
      ><a
        :href="'https://books.google.com/books?id=' + book.book.googleId"
        target="_blank"
      >google</a></span>
      <span
        v-if="book?.book.amazonId"
        class="tag is-warning"
      ><a
        :href="'https://www.amazon.com/dp/' + book.book.amazonId"
        target="_blank"
      >amazon</a></span>
      <span
        v-if="book?.book.librarythingId"
        class="tag is-warning"
      ><a
        :href="'https://www.librarything.com/work/' + book.book.librarythingId"
        target="_blank"
      >librarything</a></span>
    </div>
    <div
      v-if="book?.personalNotes"
      class="column is-full is-offset-one-quarter content"
    >
      <p
        v-if="book?.personalNotes"
        class="has-text-left  has-text-weight-semibold"
      >
        Personal Notes :
      </p>
      <p
        v-if="book?.personalNotes"
        class="has-text-left"
      >
        {{ book.personalNotes }}
      </p>
    </div>
    <div
      v-if="book?.readingEvents != null && book?.readingEvents?.length > 0"
      class="column is-full is-offset-one-quarter content"
    >
      <p
        v-if="book?.readingEvents != null && book?.readingEvents?.length > 0"
        class="has-text-left has-text-weight-semibold typewriter"
      >
        Reading events :
      </p>
      <div class="timeline">
        <header class="timeline-header">
          <span class="tag is-medium is-success">Now</span>
        </header>
        <div
          v-for="event in sortedEvents"
          :key="event.id"
        >
          <div
            class="timeline-item"
          >
            <div
              v-tooltip="{ content: 'Double click to edit.', delay: {show: 5,hide:2} }"
              class="timeline-marker is-icon"
              :class="eventClass(event)"
              @dblclick="toggleReadingEventModal(event, true)"
            >
              <i
                class="mdi"
                :class="iconClass(event)"
              />
            </div>
            <div class="timeline-content">
              <p class="heading">
                {{ format(event.modificationDate) }}
              </p>
              <div>
                <p>
                  {{ event.eventType }} 
                  <span 
                    class="icon is-hidden-tablet"
                    @click="toggleReadingEventModal(event, true)"
                  >
                    <i class="background-on-hover mdi mdi-pencil" />
                  </span>
                </p>
              </div>
            </div>
          </div>
        </div>
        <div class="timeline-header">
          <span class="tag is-medium is-success">Before</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>

.columns {
  margin-top: 10px;
}
.background-on-hover:hover {
  background-color: #cccccccc;
  border-radius: 10px;
  padding: 3px;
}

</style>
