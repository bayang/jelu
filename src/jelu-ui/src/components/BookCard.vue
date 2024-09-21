<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next";
import { computed, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { UserBook } from "../model/Book";
import { ReadingEventType } from "../model/ReadingEvent";
import EditBookModal from "./EditBookModal.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })
const oruga = useOruga();

const props = defineProps<{ 
  book: UserBook, 
  size?: string, 
  forceSelect: boolean,
  showSelect: boolean,
  proposeAdd: boolean,
  seriesId?: string,
}>();
const emit = defineEmits<{
  (e: 'update:modalClosed', open: boolean): void,
  (e: 'update:checked', id: string|null, checked: boolean): void
}>()

const checked: Ref<boolean> = ref(false)

watch(() => props.forceSelect, (newVal, oldVal) => {
  console.log("props.forceSelect changed")
  console.log(props.forceSelect)
  console.log(newVal + " " + oldVal)
  checked.value = props.forceSelect
})

const eventClass = computed(() => {
  if (props.book.lastReadingEvent) {
    if (props.book.lastReadingEvent === ReadingEventType.FINISHED) {
      return "badge-info";
    } else if (props.book.lastReadingEvent === ReadingEventType.DROPPED) {
      return "badge-error";
    } else if (
      props.book.lastReadingEvent === ReadingEventType.CURRENTLY_READING
    ) {
      return "badge-success";
    } else return "";
  }
  return "";
});

const eventText = computed(() => {
  if (props.book.lastReadingEvent) {
    if (props.book.lastReadingEvent === ReadingEventType.CURRENTLY_READING) {
      return t('reading_events.reading');
    } else if (props.book.lastReadingEvent === ReadingEventType.DROPPED) {
      return t('reading_events.dropped');
    } else if (props.book.lastReadingEvent === ReadingEventType.FINISHED) {
      return t('reading_events.finished');
    }
  }
  return "";
});

const authorsText = computed(() => {
  let txt = "";
  if (props.book.book.authors && props.book.book.authors.length > 0) {
    let first = true;
    for (let author of props.book.book.authors) {
      if (first) {
        txt += "";
        first = false;
      } else {
        txt += ", ";
      }
      txt += author.name;
    }
  }
  return txt;
});

const showProgressBar = (book: UserBook) => {
  return book.percentRead 
      && book.percentRead > 0 
      && book.lastReadingEvent != null 
      && book.lastReadingEvent === ReadingEventType.CURRENTLY_READING
}

const progressBarTooltip = computed(() => {
  return props.book.currentPageNumber != null ? `p. ${props.book.currentPageNumber}` : `${props.book.percentRead} %`
})

const currentSeries = computed(() => {
  if (props.book.book.series != null &&      props.book.book.series?.length > 0) {
    if (props.seriesId != null) {
      return props.book.book.series?.find(s => s.seriesId === props.seriesId)
    } else {
      return props.book.book.series[0]
    }
  }
  return null
})

function modalClosed() {
  console.log("modal closed from card")
  emit("update:modalClosed", true)
}

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

watch(checked, (newVal, oldVal) => {
  console.log(props.book.id + " " + checked.value)
  emit("update:checked", props.book.id != null ? props.book.id : null , checked.value)
})


</script>

<template>
  <div
    class="card card-compact bg-base-100 shadow-2xl shadow-base-300"
    @dblclick="toggleEdit(book)"
  >
    <div>
      <router-link
        v-if="book.id != null"
        :to="{ name: 'book-detail', params: { bookId: book.id } }"
      >
        <figure>
          <img
            v-if="book.book.image"
            :src="'/files/' + book.book.image"
            alt="cover image"
            class="object-fill"
            :class="props.size === 'xl' ? 'h-96' : 'h-72'"
          >
          <img
            v-else
            src="../assets/placeholder_asset.jpg"
            alt="cover placeholder"
            class="h-72 object-fill"
          >
        </figure>
      </router-link>
      <router-link
        v-else
        :to="{ name: 'book-reviews', params: { bookId: book.book.id } }"
      >
        <figure>
          <img
            v-if="book.book.image"
            :src="'/files/' + book.book.image"
            alt="cover image"
            class="object-fill"
            :class="props.size === 'xl' ? 'h-96' : 'h-72'"
          >
          <img
            v-else
            src="../assets/placeholder_asset.jpg"
            alt="cover placeholder"
            class="h-72 object-fill"
          >
        </figure>
      </router-link>
      <div
        v-if="showProgressBar(book)"
        v-tooltip="progressBarTooltip"
        class="bg-success absolute h-1.5"
        :style="{ width: book.percentRead + '%' }"
      />
      <div
        v-if="book.id != null && props.showSelect"
        class="absolute top-0 left-1"
      >
        <input
          v-model="checked"
          type="checkbox"
          class="checkbox checkbox-accent rounded-full border-2 hover:border-4"
        >
      </div>
    </div>
    <div class="card-body">
      <router-link
        v-if="book.id != null"
        :to="{ name: 'book-detail', params: { bookId: book.id } }"
      >
        <h2
          v-tooltip="book.book.title"
          class="card-title text-base max-h-11 line-clamp-2 hover:link"
        >
          {{ book.book.title }}
        </h2>
      </router-link>
      <router-link
        v-else
        :to="{ name: 'book-reviews', params: { bookId: book.book.id } }"
      >
        <h2
          v-tooltip="book.book.title"
          class="card-title text-base max-h-11 line-clamp-2 hover:link"
        >
          {{ book.book.title }}
        </h2>
      </router-link>
      <div v-if="book.book.authors != null && book.book.authors.length > 0">
        <span
          v-for="author in book.book.authors.slice(0,3)"
          :key="author.id"
        >
          <router-link
            class="link hover:underline hover:decoration-4 hover:decoration-secondary line-clamp-2 inline-block"
            :to="{ name: 'author-detail', params: { authorId: author.id } }"
          >
            {{ author.name }}
          </router-link>
          <span>&nbsp;</span>
        </span>
        <span
          v-if="book.book.authors.length > 3"
          v-tooltip="authorsText"
        >&#8230;</span>
      </div>
      <div class="card-actions justify-end">
        <span
          v-if="book.lastReadingEvent"
          :class="eventClass"
          class="badge"
        >{{ eventText }}</span>
        <div class="flex">
          <router-link
            v-if="currentSeries != null"
            v-tooltip="currentSeries.name"
            class="badge mx-1"
            :to="{ name: 'series', params: { seriesId: currentSeries.seriesId } }"
          >
            #{{ currentSeries.numberInSeries }}
          </router-link>
          <span
            v-if="book.userAvgRating"
            v-tooltip="t('labels.user_avg_rating', {rating : book.userAvgRating})"
            class="icon text-info"
          >
            <i class="mdi mdi-star mdi-18px" />
            {{ book.userAvgRating }}
          </span>
          <span
            v-if="book.avgRating"
            v-tooltip="t('labels.avg_rating', {rating : book.avgRating})"
            class="icon text-info"
          >
            <i class="mdi mdi-star-outline mdi-18px" />
            {{ book.avgRating }}
          </span>
          <span
            v-if="book.owned"
            v-tooltip="t('book.owned')"
            class="icon text-info"
          >
            <i class="mdi mdi-bookshelf mdi-18px" />
          </span>
          <span
            v-if="book.toRead"
            v-tooltip="t('book.in_read_list')"
            class="icon text-info"
          >
            <i class="mdi mdi-eye mdi-18px" />
          </span>
          <span
            v-if="proposeAdd === true && book.id == null"
            v-tooltip="t('labels.book_not_yet_in_books')"
            class="icon text-error"
          >
            <i class="mdi mdi-plus-circle mdi-18px" />
          </span>
          <slot name="icon" />
          <slot name="date" />
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>

</style>
