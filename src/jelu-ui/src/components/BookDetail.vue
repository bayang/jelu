<script setup lang="ts">
import { computed, onMounted, Ref, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { UserBook } from '../model/Book'
import dataService from "../services/DataService"
import { DateUtils } from "../utils/DateUtils"

const props = defineProps<{ bookId: string }>()

const route = useRoute()
const book : Ref<UserBook|null> = ref(null)

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

const formattedDate = computed(() => {
    return DateUtils.formatDate(book.value?.book.publishedDate)
  })

// onMounted(() => {
//   console.log("Component book detail is mounted!");
//   try {
//     getBook()
//   } catch (error) {
//     console.log("failed get book : " + error);
//   }
// });

console.log('The id value is: ' + props.bookId)

getBook()

</script>

<template>
  <div class="columns is-multiline box">
    <div class="column is-one-quarter is-offset-one-quarter">
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
    <div class="column is-half">
      <h3 class="subtitle is-3 is-capitalized">{{book?.book?.title}}</h3>
      <p v-if="book?.book?.authors.length > 0" class="has-text-left"><span class="has-text-weight-semibold">Authors : </span></p>
      <ul v-if="book?.book?.authors.length > 0" class="has-text-left block">
        <li v-for="author in book.book.authors" v-bind:key="author.id">{{author.name}}</li>
      </ul>
      <p v-if="book?.book?.publisher" class="has-text-left block"><span class="has-text-weight-semibold">Publisher : </span>{{book.book.publisher}}</p>
      <p v-if="book?.book?.isbn10" class="has-text-left block"><span class="has-text-weight-semibold">ISBN10 : </span>{{book.book.isbn10}}</p>
      <p v-if="book?.book?.isbn13" class="has-text-left block"><span class="has-text-weight-semibold">ISBN13 : </span>{{book.book.isbn13}}</p>
      <p v-if="book?.book?.pageCount" class="has-text-left block"><span class="has-text-weight-semibold">Pages : </span>{{book.book.pageCount}}</p>
      <p v-if="book?.book?.publishedDate" class="has-text-left block"><span class="has-text-weight-semibold">Published date : </span>{{formattedDate}}</p>
      <p v-if="book?.book?.series" class="has-text-left block"><span class="has-text-weight-semibold">Series : </span>{{book.book.series}}</p>
      <p v-if="book?.book?.numberInSeries" class="has-text-left block"><span class="has-text-weight-semibold">Number in series : </span>{{book.book.numberInSeries}}</p>
      <div v-if="book?.owned || book?.toRead" class="field has-text-left">
      <o-checkbox v-if="book?.owned" v-model="book.owned" disabled>Owned</o-checkbox>
      <o-checkbox v-if="book?.toRead" v-model="book.toRead" disabled>To Read</o-checkbox>
    </div>

    </div>
    <div v-if="book?.book?.summary" class="column is-full is-offset-one-quarter">
      <p v-if="book?.book?.summary" class="has-text-left has-text-weight-semibold">Summary :</p>
      <p v-if="book?.book?.summary" class="has-text-left">{{book.book.summary}}</p>

    </div>
    <div v-if="book?.personalNotes" class="column is-full is-offset-one-quarter">
      <p v-if="book?.personalNotes" class="has-text-left  has-text-weight-semibold">Personal Notes :</p>
      <p v-if="book?.personalNotes" class="has-text-left">{{book.personalNotes}}</p>
    </div>
  </div>
</template>

<style scoped>
a {
  color: #42b983;
}

label {
  margin: 0 0.5em;
  font-weight: bold;
}

code {
  background-color: #eee;
  padding: 2px 4px;
  border-radius: 4px;
  color: #304455;
}

.columns {
  margin-top: 10px;
}
</style>
