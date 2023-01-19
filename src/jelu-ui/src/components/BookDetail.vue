<script setup lang="ts">
import { computed, ComputedRef, Ref, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Book, UserBook } from '../model/Book'
import { useStore } from 'vuex'
import { key } from '../store'
import dataService from "../services/DataService"
import { ObjectUtils } from '../utils/ObjectUtils'
import EditBookModal from "./EditBookModal.vue"
import ReadingEventModalVue from './ReadingEventModal.vue'
import ReviewModalVue from './ReviewModal.vue'
import { useProgrammatic } from "@oruga-ui/oruga-next";
import dayjs from 'dayjs'
import { CreateReadingEvent, ReadingEvent, ReadingEventType } from '../model/ReadingEvent'
import { useTitle } from '@vueuse/core'
import useDates from '../composables/dates'
import { useI18n } from 'vue-i18n'
import { useClipboard } from '@vueuse/core'
import { usePermission } from '@vueuse/core'
import { Review } from '../model/Review'
import { until } from '@vueuse/core'
import { User } from '../model/User'
import ReviewCard from "./ReviewCard.vue";
import AutoImportFormModalVue from "./AutoImportFormModal.vue";
import { Metadata } from "../model/Metadata";
import MergeBookModal from './MergeBookModal.vue'

const { t, d } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const { isSupported, copy } = useClipboard()
usePermission('clipboard-read')
usePermission('clipboard-write')

const props = defineProps<{ bookId: string }>()

const store = useStore(key)
const router = useRouter()
const { oruga } = useProgrammatic();
console.log(oruga)

const { formatDate, formatDateString } = useDates()

const isAdmin = computed(() => {
  return store !== undefined && store.getters.isAdmin
})
const user: ComputedRef<User> = computed(() => {
  return store !== undefined && store.getters.getUser
})

const book: Ref<UserBook | null> = ref(null)
const edit: Ref<boolean> = ref(false)
const showModal: Ref<boolean> = ref(false)

const getBookIsLoading: Ref<boolean> = ref(false)

const userReviews: Ref<Array<Review>> = ref([])

const getBook = async () => {
  try {
    getBookIsLoading.value = true
    book.value = await dataService.getUserBookById(props.bookId)
    getBookIsLoading.value = false
    useTitle('Jelu | ' + book.value.book.title)
    getUserReviewsForBook()
  } catch (error) {
    console.log("failed get book : " + error);
    getBookIsLoading.value = false
  }
};

const getUserReviewsForBook = async() => {
  await until(user.value).not.toBeNull()
  dataService.findReviews(user.value.id, book.value?.book.id, null, null, null, 0, 20)
  .then(res => {
    console.log(res)
    if (! res.empty) {
      userReviews.value = res.content
    }
  })
  .catch(err => {
    console.log(err)
  })
}

watch(() => props.bookId, (newValue, oldValue) => {
  console.log('The new bookId is: ' + props.bookId)
})

