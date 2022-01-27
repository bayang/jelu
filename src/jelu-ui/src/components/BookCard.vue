<script setup lang="ts">
import { computed } from "vue";
import { UserBook } from "../model/Book";
import { ReadingEventType } from "../model/ReadingEvent";

const props = defineProps<{ book: UserBook }>();

const eventClass = computed(() => {
  if (props.book.lastReadingEvent) {
    if (props.book.lastReadingEvent === ReadingEventType.FINISHED) {
      return "is-info";
    } else if (props.book.lastReadingEvent === ReadingEventType.DROPPED) {
      return "is-danger";
    } else if (
      props.book.lastReadingEvent === ReadingEventType.CURRENTLY_READING
    ) {
      return "is-primary";
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
  <div class="card">
    <div class="card-image">
      <figure class="image is-3by4">
        <img
          v-if="book.book.image"
          :src="'/files/' + book.book.image"
          alt="cover image"
        >
        <img
          v-else
          src="../assets/placeholder_asset.png"
          alt="cover placeholder"
        >
      </figure>
      <div
        v-if="showProgressBar(book)"
        class="jelu-progress"
        :style="{ width: book.percentRead + '%' }"
      />
    </div>
    <header class="card-header">
      <p
        v-snip:js="4" 
        class="p-3 card-header-title is-capitalized is-family-sans-serif"
      >
        {{ book.book.title }}
      </p>
    </header>
    <div class="card-content has-text-dark py-2">
      <div class="content has-text-left m-0">
        <p
          v-if="book.book.authors != null && book.book.authors.length > 0"
          v-snip:js="3"
          class="is-inline-block"
        >
          {{ authorsText }}
        </p>
      </div>
      <footer class="card-footer">
        <div>
          <div
            v-if="book.lastReadingEvent"
            class="m-0 tags has-addons "
          >
            <span class="tag is-family-sans-serif">Status</span>
            <span
              :class="eventClass"
              class="tag is-capitalized is-family-sans-serif"
            >{{ eventText }}</span>
          </div>
          <div>
            <o-tooltip
              v-if="book.owned"
              label="owned"
              variant="info"
            >
              <span
                v-if="book.owned"
                class="icon has-text-info"
              >
                <i class="mdi mdi-bookshelf mdi-18px" />
              </span>
            </o-tooltip>
            <o-tooltip
              v-if="book.toRead"
              label="in read list"
              variant="info"
            >
              <span
                v-if="book.toRead"
                class="icon has-text-info"
              >
                <i class="mdi mdi-eye mdi-18px" />
              </span>
            </o-tooltip>
            <slot name="icon" />
          </div>
        </div>
      </footer>
    </div>
  </div>
</template>

<style lang="scss" scoped>

</style>
