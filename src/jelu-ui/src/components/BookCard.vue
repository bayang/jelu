<script setup lang="ts">
import { computed } from "vue";
import { UserBook } from "../model/Book";
import { ReadingEventType } from "../model/ReadingEvent";

const props = defineProps<{ book: UserBook }>();

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
      return "reading";
    } else return props.book.lastReadingEvent.toLowerCase();
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

</script>

<template>
  <div class="card card-compact bg-base-100 shadow-2xl shadow-base-300 rounded">
    <div>
      <figure>
        <img
          v-if="book.book.image"
          :src="'/files/' + book.book.image"
          alt="cover image"
        >
        <img
          v-else
          src="../assets/placeholder_asset.jpg"
          alt="cover placeholder"
        >
      </figure>
      <div
        v-if="showProgressBar(book)"
        class="bg-success absolute h-1.5"
        :style="{ width: book.percentRead + '%' }"
      />
    </div>
    <div class="card-body">
      <h2
        v-snip:js="4"
        v-tooltip="book.book.title" 
        class="card-title text-base"
      >
        {{ book.book.title }}
      </h2>
      <p
        v-if="book.book.authors != null && book.book.authors.length > 0"
        v-snip:js="3"
      >
        {{ authorsText }}
      </p>
      <div class="card-actions justify-end">
        <span
          v-if="book.lastReadingEvent"
          :class="eventClass"
          class="badge is-capitalized is-family-sans-serif"
        >{{ eventText }}</span>
        <div>
          <span
            v-if="book.owned"
            v-tooltip="'owned'"
            class="icon text-info"
          >
            <i class="mdi mdi-bookshelf mdi-18px" />
          </span>
          <span
            v-if="book.toRead"
            v-tooltip="'in read list'"
            class="icon text-info"
          >
            <i class="mdi mdi-eye mdi-18px" />
          </span>
          <slot name="icon" />
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>

</style>