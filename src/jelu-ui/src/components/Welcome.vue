<script setup lang="ts">
import { useProgrammatic } from "@oruga-ui/oruga-next"
import { useTitle } from '@vueuse/core'
import { computed, Ref, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import useEvents from "../composables/events"
import { UserBook } from '../model/Book'
import { CreateReadingEvent, ReadingEvent, ReadingEventType, ReadingEventWithUserBook } from '../model/ReadingEvent'
import { Review } from "../model/Review"
import dataService from "../services/DataService"
import { key } from '../store'
import BookCard from "./BookCard.vue"
import QuotesDisplay from './QuotesDisplay.vue'
import ReadingEventModalVue from './ReadingEventModal.vue'
import ReviewBookCard from './ReviewBookCard.vue';

useTitle('Jelu | Home')

const store = useStore(key)
const {oruga} = useProgrammatic()
const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })
const { eventClass, eventLabel } = useEvents()

const isLogged = computed(() => {
    return store != null && store != undefined && store.getters.getLogged
  })

const initialLoad : Ref<boolean> = ref(true)

const showModal: Ref<boolean> = ref(false)

const currentlyReadingIsLoading: Ref<boolean> = ref(false)

const recentEventsIsLoading: Ref<boolean> = ref(false)

const books: Ref<Array<UserBook>> = ref([]);

const events: Ref<Array<ReadingEventWithUserBook>> = ref([]);

const hasBooks = computed(() => books.value.length > 0)

const userReviews: Ref<Array<Review>> = ref([]);

const getCurrentlyReading = async () => {
  currentlyReadingIsLoading.value = true
  try {
    const res = await dataService.findUserBookByCriteria([ReadingEventType.CURRENTLY_READING], null, null, null)
    if (res.numberOfElements <= 6) {
      books.value = res.content
    }
    else {
      books.value = res.content.slice(0,6)
    }
    currentlyReadingIsLoading.value = false
  } catch (error) {
    console.log("failed get books : " + error)
    currentlyReadingIsLoading.value = false
  }

};

const nonCurrentlyReadingEvents: Array<ReadingEventType> = [ReadingEventType.DROPPED, ReadingEventType.FINISHED]

const getMyEvents = async () => {
  recentEventsIsLoading.value = true
  try {
    const res = await dataService.myReadingEvents(nonCurrentlyReadingEvents, undefined, undefined, undefined, undefined, undefined, 0, 8)
    const notCurrentlyReading = res.content.filter(e => e.eventType !== ReadingEventType.CURRENTLY_READING)
    events.value = notCurrentlyReading
    recentEventsIsLoading.value = false
  } catch (error) {
    console.log("failed get events : " + error)
    recentEventsIsLoading.value = false
  }
};

const getUserReviews = async () => {
  try {
    const res = await dataService.findReviews(
      undefined, undefined, null,
    null, null,
    0, 20, null)
    userReviews.value = res.content
  } catch (error) {
    console.log("failed get reviews : " + error);
  }
};

if (isLogged.value) {
  try {
      getCurrentlyReading()
      getMyEvents()
      getUserReviews()
  } catch (error) {
    console.log("failed get books : " + error);
  }
}

watch(() => isLogged.value, (newValue, oldValue) => {
  console.log('logged changed ' + isLogged.value)
  if (initialLoad.value && isLogged.value) {
    try {
      initialLoad.value = false
      getCurrentlyReading()
      getMyEvents()
  } catch (error) {
    console.log("failed get books : " + error);
  }
  }
})

function modalClosed() {
  console.log("modal closed")
  getCurrentlyReading()
}

function defaultCreateEvent(bookId: string): CreateReadingEvent {
  return {
  eventType: ReadingEventType.FINISHED, 
  eventDate: new Date(),
  bookId: bookId
}
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

</script>

<template>
  <div v-if="isLogged">
    <div v-if="hasBooks">
      <h2 class="typewriter text-3xl pb-3">
        {{ t('home.currently_reading') }} :
      </h2>
      <div class="flex flex-row flex-wrap justify-center gap-3">
        <div
          v-for="book in books"
          :key="book.id"
          class="sm:basis-5/12 md:basis-1/3 lg:basis-1/4 xl:basis-1/6 basis-8/12"
        >
          <book-card
            :book="book"
            size="xl"
            :force-select="false"
            :show-select="false"
            :propose-add="true"
          >
            <template #icon>
              <span
                v-tooltip="t('labels.mark_read_or_drop')"
                class="icon text-info"
                @click.prevent="toggleReadingEventModal(defaultCreateEvent(book.book.id!!), false)"
              >
                <i class="mdi mdi-check-circle mdi-18px" />
              </span>
            </template>
          </book-card>
        </div>
      </div>
    </div>
    <div
      v-else-if="currentlyReadingIsLoading"
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
    <!-- logged, no books -->
    <div v-else>
      <h2 class="text-3xl typewriter">
        {{ t('home.not_reading') }}
      </h2>
      <span class="icon is-large">
        <i class="mdi mdi-book-open-page-variant-outline mdi-48px" />
      </span>
    </div>
    <h2
      v-if="events.length > 0"
      class="text-3xl typewriter py-4"
    >
      {{ t('home.recent_events') }} :
    </h2>
    <div
      v-if="events.length > 0"
      class="grid grid-cols-2 md:grid-cols-4 xl:grid-cols-6 2xl:grid-cols-8 gap-1"
    >
      <div
        v-for="event in events"
        :key="event.id"
        class="m-1 pb-6"
      >
        <div class="h-full">
          <p>
            <span
              class="badge mb-1"
              :class="eventClass(event.eventType)"
            >{{ eventLabel(event.eventType) }}</span>
          </p>
          <book-card
            :book="event.userBook"
            class="h-full"
            :force-select="false"
            :show-select="false"
            :propose-add="true"
          />
        </div>
      </div>
    </div>
    <div
      v-else-if="recentEventsIsLoading"
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
    <h2
      v-if="userReviews.length > 0"
      class="text-3xl typewriter py-4 capitalize"
    >
      {{ t('reviews.review', 2) }}
    </h2>
    <div
      v-if="userReviews.length > 0"
      class="flex flex-nowrap overflow-x-auto mb-10"
    >
      <div
        v-for="review in userReviews"
        :key="review.id"
        class="m-1 pb-6 shrink-0 grow-0"
      >
        <ReviewBookCard
          :review="review"
          :book-reviews-link="true"
          :show-user-name="true"
        />
      </div>
    </div>
    <quotes-display v-if="isLogged" />
  </div>
  <!-- not logged -->
  <div v-else>
    <p class="capitalize">
      {{ t('user.log_first') }}
    </p>
  </div>
</template>

<style lang="scss" scoped>

</style>
