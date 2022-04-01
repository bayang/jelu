<script setup lang="ts">
import { Ref, ref } from "vue";
import { CreateReadingEvent, ReadingEvent } from "../model/ReadingEvent";
import dataService from "../services/DataService";

const props = defineProps<{
  readingEvent: ReadingEvent|CreateReadingEvent,
  edit: boolean
}>()

const currentEvent: Ref<ReadingEvent> = ref(props.readingEvent)
const currentCreateEvent: Ref<CreateReadingEvent> = ref(props.readingEvent)
console.log(currentEvent.value)
console.log(currentCreateEvent.value)

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
          <h1 class="typewriter text-2xl">
            Edit event
          </h1>
        </div>
      </div>
      <div>
        <div class="field">
          <label class="label">
            <span class="label-text font-semibold">Last event type : </span>
          </label>
          <o-radio
            v-model="currentEvent.eventType"
            native-value="FINISHED"
          >
            Finished
          </o-radio>
          <o-radio
            v-model="currentEvent.eventType"
            native-value="CURRENTLY_READING"
          >
            Currently reading
          </o-radio>
          <o-radio
            v-model="currentEvent.eventType"
            native-value="DROPPED"
          >
            Dropped
          </o-radio>
        </div>
        <div class="field">
          <label class="label">
            <span class="label-text font-semibold">Event date : </span>
          </label>
          <o-datepicker
            ref="datepicker"
            v-model="currentEvent.modificationDate"
            :show-week-number="false"
            :locale="undefined"
            placeholder="Click to select..."
            :expanded="true"
            icon="calendar"
            icon-right="close"
            icon-right-clickable="true"
            mobile-native="false"
            mobile-modal="false"
            trap-focus
          />
        </div>
        <div class="mt-3">
          <button
            class="btn btn-secondary mr-2"
            @click="update"
          >
            <span class="icon">
              <i class="mdi mdi-pencil mdi-18px" />
            </span>
            <span>Submit</span>
          </button>
          <button
            class="btn btn-error"
            @click="deleteEvent"
          >
            <span class="icon">
              <i class="mdi mdi-delete mdi-18px" />
            </span>
            <span>Delete</span>
          </button>
        </div>
      </div>
    </div>
    <div
      v-else
    >
      <div>
        <div>
          <h1 class="typewriter text-2xl">
            Choose event
          </h1>
        </div>
      </div>
      <div>
        <div class="field">
          <label class="label">
            <span class="label-text font-semibold">Event type : </span>
          </label>
          <o-radio
            v-model="currentCreateEvent.eventType"
            native-value="FINISHED"
          >
            Finished
          </o-radio>
          <o-radio
            v-model="currentCreateEvent.eventType"
            native-value="CURRENTLY_READING"
          >
            Currently reading
          </o-radio>
          <o-radio
            v-model="currentCreateEvent.eventType"
            native-value="DROPPED"
          >
            Dropped
          </o-radio>
        </div>
        <div class="field">
          <label class="label">
            <span class="label-text font-semibold">Event date :</span>
          </label>
          <o-datepicker
            ref="datepicker"
            v-model="currentCreateEvent.eventDate"
            :show-week-number="false"
            :locale="undefined"
            placeholder="Click to select..."
            :expanded="true"
            icon="calendar"
            icon-right="close"
            icon-right-clickable="true"
            mobile-native="false"
            mobile-modal="false"
            trap-focus
          />
        </div>
        <div>
          <button
            class="btn btn-secondary btn-outline mt-3"
            @click="create"
          >
            <span class="icon">
              <i class="mdi mdi-pencil mdi-18px" />
            </span>
            <span>Create</span>
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
