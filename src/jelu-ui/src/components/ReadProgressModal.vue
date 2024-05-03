<script setup lang="ts">
import { Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { UserBookUpdate } from "../model/Book";
import dataService from "../services/DataService";
import { ObjectUtils } from "../utils/ObjectUtils";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const props = defineProps<{
  userBookId: string,
  pageCount: number|null,
  currentProgress: number|null,
  currentPage: number|null,
}>()

const userBookUpdate: Ref<UserBookUpdate> = ref({id: props.userBookId, percentRead: props.currentProgress, currentPageNumber: props.currentPage})
const progress: Ref<boolean> = ref(false)

const emit = defineEmits<{
  (e: 'close'): void
}>()

const update = () => {
  progress.value = true
  // if user changed a finished event to a currently reading -> remove end date
  dataService.updateUserBook(userBookUpdate.value)
    .then(res => {
      progress.value = false
      emit('close')
    })
    .catch(e => {
      progress.value = false
    })
}

watch(() => [userBookUpdate.value.currentPageNumber, userBookUpdate.value.percentRead],(newVals, oldVals) => {
  if (props.pageCount != null) {
    ObjectUtils.computePages(newVals, oldVals, userBookUpdate.value, props.pageCount)
  }
})

</script>

<template>
  <section class="event-modal">
    <div>
      <div>
        <div>
          <h1 class="typewriter text-2xl first-letter:capitalize">
            {{ t('labels.set_progress') }}
          </h1>
        </div>
      </div>
      <div class="flex flex-col">
        <div class="field">
          <label class="label">
            <span class="label-text font-semibold first-letter:capitalize">{{ t('book.percent_read') }} : </span>
          </label>
          <o-slider
            v-model="userBookUpdate.percentRead"
            :min="0"
            :max="100"
          />
        </div>
        <div class="field">
          <label class="label">
            <span class="label-text font-semibold first-letter:capitalize">{{ t('book.current_page_number') }} : </span>
          </label>
          <o-input
            v-model="userBookUpdate.currentPageNumber"
            type="number"
            min="0"
            class="input focus:input-accent"
          />
        </div>
        <div class="mt-3 place-self-center">
          <button
            class="btn btn-success mr-2 uppercase"
            @click="update"
          >
            <span class="icon">
              <i class="mdi mdi-pencil mdi-18px" />
            </span>
            <span>{{ t('labels.submit') }}</span>
          </button>
        </div>
      </div>
    </div>
    <progress
      v-if="progress"
      class="animate-pulse progress progress-success mt-5"
      max="100"
    />
  </section>
</template>

<style lang="scss">

</style>
