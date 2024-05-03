<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from 'vue-i18n';
import { Review } from "../model/Review";
import dataService from "../services/DataService";

const { t, d } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const props = defineProps<{ 
  review: Review,
  showDelete: boolean,
  showEdit: boolean,
}>();

const username = ref("")

const getUsername = async () => {
  username.value = await dataService.usernameById(props.review.user)
}

getUsername()

const emit = defineEmits<{
  (e: 'update:delete', reviewId: string): void,
  (e: 'update:edit', reviewId: string): void,
}>()

const deleteReview = async (reviewId: string) => {
  emit("update:delete", reviewId)
}

const editReview = async (reviewId: string) => {
  emit("update:edit", reviewId)
}

</script>

<template>
  <div
    class="card card-compact bg-base-200 shadow-2xl shadow-base-300 p-4"
  >
    <div class="card-body">
      <div class="card-title">
        <div class="avatar placeholder">
          <div class="bg-[color-mix(in_oklab,oklch(var(--n)),black_7%)] text-neutral-content rounded-full w-12">
            <span class="uppercase">{{ username.slice(0,2) }}</span>
          </div>
        </div>
        {{ username }} :
      </div>
      <div class="ml-7">
        <v-md-preview
          class="text-justify text-base"
          :text="props.review.text"
        />
      </div>
      <p class="font-bold font-mono">
        {{ review.rating }}/10
      </p>
      <div class="card-actions justify-end items-center">
        <router-link
          class="link hover:underline hover:decoration-4 hover:decoration-secondary text-sm italic"
          :to="{ name: 'review-detail', params: { reviewId: props.review.id } }"
        >
          {{ d(props.review.reviewDate, 'short') }}&nbsp;
        </router-link>
        <button
          v-if="props.showEdit"
          class="btn btn-outline btn-warning p-2 border-none"
          @click="editReview(props.review.id)"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-5 w-5"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
          </svg>
        </button>
        <button
          v-if="props.showDelete"
          class="btn btn-outline btn-error p-2 border-none"
          @click="deleteReview(props.review.id)"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-5 w-5"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fill-rule="evenodd"
              d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z"
              clip-rule="evenodd"
            />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>

</style>
