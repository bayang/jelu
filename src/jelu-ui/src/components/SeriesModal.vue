<script setup lang="ts">
import { Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { Series } from "../model/Series";
import dataService from "../services/DataService";
import { ObjectUtils } from "../utils/ObjectUtils";

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const props = defineProps<{
  series: Series,
  edit: boolean,
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const progress: Ref<boolean> = ref(false)

const seriesName = ref(props.edit != null && props.edit === true && props.series.name != null ? props.series.name : "")
const description = ref(props.edit != null && props.edit === true && props.series.description != null ? props.series.description : "")
const rating = ref(props.edit != null && props.edit === true && props.series.userRating != null ? props.series.userRating : undefined)

watch(rating, (newval, oldval) => {
    console.log("rating changed " + newval)
    if (newval != null && newval > 10) {
        rating.value = 10
    }
    if (newval != null && newval < 0) {
        rating.value = 0
    }
})

const editSeries = () => {
  console.log("submit ")
  if (props.series.id != null) {
    progress.value = true
    dataService.updateSeries(props.series.id, {
      description: description.value,
      name: seriesName.value,
      rating: rating.value
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

const classFor = (n: number, rating: number | undefined) => {
  const color = rating == undefined ? 'bg-danger' : 'bg-accent'
  if (n === 0) {
    return "rating-hidden"
  } else if (Number.isInteger(n)) {
    return `${color} mask mask-star-2 mask-half-2`
  } else {
    return `${color} mask mask-star-2 mask-half-1`
  }
}

const submit = () => {
    console.log("not implemented yet")
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
            {{ t('series.edit_series') }}
          </h1>
          <h1
            v-else
            class="typewriter text-2xl first-letter:capitalize"
          >
            {{ t('series.create_series') }}
          </h1>
        </div>
      </div>
      <div>
        <div class="field">
          <label class="label">
            <span class="label-text font-semibold capitalize text-lg">{{ t('labels.description') }} :</span>
          </label>
          <div class="flex gap-1">
            <v-md-editor
              v-model="description"
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
            >{{ t('series.name') }} :</span>
          </label>

          <div class="flex gap-3">
            <input
              v-model="seriesName"
              type="text"
              class="input input-bordered w-full"
            >
          </div>
        </div>
        <div class="field">
          <label class="label">
            <span class="label-text font-semibold capitalize text-lg">{{ t('reviews.rating') }} :</span>
          </label>

          <div class="flex gap-3">
            <span
              class="tooltip"
              data-tip="remove rating"
              @click="rating = undefined"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.5"
                stroke="currentColor"
                class="size-6"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M18.364 18.364A9 9 0 0 0 5.636 5.636m12.728 12.728A9 9 0 0 1 5.636 5.636m12.728 12.728L5.636 5.636"
                />
              </svg>
            </span>
            <div class="rating rating-half">
              <input
                v-for="n in ObjectUtils.range(0, 10.5, 0.5)"
                :key="n"
                v-model="rating"
                :value="n"
                type="radio"
                name="rating-10"
                :class="classFor(n, rating)"
              >
            </div>
            <span>{{ rating }}</span>
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
            @click="editSeries"
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
