<script setup lang="ts">
import { Ref, ref, watch } from "vue";
import { CreateReadingEvent, ReadingEvent, ReadingEventType } from "../model/ReadingEvent";
import dataService from "../services/DataService";
import { useI18n } from 'vue-i18n'
import Datepicker from 'vue3-datepicker'

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const props = defineProps<{
  readingEvent: ReadingEvent|CreateReadingEvent,
  edit: boolean
}>()

const currentEvent: Ref<ReadingEvent> = ref(props.readingEvent)
const currentCreateEvent: Ref<CreateReadingEvent> = ref(props.readingEvent)
console.log(currentEvent.value)
console.log(currentCreateEvent.value)

watch(() => currentCreateEvent.value.eventType, (newValue, oldValue) => {
  if (currentCreateEvent.value.eventType == ReadingEventType.CURRENTLY_READING) {
    currentCreateEvent.value.eventDate = undefined
    currentCreateEvent.value.startDate = new Date()
  } else {
    currentCreateEvent.value.startDate = undefined
    currentCreateEvent.value.eventDate = new Date()
  }
})

const progress: Ref<boolean> = ref(false)

const emit = defineEmits<{
  (e: 'close'): void
}>()

const create = () => {
  progress.value = true
  dataService.createReadingEvent(currentCreateEvent.value)
    .then(res => {
      progress.value = false
      emit('close')
    })
    .catch(e => {
      progress.value = false
    })
}

const update = () => {
  progress.value = true
  // if user changed a finished event to a currently reading -> remove end date
  if (currentEvent.value.eventType === ReadingEventType.CURRENTLY_READING) {
    currentEvent.value.endDate = undefined
  }
  dataService.updateReadingEvent(currentEvent.value)
    .then(res => {
      progress.value = false
      emit('close')
    })
    .catch(e => {
      progress.value = false
    })
}

const deleteEvent = () => {
  if (currentEvent.value.id != null) {
    progress.value = true
    dataService.deleteReadingEvent(currentEvent.value.id)
    .then(res => {
      progress.value = false
      emit('close')
    })
    .catch(e => {
      progress.value = false
    })
  }
}

</script>

