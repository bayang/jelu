<script setup lang="ts">
import { computed, ref } from "vue";
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
</script>

<template>
  <router-link :to="{ name: 'book-detail', params: { bookId: book.id } }">
    <div class="card">
      <div class="card-image">
        <figure class="image is-3by4">
          <img
            v-if="book.book.image"
            :src="'/files/' + book.book.image"
            alt="cover image"
          />
          <img
            v-else
            src="../assets/placeholder_asset.png"
            alt="cover placeholder"
          />
        </figure>
      </div>
      <header class="card-header">
        <p class="card-header-title is-capitalized is-family-sans-serif">
          <span class="icon has-text-primary">
            <i class="mdi mdi-book-open-blank-variant mdi-18px"></i>
          </span>
          {{ book.book.title }}
        </p>
      </header>
      <div class="card-content has-text-dark">
        <div class="content has-text-left">
          <span v-if="book.book.authors.length > 0" class="is-inline-block"
            >Authors : {{ authorsText }}</span
          >
          <span v-if="book.book.publisher" class="is-inline-block"
            >Publisher : {{ book.book.publisher }}</span
          >
        </div>
        <footer class="card-footer">
          <div class="tags has-addons ">
            <span v-if="book.lastReadingEvent" class="tag is-family-sans-serif">Status</span>
            <span
              v-if="book.lastReadingEvent"
              :class="eventClass"
              class="tag is-capitalized is-family-sans-serif"
              >{{ eventText }}</span
            >
          </div>
        </footer>
      </div>
    </div>
  </router-link>
</template>

<style lang="scss" scoped>
@import "../assets/style.scss";

$tag-color: findColorInvert($card-background-color);

header a {
  color: findColorInvert($card-background-color);
}

footer.card-footer span.tag {
  color: $tag-color;
}

.card-content .content {
  font-size: $small-font-size;
}
</style>