const sortedEvents = computed(() => {
  if (book.value && book.value.readingEvents) {
    return [...book.value.readingEvents].sort((a, b) => dayjs(a.startDate).isAfter(dayjs(b.startDate)) ? -1 : 1)
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

function reviewModalClosed() {
  console.log("review modal closed")
  getUserReviewsForBook()
}

const toggleEdit = () => {
  edit.value = !edit.value
  oruga.modal.open({
    parent: this,
    component: EditBookModal,
    trapFocus: true,
    active: true,
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
    component: ReadingEventModalVue,
    trapFocus: true,
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

function toggleReviewModal(currentBook: Book|undefined, edit: boolean, review: Review|null) {
  if (currentBook != null && currentBook != undefined) {
    oruga.modal.open({
      component: ReviewModalVue,
      trapFocus: true,
      active: true,
      canCancel: ['x', 'button', 'outside'],
      scroll: 'keep',
      props: {
        "book": currentBook,
        "edit" : edit,
        "review": review
      },
      onClose: reviewModalClosed
    });
  }
}

const toggleFetchMetadataModal = (currentBook: Book|undefined) => {
  oruga.modal.open({
    parent: this,
    component: AutoImportFormModalVue,
    trapFocus: true,
    active: true,
    canCancel: ['x', 'button', 'outside'],
    scroll: 'keep',
    props: {
        "book": currentBook,
      },
    events: {
      metadataReceived: (modalMetadata: Metadata) => {
        console.log("received metadata")
        console.log(modalMetadata)
        toggleMergeBookModal(currentBook, modalMetadata)
      }
    },
    onClose: modalClosed
  });
}

const toggleMergeBookModal = (currentBook: Book|undefined, metadata: Metadata) => {
  oruga.modal.open({
    parent: this,
    component: MergeBookModal,
    trapFocus: true,
    active: true,
    canCancel: ['x', 'button', 'outside'],
    scroll: 'keep',
    props: {
        "book": currentBook,
        "metadata": metadata
      },
    onClose: modalClosed
  });
}

const deleteBook = async () => {
  let deleteForUserOnly = true
  let abort = false
  if (isAdmin.value) {
    await ObjectUtils.swalMixin.fire({
      html: `<p>${t('labels.delete_for_all_or_only_you')}</p>`,
      showDenyButton: true,
      showCancelButton: true,
      confirmButtonText: t('labels.only_me'),
      denyButtonText: t('labels.all_users'),
      cancelButtonText: t('labels.dont_delete'),
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
      html: `<p>${t('labels.delete_this_book')}</p>`,
      showCancelButton: true,
      showConfirmButton: false,
      showDenyButton: true,
      confirmButtonText: t('labels.delete'),
      cancelButtonText: t('labels.dont_delete'),
      denyButtonText: t('labels.delete'),
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
    ObjectUtils.toast(oruga, "success", t('labels.book_was_deleted'), 4000);
    router.push({ name: 'home' })
  })
    .catch(err => {
      ObjectUtils.toast(oruga, "danger", t('labels.delete_for_all_or_only_you', {msg : err.message}), 4000);
    })
}

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

const eventLabel = (type: ReadingEventType) => {
    if (type === ReadingEventType.FINISHED) {
      return t('reading_events.finished');
    } else if (type === ReadingEventType.DROPPED) {
      return t('reading_events.dropped');
    } else if (type === ReadingEventType.CURRENTLY_READING) {
      return t('reading_events.reading');
    } else return "";
};

function defaultCreateEvent(): CreateReadingEvent {
  return {
    eventType: ReadingEventType.CURRENTLY_READING,
    eventDate: new Date(),
    startDate: new Date(),
    bookId: book.value?.book.id
  }
}

const embedCode = computed(() => {
  if (book.value) {
    return generateEmbed(book.value)
  }
  return ''
})

function generateEmbed(book: UserBook) {
  let baseUrl = window.location.origin
  let bookUrl = router.resolve({ name: 'book-detail', params: { bookId: book.id } }).href
  let top = `<div id="embed-body" style="padding: 5px; width: 150px; border: 1px solid #cccccc;}"><div class="embed-element" style="overflow: hidden;list-style: none; text-align: center; padding: 5px; margin: 0px;">`
  if (book.book.image != null) {
    let couv = `<div class="embed-cover"> <a href="${baseUrl}${bookUrl}" target="_blank"><img src="${baseUrl}/files/${book.book.image}" title="${book.book.title}" alt="${book.book.title}" style="border: 1px solid #cccccc;border-width:1px; padding: 3px; background-color: #fff;width:80px;"></a></div>`
    top = top.concat(couv)
  }
  let body = `<div class="embed-book" style="margin: 0px 3px 5px 5px;font-size: 13px;font-family:sans-serif; font-weight : bold;"><a href="${baseUrl}${bookUrl}" target="_blank" style="text-decoration:none;">${book.book.title}</a></div>`
  top = top.concat(body)
  if (book.book.authors != undefined && book.book.authors?.length > 0) {
      let firstAuthor = book?.book.authors[0]
      let authorId = firstAuthor.id
      let rout = router.resolve({ name: 'author-detail', params: { authorId: authorId } }).href
  let authorPart = `<div class="embed-author" style="margin: 0px 3px 5px 5px;font-size: 12px;color: gray;"><a href="${baseUrl}${rout}" target="_blank" style="text-decoration:none;">${firstAuthor.name}</a></div>`
      top = top.concat(authorPart)
    }
  let bottom = `<div class="embed-tail" style="clear:both;"></div></div></div>`
  top = top.concat(bottom)
  return top
}

function copyToClipboard(content: string) {
  copy(content)
  ObjectUtils.toast(oruga, "success", t('labels.saved'), 1000)

}

const deleteReview = async (reviewId: string) => {
  console.log("delete " + reviewId)
  let abort = false
  await ObjectUtils.swalMixin.fire({
      html: `<p>${t('reviews.delete_review')}</p>`,
      showCancelButton: true,
      showConfirmButton: false,
      showDenyButton: true,
      confirmButtonText: t('labels.delete'),
      cancelButtonText: t('labels.dont_delete'),
      denyButtonText: t('labels.delete'),
    }).then((result) => {
      if (result.isDismissed) {
        abort = true
        return;
      }
    })
    console.log("abort " + abort)
    if (abort) {
      return
    }
    dataService.deleteReview(reviewId)
    .then(res => {
      getUserReviewsForBook()
    })
    .catch(err => {
      console.log(err)
    })
}

getBook()

</script>

<template>
  <div class="grid grid-cols-1 justify-center justify-items-center">
    <div class="grid sm:grid-cols-3 mb-4 sm:w-10/12">
      <div />
      <div>
        <h3 class="typewriter text-3xl">
          {{ book?.book?.title }}
        </h3>
      </div>
      <div
        v-if="book != null"
        class="flex items-center flex-wrap"
      >
        <button
          class="btn btn-primary btn-outline is-light mr-2 p-2"
          @click="toggleEdit"
        >
          <span class="icon">
            <i class="mdi mdi-pencil mdi-18px" />
          </span>
          <span>{{ t('labels.edit') }}</span>
        </button>
        <button
          class="btn btn-error btn-outline mr-2 p-2"
          @click="deleteBook"
        >
          <span class="icon">
            <i class="mdi mdi-delete mdi-18px" />
          </span>
          <span>{{ t('labels.delete') }}</span>
        </button>
        <button
          class="btn btn-info btn-outline p-2"
          @click="toggleReadingEventModal(defaultCreateEvent(), false)"
        >
          <span class="icon">
            <i class="mdi mdi-plus mdi-18px" />
          </span>
          <span>{{ t('labels.event') }}</span>
        </button>
        <label
          v-tooltip="t('labels.get_embed_code')"
          for="my-modal-4"
          class="btn btn-circle btn-outline ml-0 border-none modal-button"
        ><svg
          xmlns="http://www.w3.org/2000/svg"
          class="h-5 w-5"
          viewBox="0 0 20 20"
          fill="currentColor"
        >
          <path d="M15 8a3 3 0 10-2.977-2.63l-4.94 2.47a3 3 0 100 4.319l4.94 2.47a3 3 0 10.895-1.789l-4.94-2.47a3.027 3.027 0 000-.74l4.94-2.47C13.456 7.68 14.19 8 15 8z" />
        </svg></label>
        <div class="dropdown dropdown-hover bg-transparent">
          <label
            tabindex="0"
            class="btn m-1 btn-circle btn-outline border-none"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
              class="w-6 h-6"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M12 6.75a.75.75 0 110-1.5.75.75 0 010 1.5zM12 12.75a.75.75 0 110-1.5.75.75 0 010 1.5zM12 18.75a.75.75 0 110-1.5.75.75 0 010 1.5z"
              />
            </svg>
          </label>
          <ul
            tabindex="0"
            class="dropdown-content menu p-2 shadow bg-base-100 rounded-box w-52"
          >
            <li>
              <button
                v-tooltip="t('reviews.create_review')"
                class="btn btn-circle btn-outline border-none"
                @click="toggleReviewModal(book?.book, false, null)"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  class="h-5 w-5"
                  viewBox="0 0 20 20"
                  fill="currentColor"
                >
                  <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                </svg>
              </button>
            </li>
            <li>
              <button
                v-tooltip="t('book_merge.fetch_metadata')"
                class="btn btn-circle btn-outline border-none"
                @click="toggleFetchMetadataModal(book?.book)"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke-width="1.5"
                  stroke="currentColor"
                  class="w-6 h-6"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m5.231 13.481L15 17.25m-4.5-15H5.625c-.621 0-1.125.504-1.125 1.125v16.5c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9zm3.75 11.625a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z"
                  />
                </svg>
              </button>
            </li>
          </ul>
        </div>
      </div>
    </div>
    <div
      class="justify-center justify-items-center sm:gap-10 grid grid-cols-1 sm:grid-cols-2 sm:w-10/12"
    >
      <div class="sm:justify-self-end">
        <figure>
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
          <span class="font-semibold capitalize">{{ t('book.author', 2) }} :</span>
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
        <p
          v-if="book != null && book.book != null && book.book.translators != null && book?.book?.translators?.length > 0"
        >
          <span class="font-semibold capitalize">{{ t('book.translator', 2) }} :</span>
        </p>
        <ul
          v-if="book != null && book.book != null && book.book.translators != null && book?.book?.translators?.length > 0"
        >
          <li
            v-for="translator in book?.book?.translators"
            :key="translator.id"
          >
            <router-link
              class="link hover:underline hover:decoration-4 hover:decoration-secondary"
              :to="{ name: 'author-detail', params: { authorId: translator.id } }"
            >
              {{ translator.name }}&nbsp;
            </router-link>
          </li>
        </ul>
        <p v-if="book?.book?.publisher">
          <span class="font-semibold capitalize">{{ t('book.publisher') }} :</span>
          {{ book.book.publisher }}
        </p>
        <p v-if="book?.book?.isbn10">
          <span class="font-semibold uppercase">{{ t('book.isbn10') }} :</span>
          {{ book.book.isbn10 }}
        </p>
        <p v-if="book?.book?.isbn13">
          <span class="font-semibold uppercase">{{ t('book.isbn13') }} :</span>
          {{ book.book.isbn13 }}
        </p>
        <p v-if="book?.book?.pageCount">
          <span class="font-semibold capitalize">{{ t('book.page', 2) }} :</span>
          {{ book.book.pageCount }}
        </p>
        <p v-if="book?.book?.publishedDate">
          <span class="font-semibold capitalize">{{ t('book.published_date') }} :</span>
          {{ formatDateString(book.book.publishedDate) }}
        </p>
        <p v-if="book?.book?.series">
          <span class="font-semibold capitalize">{{ t('book.series') }} :&nbsp;</span>
          <router-link
            class="link hover:underline hover:decoration-4 hover:decoration-secondary"
            :to="{ name: 'series', query: { series: book?.book.series } }"
          >
            {{ book.book.series }}&nbsp;
            <span
              v-if="book?.book?.numberInSeries"
            >-&nbsp;{{ book.book.numberInSeries }}</span>
          </router-link>
        </p>
        <p v-if="book?.book?.language">
          <span class="font-semibold capitalize">{{ t('book.language') }} :</span>
          {{ book.book.language }}
        </p>
        <div v-if="book?.owned || book?.toRead || book?.borrowed">
          <span
            v-if="book?.owned"
            class="badge badge-info"
          >{{ t('book.owned') }}</span>
          <span
            v-if="book?.toRead"
            class="badge badge-info mx-1"
          >{{ t('book.to_read') }}</span>
          <span
            v-if="book?.borrowed"
            class="badge badge-info"
          >{{ t('book.borrowed') }}</span>
        </div>
      </div>
    </div>
    <div
      v-if="book?.book?.summary"
      class="flex flex-row justify-center mt-4 prose-base dark:prose-invert sm:w-10/12"
    >
      <div
        v-if="book?.book?.summary"
        class="jelu-bordered w-11/12 sm:w-9/12 p-2.5"
      >
        <p
          v-if="book?.book?.summary"
          class="font-semibold capitalize"
        >
          {{ t('book.summary') }} :
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
        class="badge badge-primary mt-3 m-0.5 hover:font-bold hover:border-4"
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
        class="font-semibold capitalize"
      >
        {{ t('book.personal_notes') }} :
      </p>
      <p v-if="book?.personalNotes">
        {{ book.personalNotes }}
      </p>
    </div>
    <div class="mt-2">
      <router-link
        class="link text-2xl typewriter"
        :to="{ name: 'book-reviews', params: { bookId: book?.book.id } }"
      >
        {{ t('reviews.all_reviews') }}
      </router-link>
    </div>
    <div
      v-if="userReviews != null && userReviews.length > 0"
      class="w-11/12 sm:w-10/12 flex flex-row flex-wrap justify-center mt-4 gap-4"
    >
      <p class="typewriter text-2xl mb-3 capitalize sm:w-full">
        {{ t('reviews.my_reviews') }} :
      </p>
      <div
        v-for="review in userReviews"
        :key="review.id"
        class="w-11/12 2xl:basis-10/12"
      >
        <review-card
          v-if="review != null"
          :review="review"
          :show-delete="true"
          :show-edit="true"
          @update:delete="deleteReview($event)"
          @update:edit="toggleReviewModal(book?.book, true, review)"
        />
      </div>
    </div>
    <!-- https://tailwindcomponents.com/component/vertical-timeline -->
    <div
      v-if="book?.readingEvents != null && book?.readingEvents?.length > 0"
      class="mt-4"
    >
      <p
        v-if="book?.readingEvents != null && book?.readingEvents?.length > 0"
        class="typewriter text-2xl mb-3 capitalize"
      >
        {{ t('reading_events.reading_events') }} :
      </p>
      <div class="flex flex-col md:grid grid-cols-9 mx-auto p-2 text-blue-50">
        <div class="col-start-5 mb-3 p-2 font-semibold timeline-item capitalize">
          {{ t('reading_events.now') }}
        </div>

        <div
          v-for="(event, index) in sortedEvents"
          :key="event.id"
          class="flex md:contents"
          :class="{ 'flex-row-reverse': index % 2 === 0 }"
        >
          <div
            v-if="index % 2 === 0"
            class="col-start-1 col-end-5 p-2 my-4 ml-auto shadow-md timeline-item"
          >
            <div
              v-if="event.endDate != null"
              class="sm:flex sm:gap-2"
            >
              <h3 class="font-semibold">
                {{ formatDate(event.endDate) }}
              </h3>
              <p class="capitalize">
                {{ eventLabel(event.eventType) }}&nbsp;-
              </p>
              <h3 class="font-semibold">
                {{ formatDate(event.startDate) }}
              </h3>
              <p class="capitalize">
                started
              </p>
            </div>
            <div v-else>
              <h3 class="font-semibold">
                {{ formatDate(event.startDate) }}
              </h3>
              <p class="capitalize">
                {{ eventLabel(event.eventType) }}
              </p>
            </div>
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
              v-tooltip="{ content: t('labels.double_click_to_edit'), delay: { show: 5, hide: 2 } }"
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
              v-tooltip="{ content: t('labels.double_click_to_edit'), delay: { show: 5, hide: 2 } }"
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
            class="col-start-6 col-end-10 p-2 my-4 mr-auto shadow-md timeline-item"
          >
            <div
              v-if="event.endDate != null"
              class="sm:flex sm:gap-2"
            >
              <h3 class="font-semibold">
                {{ formatDate(event.endDate) }}
              </h3>
              <p class="capitalize">
                {{ eventLabel(event.eventType) }}&nbsp;-
              </p>
              <h3 class="font-semibold">
                {{ formatDate(event.startDate) }}
              </h3>
              <p class="capitalize">
                started
              </p>
            </div>
            <div v-else>
              <h3 class="font-semibold">
                {{ formatDate(event.startDate) }}
              </h3>
              <p class="capitalize">
                {{ eventLabel(event.eventType) }}
              </p>
            </div>
            <button
              class="sm:hidden btn btn-xs btn-circle btn-outline mb-0 border-0"
              @click="toggleReadingEventModal(event, true)"
            >
              <i class="mdi mdi-pencil mdi-18px" />
            </button>
          </div>
        </div>
        <div class="col-start-5 mt-3 p-2 font-semibold timeline-item capitalize">
          {{ t('reading_events.before') }}
        </div>
      </div>
    </div>
  </div>
  <o-loading
    v-model:active="getBookIsLoading"
    :full-page="true"
    :can-cancel="true"
  />
  <input
    id="my-modal-4"
    type="checkbox"
    class="modal-toggle"
  >
  <label
    for="my-modal-4"
    class="modal cursor-pointer"
  >
    <label
      class="modal-box relative"
      for=""
    >
      <div class="flex justify-center items-center">
        <h3 class="text-lg font-bold first-letter:capitalize">{{ t('labels.copy_paste_code') }}</h3>
        <button
          v-if="isSupported"
          class="btn btn-outline btn-sm btn-circle border-none ml-1"
          @click="copyToClipboard(embedCode)"
        ><svg
          xmlns="http://www.w3.org/2000/svg"
          class="h-6 w-6"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3"
          />
        </svg></button>
      </div>
      <div class="py-4 prose"><pre><code>{{ embedCode }}</code></pre></div>
      <div class="mt-2 capitalize">{{ t('labels.preview') }} : </div>
      <div
        class="inline-block mt-2"
        v-html="embedCode"
      />
    </label>
  </label>
</template>

<style scoped>

.dropdown-content.menu {
  width: fit-content !important;
}

</style>
