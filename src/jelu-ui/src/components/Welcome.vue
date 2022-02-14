<script setup lang="ts">
import { computed, onMounted, Ref, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { UserBook } from '../model/Book'
import { ReadingEventType, ReadingEventWithUserBook } from '../model/ReadingEvent'
import dataService from "../services/DataService"
import { key } from '../store'
import { StringUtils } from '../utils/StringUtils'
import BookCard from "./BookCard.vue"
import QuotesDisplay from './QuotesDisplay.vue'

const store = useStore(key)
const router = useRouter()

const isLogged = computed(() => {
    return store.state.isLogged
  })

const books: Ref<Array<UserBook>> = ref([]);

const events: Ref<Array<ReadingEventWithUserBook>> = ref([]);

const hasBooks = computed(() => books.value.length > 0)

const getCurrentlyReading = async () => {
  try {
    const res = await dataService.findUserBookByCriteria([ReadingEventType.CURRENTLY_READING], null)
    if (res.numberOfElements <= 6) {
      books.value = res.content
    }
    else {
      books.value = res.content.slice(0,6)
    }
  } catch (error) {
    console.log("failed get books : " + error)
  }

};

const nonCurrentlyReadingEvents: Array<ReadingEventType> = [ReadingEventType.DROPPED, ReadingEventType.FINISHED]

const getMyEvents = async () => {
  try {
    const res = await dataService.myReadingEvents(nonCurrentlyReadingEvents, 0, 8)
    const notCurrentlyReading = res.content.filter(e => e.eventType !== ReadingEventType.CURRENTLY_READING)
    events.value = notCurrentlyReading
  } catch (error) {
    console.log("failed get events : " + error)
  }

};

onMounted(() => {
  console.log("Component is mounted!");
});

const visibleAdvanced: Ref<boolean> = ref(false);

const hideAdvanced = () => {
  setTimeout(() => visibleAdvanced.value = false, 500)
}

const search = (searchterm: string) => {
  console.log(searchterm)
  if (StringUtils.isNotBlank(searchterm)) {
    router.push({ path: '/search', query: { title: searchterm } })
  }
}

const eventClass = (type: ReadingEventType) => {
    if (type === ReadingEventType.FINISHED) {
      return "is-info";
    } else if (type === ReadingEventType.DROPPED) {
      return "is-danger";
    } else if (
      type === ReadingEventType.CURRENTLY_READING
    ) {
      return "is-primary";
    } else return "";
};

if (isLogged.value) {
  try {
      getCurrentlyReading()
      getMyEvents()
  } catch (error) {
    console.log("failed get books : " + error);
  }
}

</script>

<template>
  <div v-if="isLogged">
    <div class="columns is-centered">
      <div class="column is-4">
        <o-field>
          <o-input
            placeholder="Search..."
            type="search" 
            icon="magnify"
            icon-clickable 
            icon-pack="mdi"
            @focus="visibleAdvanced = true"
            @blur="hideAdvanced"
            @keyup.enter="search($event.target.value)"
          />
        </o-field>
        <router-link
          v-if="visibleAdvanced"
          class="is-family-sans-serif is-size-7"
          :to="{ name: 'search' }"
        >
          Advanced search
        </router-link>
      </div>
    </div>
    <div v-if="hasBooks">
      <h2 class="title has-text-weight-normal typewriter">
        Currently reading :
      </h2>
      <div class="columns is-multiline is-variable is-4 is-centered">
        <div
          v-for="book in books"
          :key="book.id"
          class="column is-2 is-8-mobile is-offset-2-mobile"
        >
          <router-link
            v-if="book.id != undefined"
            :to="{ name: 'book-detail', params: { bookId: book.id } }"
          >
            <book-card :book="book" />
          </router-link>
        </div>
      </div>
    </div>
    <!-- logged, no books -->
    <div v-else>
      <h2 class="title has-text-weight-normal typewriter">
        Not currently reading anything
      </h2>
      <span class="icon is-large">
        <i class="mdi mdi-book-open-page-variant-outline mdi-48px" />
      </span>
    </div>
    <h2
      v-if="events.length > 0"
      class="title has-text-weight-normal typewriter pt-4"
    >
      recent reading events :
    </h2>
    <div
      v-if="events.length > 0"
      class="is-flex is-flex-wrap-wrap is-justify-content-center"
    >
      <div
        v-for="event in events"
        :key="event.id"
        class="books-grid-item m-1"
      >
        <div>
          <p>
            <span
              class="tag mb-1"
              :class="eventClass(event.eventType)"
            >{{ event.eventType }}</span>
          </p>
          <router-link
            v-if="event.userBook.id != undefined"
            :to="{ name: 'book-detail', params: { bookId: event.userBook.id } }"
          >
            <book-card :book="event.userBook" />
          </router-link>
        </div>
      </div>
    </div>
    <quotes-display />
  </div>
  <!-- not logged -->
  <div v-else>
    <p class="is-capitalized">
      Please log in first
    </p>
  </div>
</template>

<style lang="scss" scoped>


</style>