<template>
  <section class="event-modal">
    <div
      v-if="props.edit"
    >
      <div>
        <div>
          <h1 class="typewriter text-2xl first-letter:capitalize">
            {{ t('reading_events.edit_event') }}
          </h1>
        </div>
      </div>
      <div>
        <div class="field">
          <label class="label">
            <span class="label-text font-semibold">{{ t('reading_events.last_event_type') }} : </span>
          </label>
          <o-radio
            v-model="currentEvent.eventType"
            native-value="FINISHED"
          >
            {{ t('reading_events.finished') }}
          </o-radio>
          <o-radio
            v-model="currentEvent.eventType"
            native-value="CURRENTLY_READING"
          >
            {{ t('reading_events.currently_reading') }}
          </o-radio>
          <o-radio
            v-model="currentEvent.eventType"
            native-value="DROPPED"
          >
            {{ t('reading_events.dropped') }}
          </o-radio>
        </div>
        <div class="field">
          <label class="label">
            <span class="label-text font-semibold first-letter:capitalize">{{ t('reading_events.start_date') }} : </span>
          </label>
          <datepicker
            v-model="currentEvent.startDate"
            class="input input-primary"
            :typeable="true"
            :clearable="false"
          >
            <template #clear="{ onClear }">
              <button @click="onClear">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke-width="1.5"
                  stroke="currentColor"
                  class="w-6 h-6"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M12 9.75L14.25 12m0 0l2.25 2.25M14.25 12l2.25-2.25M14.25 12L12 14.25m-2.58 4.92l-6.375-6.375a1.125 1.125 0 010-1.59L9.42 4.83c.211-.211.498-.33.796-.33H19.5a2.25 2.25 0 012.25 2.25v10.5a2.25 2.25 0 01-2.25 2.25h-9.284c-.298 0-.585-.119-.796-.33z"
                  />
                </svg>
              </button>
            </template>
          </datepicker>
        </div>
        <div
          v-if="currentEvent.eventType !== ReadingEventType.CURRENTLY_READING"
          class="field"
        >
          <label class="label">
            <span class="label-text font-semibold first-letter:capitalize">{{ t('reading_events.event_date') }} : </span>
          </label>
          <datepicker
            v-model="currentEvent.endDate"
            class="input input-primary"
            :typeable="true"
            :clearable="true"
          >
            <template #clear="{ onClear }">
              <button @click="onClear">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke-width="1.5"
                  stroke="currentColor"
                  class="w-6 h-6"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M12 9.75L14.25 12m0 0l2.25 2.25M14.25 12l2.25-2.25M14.25 12L12 14.25m-2.58 4.92l-6.375-6.375a1.125 1.125 0 010-1.59L9.42 4.83c.211-.211.498-.33.796-.33H19.5a2.25 2.25 0 012.25 2.25v10.5a2.25 2.25 0 01-2.25 2.25h-9.284c-.298 0-.585-.119-.796-.33z"
                  />
                </svg>
              </button>
            </template>
          </datepicker>
        </div>
        <div class="mt-3">
          <button
            class="btn btn-secondary mr-2 uppercase"
            @click="update"
          >
            <span class="icon">
              <i class="mdi mdi-pencil mdi-18px" />
            </span>
            <span>{{ t('labels.submit') }}</span>
          </button>
          <button
            class="btn btn-error uppercase"
            @click="deleteEvent"
          >
            <span class="icon">
              <i class="mdi mdi-delete mdi-18px" />
            </span>
            <span>{{ t('labels.delete') }}</span>
          </button>
        </div>
      </div>
    </div>
    <div
      v-else
    >
      <div>
        <div>
          <h1 class="typewriter text-2xl capitalize">
            {{ t('reading_events.choose_event') }}
          </h1>
        </div>
      </div>
      <div>
        <div class="field">
          <label class="label">
            <span class="label-text font-semibold first-letter:capitalize">{{ t('reading_events.event_type') }} : </span>
          </label>
          <o-radio
            v-model="currentCreateEvent.eventType"
            native-value="FINISHED"
          >
            {{ t('reading_events.finished') }}
          </o-radio>
          <o-radio
            v-model="currentCreateEvent.eventType"
            native-value="CURRENTLY_READING"
          >
            {{ t('reading_events.currently_reading') }}
          </o-radio>
          <o-radio
            v-model="currentCreateEvent.eventType"
            native-value="DROPPED"
          >
            {{ t('reading_events.dropped') }}
          </o-radio>
        </div>
        <div
          v-if="currentEvent.eventType === ReadingEventType.CURRENTLY_READING"
          class="field"
        >
          <label class="label">
            <span class="label-text font-semibold first-letter:capitalize">{{ t('reading_events.start_date') }} :</span>
          </label>
          <datepicker
            v-model="currentCreateEvent.startDate"
            class="input input-primary"
            :clearable="true"
            :typeable="true"
          >
            <template #clear="{ onClear }">
              <button @click="onClear">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke-width="1.5"
                  stroke="currentColor"
                  class="w-6 h-6"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M12 9.75L14.25 12m0 0l2.25 2.25M14.25 12l2.25-2.25M14.25 12L12 14.25m-2.58 4.92l-6.375-6.375a1.125 1.125 0 010-1.59L9.42 4.83c.211-.211.498-.33.796-.33H19.5a2.25 2.25 0 012.25 2.25v10.5a2.25 2.25 0 01-2.25 2.25h-9.284c-.298 0-.585-.119-.796-.33z"
                  />
                </svg>
              </button>
            </template>
          </datepicker>
        </div>
        <div
          v-if="currentCreateEvent.eventType != ReadingEventType.CURRENTLY_READING"
          class="field"
        >
          <label class="label">
            <span class="label-text font-semibold first-letter:capitalize">{{ t('reading_events.event_date') }} :</span>
          </label>
          <datepicker
            v-model="currentCreateEvent.eventDate"
            class="input input-primary"
            :clearable="true"
            :typeable="true"
          >
            <template #clear="{ onClear }">
              <button @click="onClear">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke-width="1.5"
                  stroke="currentColor"
                  class="w-6 h-6"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M12 9.75L14.25 12m0 0l2.25 2.25M14.25 12l2.25-2.25M14.25 12L12 14.25m-2.58 4.92l-6.375-6.375a1.125 1.125 0 010-1.59L9.42 4.83c.211-.211.498-.33.796-.33H19.5a2.25 2.25 0 012.25 2.25v10.5a2.25 2.25 0 01-2.25 2.25h-9.284c-.298 0-.585-.119-.796-.33z"
                  />
                </svg>
              </button>
            </template>
          </datepicker>
        </div>
        <div>
          <button
            class="btn btn-secondary btn-outline mt-3 uppercase"
            @click="create"
          >
            <span class="icon">
              <i class="mdi mdi-pencil mdi-18px" />
            </span>
            <span>{{ t('labels.create') }}</span>
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
