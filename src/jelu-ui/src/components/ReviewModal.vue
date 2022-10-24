<script setup lang="ts">
import { Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { Book } from "../model/Book";
import { Review, Visibility } from "../model/Review";
import dataService from "../services/DataService";

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const props = defineProps<{
  book: Book,
  edit: boolean,
  review: Review | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const progress: Ref<boolean> = ref(false)

const visibility: Ref<Visibility> = ref(props.edit != null && props.edit === true && props.review?.visibility != null ? props.review?.visibility : Visibility.PRIVATE)
const rating = ref(props.edit != null && props.edit === true && props.review?.rating != null ? props.review?.rating : 5.0)
const reviewText = ref(props.edit != null && props.edit === true && props.review?.text != null ? props.review?.text : "")

watch(visibility, (newVal, oldVal) => {
  console.log("visibilty " + visibility.value)
})

watch(rating, (newVal, oldVal) => {
  console.log("visibilty " + newVal + " " + oldVal)
})

// https://stackoverflow.com/questions/39924644/es6-generate-an-array-of-numbers
const range = (start: number, end: number, step: number) => {
  return Array.from(Array.from(Array(Math.ceil((end - start) / step)).keys()), x => start + x * step);
}

const classFor = (n: number) => {
  if (n === 0) {
    return "rating-hidden"
  } else if (Number.isInteger(n)) {
    return "bg-accent mask mask-star-2 mask-half-2"
  } else {
    return "bg-accent mask mask-star-2 mask-half-1"
  }
}

const submit = () => {
  console.log("submit ")
  if (props.book.id != null) {
    progress.value = true
    dataService.saveReview({
      bookId: props.book.id,
      rating: rating.value,
      text: reviewText.value,
      visibility: visibility.value,
      reviewDate: new Date()
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

const editReview = () => {
  console.log("edit ")
  if (props.book.id != null && props.review?.id != null) {
    progress.value = true
    dataService.updateReview(props.review.id, {
      rating: rating.value,
      text: reviewText.value,
      visibility: visibility.value,
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
  <section class="review-modal">
    <div class="w-full">
      <div>
        <div>
          <h1
            v-if="props.edit === true"
            class="typewriter text-2xl first-letter:capitalize"
          >
            {{ t('reviews.edit_review') }}
          </h1>
          <h1
            v-else
            class="typewriter text-2xl first-letter:capitalize"
          >
            {{ t('reviews.create_review') }}
          </h1>
        </div>
      </div>
      <div>
        <div class="field">
          <label class="label">
            <span class="label-text font-semibold capitalize text-lg">{{ t('reviews.review') }} :</span>
          </label>
          <div class="flex gap-1">
            <v-md-editor
              v-model="reviewText"
              :disabled-menus="['image/upload-image', 'toc', 'save']"
              class="textarea textarea-accent w-full"
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
            <span class="label-text font-semibold capitalize text-lg">{{ t('reviews.rating') }} :</span>
          </label>

          <div class="flex gap-3">
            <div class="rating rating-half">
              <input
                v-for="n in range(0, 10.5, 0.5)"
                :key="n"
                v-model="rating"
                :value="n"
                type="radio"
                name="rating-10"
                :class="classFor(n)"
              >
            </div>
            <span>{{ rating }}</span>
          </div>
        </div>

        <div class="my-3">
          <button
            v-if="props.edit == null || props.edit === false"
            class="btn btn-secondary mr-2"
            :disabled="progress"
            :class="{ 'loading': progress }"
            @click="submit"
          >
            <span class="icon">
              <i class="mdi mdi-pencil mdi-18px" />
            </span>
            <span>{{ t('labels.submit') }}</span>
          </button>
          <button
            v-else
            class="btn btn-secondary mr-2"
            :disabled="progress"
            :class="{ 'loading': progress }"
            @click="editReview"
          >
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
