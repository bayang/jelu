<script setup lang="ts">
import { Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { Book } from "../model/Book";
import { Visibility } from "../model/Review";
import { BookQuote } from "../model/BookQuote"
import dataService from "../services/DataService";

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const props = defineProps<{
  book: Book,
  edit: boolean,
  bookQuote: BookQuote | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const progress: Ref<boolean> = ref(false)

const visibility: Ref<Visibility> = ref(props.edit != null && props.edit === true && props.bookQuote?.visibility != null ? props.bookQuote?.visibility : Visibility.PRIVATE)
const quoteText = ref(props.edit != null && props.edit === true && props.bookQuote?.text != null ? props.bookQuote?.text : "")
const position = ref(props.edit != null && props.edit === true && props.bookQuote?.position != null ? props.bookQuote?.position : "")

watch(visibility, (newVal, oldVal) => {
  console.log("visibilty " + visibility.value)
})

const submit = () => {
  console.log("submit ")
  if (props.book.id != null) {
    progress.value = true
    dataService.saveBookQuote({
      bookId: props.book.id,
      text: quoteText.value,
      visibility: visibility.value,
      position: position.value
    })
      .then(res => {
        progress.value = false
        emit('close')
      })
      .catch(err => {
        progress.value = false
        console.log(err)
      })
  }
}

const editBookQuote = () => {
  console.log("edit ")
  if (props.book.id != null && props.bookQuote?.id != null) {
    progress.value = true
    dataService.updateBookQuote(props.bookQuote.id, {
      text: quoteText.value,
      visibility: visibility.value,
      position: position.value
    })
      .then(res => {
        progress.value = false
        emit('close')
      })
      .catch(err => {
        progress.value = false
        console.log(err)
      })
  }
}

</script>

<template>
  <section class="quote-modal">
    <div class="w-full">
      <div>
        <div>
          <h1
            v-if="props.edit === true"
            class="typewriter text-2xl first-letter:capitalize"
          >
            {{ t('book_quotes.edit_quote') }}
          </h1>
          <h1
            v-else
            class="typewriter text-2xl first-letter:capitalize"
          >
            {{ t('book_quotes.create') }}
          </h1>
        </div>
      </div>
      <div>
        <div class="field">
          <label class="label">
            <span class="label-text font-semibold capitalize text-lg">{{ t('book_quotes.quote') }} :</span>
          </label>
          <div class="flex gap-1">
            <v-md-editor
              v-model="quoteText"
              :disabled-menus="['image/upload-image', 'toc', 'save']"
              class="w-full"
              rows="6"
            />
          </div>
        </div>
        <div class="field">
          <label class="label">
            <span
              class="label-text font-semibold capitalize text-lg"
            >{{ t('reviews.visibility') }} :</span>
          </label>

          <div class="flex gap-3">
            <label>{{ t('reviews.private') }}</label>
            <input
              v-model="visibility"
              type="radio"
              :value="Visibility.PRIVATE"
              class="radio radio-accent"
            >
            <label>{{ t('reviews.public') }}</label>
            <input
              v-model="visibility"
              type="radio"
              :value="Visibility.PUBLIC"
              class="radio radio-accent"
            >
          </div>
        </div>
        <div class="field">
          <label class="label">
            <span
              class="label-text font-semibold capitalize text-lg"
            >{{ t('book_quotes.position') }} :</span>
          </label>

          <div class="flex gap-3">
            <input
              v-model="position"
              type="text"
              class="input input-bordered w-full"
            >
          </div>
        </div>

        <div class="my-3">
          <button
            v-if="props.edit == null || props.edit === false"
            class="btn btn-secondary mr-2 uppercase"
            :disabled="progress"
            @click="submit"
          >
            <span
              v-if="progress"
              class="loading loading-spinner"
            />
            <span class="icon">
              <i class="mdi mdi-pencil mdi-18px" />
            </span>
            <span>{{ t('labels.submit') }}</span>
          </button>
          <button
            v-else
            class="btn btn-secondary mr-2 uppercase"
            :disabled="progress"
            @click="editBookQuote"
          >
            <span
              v-if="progress"
              class="loading loading-spinner"
            />
            <span class="icon">
              <i class="mdi mdi-pencil mdi-18px" />
            </span>
            <span>{{ t('labels.edit') }}</span>
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<style lang="scss">
</style>
