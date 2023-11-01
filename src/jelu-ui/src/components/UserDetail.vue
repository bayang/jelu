<script setup lang="ts">
import { useTitle } from '@vueuse/core';
import { computed, Ref, ref, watch } from "vue";
import Avatar from 'vue-avatar-sdh';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import useEvents from '../composables/events';
import { UserBook } from "../model/Book";
import { ReadingEventType, ReadingEventWithUserBook } from '../model/ReadingEvent';
import { Review } from '../model/Review';
import { User } from '../model/User';
import dataService from "../services/DataService";
import BookCard from "./BookCard.vue";
import ReviewBookCard from './ReviewBookCard.vue';

const route = useRoute()

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})
const { eventClass, eventLabel } = useEvents()

const user: Ref<User> = ref({login: '', isAdmin: false})
const currentlyReading: Ref<Array<UserBook>> = ref([]);
const events: Ref<Array<ReadingEventWithUserBook>> = ref([]);
const toRead: Ref<Array<UserBook>> = ref([]);
const userReviews: Ref<Array<Review>> = ref([]);

const currentlyReadingIsLoading: Ref<boolean> = ref(false)
const recentEventsIsLoading: Ref<boolean> = ref(false)
const getToReadIsLoading: Ref<boolean> = ref(false)

watch(() => route.params.userId, (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal && route.params.userId !== undefined) {
    getUser()
  }
})

const getUser = async () => {
  try {
    user.value = await dataService.getUserById(route.params.userId as string)
    useTitle('Jelu | User')
  } catch (error) {
    console.log("failed get user : " + error);
  }
};
const getCurrentlyReading = async () => {
  currentlyReadingIsLoading.value = true
  try {
    const res = await dataService.findUserBookByCriteria([ReadingEventType.CURRENTLY_READING], null, route.params.userId as string, null)
    currentlyReading.value = res.content
    currentlyReadingIsLoading.value = false
  } catch (error) {
    console.log("failed get books : " + error)
    currentlyReadingIsLoading.value = false
  }
};

const nonCurrentlyReadingEvents: Array<ReadingEventType> = [ReadingEventType.DROPPED, ReadingEventType.FINISHED]

const getReadEvents = async () => {
  recentEventsIsLoading.value = true
  try {
    const res = await dataService.findReadingEvents(nonCurrentlyReadingEvents, route.params.userId as string, undefined, undefined, undefined, undefined, undefined, 0, undefined, "end_date:desc")
    events.value = res.content
    recentEventsIsLoading.value = false
  } catch (error) {
    console.log("failed get events : " + error)
    recentEventsIsLoading.value = false
  }
};

const getToRead = async () => {
  getToReadIsLoading.value = true
  try {
    const res = await dataService.findUserBookByCriteria(
      null, null, route.params.userId as string,
    true, null, null,
    0, 20, undefined)
    toRead.value = res.content
    getToReadIsLoading.value = false
  } catch (error) {
    console.log("failed get books : " + error);
    getToReadIsLoading.value = false
  }
};

const getUserReviews = async () => {
  try {
    const res = await dataService.findReviews(
      route.params.userId as string, undefined, null,
    null, null,
    0, 20, null)
    userReviews.value = res.content
  } catch (error) {
    console.log("failed get reviews : " + error);
  }
};

const toUserBook = (book: UserBook): UserBook => {
    const converted = {
      book: book.book
    } as UserBook
    return converted
  }

const convertedBooks = computed(() => currentlyReading.value?.map(b => toUserBook(b)))
const convertedBooksToRead = computed(() => toRead.value?.map(b => toUserBook(b)))

getUser()
getCurrentlyReading()
getReadEvents()
getToRead()
getUserReviews()
</script>

<template>
  <section class="">
    <div class="w-full flex flex-wrap justify-center items-center gap-2">
      <Avatar
        :username="user.login"
      />
      <strong>{{ user.login }}</strong>
    </div>
    <h2
      v-if="currentlyReading.length > 0"
      class="text-3xl typewriter py-4"
    >
      {{ t('home.currently_reading') }}
    </h2>
    <div
      v-if="currentlyReading.length > 0"
      class="flex flex-nowrap overflow-x-auto"
    >
      <div
        v-for="book in convertedBooks"
        :key="book.id"
        class="m-1 pb-6 w-56 shrink-0 grow-0"
      >
        <book-card
          :book="book"
          class="h-full"
          :force-select="false"
          :show-select="false"
          :propose-add="false"
        />
      </div>
    </div>
    <div
      v-else-if="currentlyReadingIsLoading"
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
      v-if="events.length > 0"
      class="text-3xl typewriter py-4"
    >
      <router-link

        class="link text-3xl typewriter py-4"
        :to="{ name: 'my-books', query: { userId: route.params.userId } }"
      >
        {{ t('home.recent_events') }}
      </router-link>
    </h2>
    <div
      v-if="events.length > 0"
      class="flex flex-nowrap overflow-x-auto"
    >
      <div
        v-for="event in events"
        :key="event.id"
        class="m-1 pb-6 w-56 shrink-0 grow-0"
      >
        <div class="h-full">
          <p>
            <span
              class="badge mb-1"
              :class="eventClass(event.eventType)"
            >{{ eventLabel(event.eventType) }}</span>
          </p>
          <book-card
            :book="toUserBook(event.userBook)"
            class="h-full"
            :force-select="false"
            :show-select="false"
            :propose-add="false"
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
      v-if="toRead.length > 0"
      class="text-3xl typewriter py-4"
    >
      <router-link

        class="link text-3xl typewriter py-4"
        :to="{ name: 'to-read', query: { userId: route.params.userId } }"
      >
        {{ t('nav.to_read') }}
      </router-link>
    </h2>
    <div
      v-if="toRead.length > 0"
      class="flex flex-nowrap overflow-x-auto"
    >
      <div
        v-for="book in convertedBooksToRead"
        :key="book.id"
        class="m-1 pb-6 w-56 shrink-0 grow-0"
      >
        <book-card
          :book="book"
          class="h-full"
          :force-select="false"
          :show-select="false"
          :propose-add="false"
        />
      </div>
    </div>
    <div
      v-else-if="getToReadIsLoading"
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
      class="text-3xl typewriter py-4"
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
          :book-reviews-link="false"
          :show-user-name="false"
        />
      </div>
    </div>
  </section>
</template>

<style lang="scss">
</style>
