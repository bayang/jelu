<script setup lang="ts">
import { computed, onMounted, Ref, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { UserBook } from '../model/Book'
import { CreateReadingEvent, ReadingEvent, ReadingEventType, ReadingEventWithUserBook } from '../model/ReadingEvent'
import dataService from "../services/DataService"
import { key } from '../store'
import { StringUtils } from '../utils/StringUtils'
import BookCard from "./BookCard.vue"
import QuotesDisplay from './QuotesDisplay.vue'
import { useProgrammatic } from "@oruga-ui/oruga-next";
import ReadingEventModalVue from './ReadingEventModal.vue'
import { useTitle } from '@vueuse/core'

useTitle('Jelu | Home')

const store = useStore(key)
const router = useRouter()
const {oruga} = useProgrammatic();

const isLogged = computed(() => {
    return store != null && store != undefined && store.getters.getLogged
  })

const showModal: Ref<boolean> = ref(false)

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
      return "badge-info";
    } else if (type === ReadingEventType.DROPPED) {
      return "badge-error";
    } else if (
      type === ReadingEventType.CURRENTLY_READING
    ) {
      return "badge-success";
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

</script>

<template>
  <div v-if="isLogged">
    <div class="flex flex-row justify-center">
      <div class="basis-10/12 sm:basis-1/3">
        <o-field>
          <o-input
            placeholder="Search..."
            type="search" 
            icon="magnify"
            icon-clickable 
            icon-pack="mdi"
            class="input focus:input-accent"
            @focus="visibleAdvanced = true"
            @blur="hideAdvanced"
            @keyup.enter="search($event.target.value)"
          />
        </o-field>
        <router-link
          v-if="visibleAdvanced"
          class="link-hover font-sans"
          :to="{ name: 'search' }"
        >
          Advanced search
        </router-link>
      </div>
    </div>
    <div v-if="hasBooks">
      <h2 class="typewriter text-3xl py-4">
        Currently reading :
      </h2>
      <div class="flex flex-row flex-wrap justify-center gap-3">
        <div
          v-for="book in books"
          :key="book.id"
          class="sm:basis-1/6 basis-8/12"
        >
          <router-link
            v-if="book.id != undefined"
            :to="{ name: 'book-detail', params: { bookId: book.id } }"
          >
            <book-card :book="book">
              <template #icon>
                <span
                  v-tooltip="'Mark read or drop'"
                  class="icon has-text-info text-info"
                  @click.prevent="toggleReadingEventModal(defaultCreateEvent(book.book.id!!), false)"
                >
                  <i class="mdi mdi-check-circle mdi-18px" />
                </span>
              </template>
            </book-card>
          </router-link>
        </div>
      </div>
    </div>
    <!-- logged, no books -->
    <div v-else>
      <h2 class="text-3xl typewriter">
        Not currently reading anything
      </h2>
      <span class="icon is-large">
        <i class="mdi mdi-book-open-page-variant-outline mdi-48px" />
      </span>
    </div>
    <h2
      v-if="events.length > 0"
      class="text-3xl typewriter py-4"
    >
      recent reading events :
    </h2>
    <div
      v-if="events.length > 0"
      class="grid grid-cols-2 sm:grid-cols-8 gap-1"
    >
      <div
        v-for="event in events"
        :key="event.id"
        class="m-1"
      >
        <div>
          <p>
            <span
              class="badge mb-1"
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
    <quotes-display v-if="isLogged" />
  </div>
  <!-- not logged -->
  <div v-else>
    <p class="capitalize">
      Please log in first
    </p>
  </div>
</template>

<style lang="scss" scoped>


</style>
