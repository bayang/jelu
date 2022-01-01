<script setup lang="ts">
import { computed, onMounted, Ref, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { UserBook } from '../model/Book'
import dataService from "../services/DataService"
import { DateUtils } from "../utils/DateUtils"
import EditBookModal from "./EditBookModal.vue"
import { useProgrammatic } from "@oruga-ui/oruga-next";
import dayjs from 'dayjs'

const props = defineProps<{ bookId: string }>()

const route = useRoute()
const {oruga} = useProgrammatic();
console.log(oruga)
const book : Ref<UserBook|null> = ref(null)
const edit: Ref<Boolean> = ref(false)

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

const sortedEvents = computed(() => book.value?.readingEvents?.sort((a, b) => dayjs(a.creationDate).isAfter(dayjs(b.creationDate)) ? -1 : 1))

const hasExternalLink = computed(() => book.value?.book.amazonId != null 
                                        || book.value?.book.goodreadsId != null
                                        || book.value?.book.googleId != null
                                        || book.value?.book.librarythingId != null)

const format = (dateString: string|null|undefined) => {
  if (dateString != null) {
    return DateUtils.formatDate(dateString)
  }
  return ''
}

function modalClosed(args: any) {
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
            "book" : book.value
          },
          onClose: modalClosed
        });
}

console.log('The id value is: ' + props.bookId)

getBook()

</script>

<template>
  <div class="columns is-multiline box">
    <div class="column is-centered is-four-fifths">
<h3 class="subtitle is-3 is-capitalized">{{book?.book?.title}}</h3>
    </div>
    <div v-if="book != null" class="column is-one-fifth">
<button @click="toggleEdit" class="button is-primary is-light">
  <span class="icon">
      <i class="mdi mdi-pencil"></i>
    </span>
  <span>Edit</span>
  </button>
    </div>
    <div class="column is-one-fifth is-offset-one-fifth">
      <figure class="image is-3by4">
          <img
            v-if="book?.book?.image"
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
    <div class="column is-three-fifths content">
      <p v-if="book != null && book.book != null && book.book.authors != null && book?.book?.authors?.length > 0" class="has-text-left"><span class="has-text-weight-semibold">Authors : </span></p>
      <ul v-if="book != null && book.book != null && book.book.authors != null && book?.book?.authors?.length > 0" class="has-text-left block">
        <li v-for="author in book?.book?.authors" v-bind:key="author.id">{{author.name}}</li>
      </ul>
      <p v-if="book?.book?.publisher" class="has-text-left block"><span class="has-text-weight-semibold">Publisher : </span>{{book.book.publisher}}</p>
      <p v-if="book?.book?.isbn10" class="has-text-left block"><span class="has-text-weight-semibold">ISBN10 : </span>{{book.book.isbn10}}</p>
      <p v-if="book?.book?.isbn13" class="has-text-left block"><span class="has-text-weight-semibold">ISBN13 : </span>{{book.book.isbn13}}</p>
      <p v-if="book?.book?.pageCount" class="has-text-left block"><span class="has-text-weight-semibold">Pages : </span>{{book.book.pageCount}}</p>
      <p v-if="book?.book?.publishedDate" class="has-text-left block"><span class="has-text-weight-semibold">Published date : </span>{{format(book.book.publishedDate)}}</p>
      <p v-if="book?.book?.series" class="has-text-left block"><span class="has-text-weight-semibold">Series : </span>{{book.book.series}}</p>
      <p v-if="book?.book?.numberInSeries" class="has-text-left block"><span class="has-text-weight-semibold">Number in series : </span>{{book.book.numberInSeries}}</p>
      <div v-if="book?.owned || book?.toRead" class="field has-text-left">
      <o-checkbox v-if="book?.owned" v-model="book.owned" disabled>Owned</o-checkbox>
      <o-checkbox v-if="book?.toRead" v-model="book.toRead" disabled>To Read</o-checkbox>
    </div>

    </div>
    <div v-if="book?.book?.summary" class="column is-three-fifths is-offset-one-quarter content jelu-bordered">
      <p v-if="book?.book?.summary" class="has-text-left has-text-weight-semibold">Summary :</p>
      <p v-if="book?.book?.summary" class="has-text-left" v-html="book.book.summary"></p>
    </div>
    <div class="column is-full is-offset-one-quarter content tags has-text-left  has-text-weight-semibold">
      <span class="tag is-primary is-light" v-for="tag in book?.book?.tags" v-bind:key="tag.id"><router-link :to="{ name: 'tag-detail', params: { tagId: tag.id } }">{{tag.name}}&nbsp;</router-link></span>
    </div>
    <div v-if="hasExternalLink" class="column is-full is-offset-one-quarter content tags has-text-left has-text-weight-semibold">
      <span class="tag is-warning" v-if="book?.book.goodreadsId"><a :href="'https://www.goodreads.com/book/show/' + book.book.goodreadsId" target="_blank">goodreads</a></span>
      <span class="tag is-warning" v-if="book?.book.googleId"><a :href="'https://books.google.com/books?id=' + book.book.googleId" target="_blank">google</a></span>
      <span class="tag is-warning" v-if="book?.book.amazonId"><a :href="'https://www.amazon.com/dp/' + book.book.amazonId" target="_blank">amazon</a></span>
      <span class="tag is-warning" v-if="book?.book.librarythingId"><a :href="'https://www.librarything.com/work/' + book.book.librarythingId" target="_blank">librarything</a></span>
    </div>
    <div v-if="book?.personalNotes" class="column is-full is-offset-one-quarter content">
      <p v-if="book?.personalNotes" class="has-text-left  has-text-weight-semibold">Personal Notes :</p>
      <p v-if="book?.personalNotes" class="has-text-left">{{book.personalNotes}}</p>
    </div>
    <div v-if="book?.readingEvents != null && book?.readingEvents?.length > 0" class="column is-full is-offset-one-quarter content">
      <p v-if="book?.readingEvents != null && book?.readingEvents?.length > 0" class="has-text-left has-text-weight-semibold">Reading events :</p>
      <ul class="has-text-left" v-if="book?.readingEvents != null && book?.readingEvents?.length > 0">
        <li v-for="event in sortedEvents" v-bind:key="event.id">{{event.eventType}} - {{format(event.creationDate)}}</li>
      </ul>
    </div>
  </div>

</template>

<style lang="scss">
@import "../assets/style.scss";

.columns {
  margin-top: 10px;
}

.jelu-bordered {
  border-width: 2px;
  border-color: $jelu_background_contrast;
  border-style: solid;
  border-radius: 3px;
  -webkit-box-shadow: 10px 10px 10px 0px $jelu_background; 
  box-shadow: 10px 10px 10px 0px $jelu_background;
}

$tag-color: findColorInvert($tag-background-color);

span.tag a {
  color: $tag-color;
}

</style>
