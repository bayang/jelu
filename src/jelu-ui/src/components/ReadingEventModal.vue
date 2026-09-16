<script setup lang="ts">
import { Ref, ref, watch } from "vue";
import { CreateReadingEvent, ReadingEvent, ReadingEventType } from "../model/ReadingEvent";
import dataService from "../services/DataService";
import { useI18n } from 'vue-i18n'
import useTypography from "../composables/typography";

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
  if (currentCreateEvent.value.eventType === ReadingEventType.CURRENTLY_READING) {
    console.log("cleaning event date if currently reading")
    currentCreateEvent.value.eventDate = undefined
  }
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

const { typographyClasses } = useTypography()
</script>

<template>
  <section class="event-modal">
    <div
      v-if="props.edit"
    >
      <div>
        <div>
          <h1
            class="text-2xl first-letter:capitalize"
            :class="typographyClasses"
          >
            {{ t('reading_events.edit_event') }}
          </h1>
        </div>
      </div>
      <div>
        <div class="field">
          <label class="label">
            <span class="label-text font-semibold">{{ t('reading_events.last_event_type') }} : </span>
          </label>
          <div class="field">
            <input
              v-model="currentCreateEvent.eventType"
              type="radio"
              name="radio-28"
              class="radio radio-primary my-2"
              value="FINISHED"
            >
            <span class="label-text ml-2">
              {{ t('reading_events.finished') }}
            </span>
          </div>
          <div class="field">
            <input
              v-model="currentCreateEvent.eventType"
              type="radio"
              name="radio-28"
              class="radio radio-primary my-2"
              value="CURRENTLY_READING"
            >
            <span class="label-text ml-2">
              {{ t('reading_events.currently_reading') }}
            </span>
          </div>
          <div class="field">
            <input
              v-model="currentCreateEvent.eventType"
              type="radio"
              name="radio-28"
              class="radio radio-primary my-2"
              value="DROPPED"
            >
            <span class="label-text ml-2">
              {{ t('reading_events.dropped') }}
            </span>
          </div>
        </div>
        <div class="field">
          <label class="label">
            <span class="label-text font-semibold first-letter:capitalize">{{ t('reading_events.start_date') }} : </span>
          </label>
          <o-datepicker
            ref="datepicker"
            v-model="currentEvent.startDate"
            :show-week-number="false"
            :locale="undefined"
            :placeholder="t('labels.click_to_select')"
            icon="calendar"
            trap-focus
            expanded
          />
        </div>
        <div
          v-if="currentEvent.eventType !== ReadingEventType.CURRENTLY_READING"
          class="field"
        >
          <label class="label">
            <span class="label-text font-semibold first-letter:capitalize">{{ t('reading_events.event_date') }} : </span>
          </label>
          <o-datepicker
            ref="datepicker"
            v-model="currentEvent.endDate"
            :show-week-number="false"
            :locale="undefined"
            :placeholder="t('labels.click_to_select')"
            icon="calendar"
            trap-focus
            expanded
          />
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
          <h1
            class="text-2xl capitalize"
            :class="typographyClasses"
          >
            {{ t('reading_events.choose_event') }}
          </h1>
        </div>
      </div>
      <div>
        <div class="field">
          <label class="label">
            <span class="label-text font-semibold first-letter:capitalize">{{ t('reading_events.event_type') }} : </span>
          </label>
          <div class="field">
            <input
              v-model="currentCreateEvent.eventType"
              type="radio"
              name="radio-29"
              class="radio radio-primary my-2"
              value="FINISHED"
            >
            <span class="label-text ml-2">
              {{ t('reading_events.finished') }}
            </span>
          </div>
          <div class="field">
            <input
              v-model="currentCreateEvent.eventType"
              type="radio"
              name="radio-29"
              class="radio radio-primary my-2"
              value="CURRENTLY_READING"
            >
            <span class="label-text ml-2">
              {{ t('reading_events.currently_reading') }}
            </span>
          </div>
          <div class="field">
            <input
              v-model="currentCreateEvent.eventType"
              type="radio"
              name="radio-29"
              class="radio radio-primary my-2"
              value="DROPPED"
            >
            <span class="label-text ml-2">
              {{ t('reading_events.dropped') }}
            </span>
          </div>
        </div>
        <div
          v-if="currentEvent.eventType === ReadingEventType.CURRENTLY_READING"
          class="field"
        >
          <label class="label">
            <span class="label-text font-semibold first-letter:capitalize">{{ t('reading_events.start_date') }} :</span>
          </label>
          <o-datepicker
            ref="datepicker"
            v-model="currentCreateEvent.startDate"
            :show-week-number="false"
            :locale="undefined"
            :placeholder="t('labels.click_to_select')"
            icon="calendar"
            trap-focus
            expanded
          />
        </div>
        <div
          v-if="currentCreateEvent.eventType != ReadingEventType.CURRENTLY_READING"
          class="field"
        >
          <label class="label">
            <span class="label-text font-semibold first-letter:capitalize">{{ t('reading_events.event_date') }} :</span>
          </label>
          <o-datepicker
            ref="datepicker"
            v-model="currentCreateEvent.eventDate"
            :show-week-number="false"
            :locale="undefined"
            :placeholder="t('labels.click_to_select')"
            icon="calendar"
            trap-focus
            expanded
          />
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
