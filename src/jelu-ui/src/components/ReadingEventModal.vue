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


const emit = defineEmits<{
  (e: 'close'): void
}>()

const create = () => {
  dataService.createReadingEvent(currentCreateEvent.value)
    .then(res => {
      emit('close')
    })
}

const update = () => {
  dataService.updateReadingEvent(currentEvent.value)
    .then(res => {
      emit('close')
    })
}

const deleteEvent = () => {
  if (currentEvent.value.id != null) {
    dataService.deleteReadingEvent(currentEvent.value.id)
    .then(res => {
      emit('close')
    })
  }
}

</script>

<template>
  <section class="event-modal">
    <div
      v-if="props.edit"
      class="event-modal-body"
    >
      <div>
        <div>
          <h1 class="title has-text-weight-normal typewriter">
            Edit event
          </h1>
        </div>
      </div>
      <div>
        <div class="field">
          <label class="label">Last event type : </label>
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
          <o-field
            label="Event date"
          >
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
          </o-field>
        </div>
        <div>
          <button
            class="button is-primary is-light mr-2"
            @click="update"
          >
            <span class="icon">
              <i class="mdi mdi-pencil" />
            </span>
            <span>Submit</span>
          </button>
          <button
            class="button is-danger is-light"
            @click="deleteEvent"
          >
            <span class="icon">
              <i class="mdi mdi-delete" />
            </span>
            <span>Delete</span>
          </button>
        </div>
      </div>
    </div>
    <div
      v-else
      class="event-modal-body"
    >
      <div>
        <div>
          <h1 class="title has-text-weight-normal typewriter">
            Choose event
          </h1>
        </div>
      </div>
      <div>
        <div class="field">
          <label class="label">Event type : </label>
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
          <o-field
            label="Event date"
          >
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
          </o-field>
        </div>
        <div>
          <button
            class="button is-primary is-light mr-2"
            @click="create"
          >
            <span class="icon">
              <i class="mdi mdi-pencil" />
            </span>
            <span>Create</span>
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<style lang="scss">

</style>
