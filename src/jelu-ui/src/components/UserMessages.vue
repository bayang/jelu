<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import { onMounted, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import usePagination from '../composables/pagination';
import { MessageCategory, UserMessage } from "../model/UserMessage";
import dataService from "../services/DataService";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

useTitle('Jelu | Messages')

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading } = usePagination()
perPage.value = 10

const read: Ref<boolean> = ref(false)

const categories: Ref<Array<MessageCategory>> = ref([])

const messages: Ref<Array<UserMessage>> = ref([]);

const getMessagesIsLoading: Ref<boolean> = ref(false)

const getMessages = () => {
  getMessagesIsLoading.value = true
  dataService.messages(categories.value, read.value, pageAsNumber.value - 1, perPage.value, undefined)
  .then(res => {
    console.log(res)
    total.value = res.totalElements
    messages.value = res.content
    if (! res.empty) {
          page.value =  (res.number + 1).toString(10)
        }
        else {
          page.value = "1"
        }
        getMessagesIsLoading.value = false
        updatePageLoading(false)
  })
  .catch(e => {
      getMessagesIsLoading.value = false
      updatePageLoading(false)
    })
}

const updateMessage = (message: UserMessage, read: boolean) => {
  if (message.id !== undefined) {
    dataService.updateUserMessage(message.id, {read: read, message: "Message 5 updated"})
    .then(res => {
      console.log(`message ${message.id} read : ${read}`)
      getMessages()
    })
    .catch(e => {
      console.log(`error : message ${message.id} read : ${read}`)
    })
  }
}

function category(cat: MessageCategory) {
  if (cat === MessageCategory.ERROR) {
    return 'alert-error'
  } else if (cat === MessageCategory.INFO) {
    return 'alert-info'
  } else if (cat === MessageCategory.SUCCESS) {
    return 'alert-success'
  } else if (cat === MessageCategory.WARNING) {
    return 'alert-warning'
  }
}

function icon(cat: MessageCategory) {
  if (cat === MessageCategory.ERROR) {
    return 'M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z'
  } else if (cat === MessageCategory.INFO) {
    return 'M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z'
  } else if (cat === MessageCategory.SUCCESS) {
    return 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z'
  } else if (cat === MessageCategory.WARNING) {
    return 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z'
  }
}

watch(page, (newVal, oldVal) => {
  console.log("all " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetMessages()
  }
})

// watches set above sometimes called twice
const throttledGetMessages = useThrottleFn(() => {
  getMessages()
}, 100, false)

onMounted(() => {
  console.log("Component is mounted!");
  try {
    getMessages();
  } catch (error) {
    console.log("failed get messages : " + error);
  }
});
</script>

<template>
  <div class="grid grid-cols-1 justify-center justify-items-center justify-self-center">
    <h1 class="text-2xl typewriter w-11/12 sm:w-8/12 pb-4 capitalize">
      {{ t('settings.messages') }}
    </h1>
    <div
      v-if="messages == null || messages.length < 1"
      class="font-bold italic text-xl"
    >
      {{ t('user-messages.no_messages') }} !
    </div>
    <div
      v-for="message in messages"
      :key="message.id"
      class="m-1 alert shadow-lg sm:w-9/12"
      :class="category(message.category)"
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        class="stroke-current flex-shrink-0 h-6 w-6"
        fill="none"
        viewBox="0 0 24 24"
      ><path
        stroke-linecap="round"
        stroke-linejoin="round"
        stroke-width="2"
        :d="icon(message.category)"
      /></svg>
      <div>
        <span>{{ message.message }}</span>
        <a
          v-if="message.link"
          class="link mx-3 uppercase"
          :href="message.link"
          target="_blank"
        >{{ t('user-messages.link') }}
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-5 w-5 inline"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fill-rule="evenodd"
              d="M12.586 4.586a2 2 0 112.828 2.828l-3 3a2 2 0 01-2.828 0 1 1 0 00-1.414 1.414 4 4 0 005.656 0l3-3a4 4 0 00-5.656-5.656l-1.5 1.5a1 1 0 101.414 1.414l1.5-1.5zm-5 5a2 2 0 012.828 0 1 1 0 101.414-1.414 4 4 0 00-5.656 0l-3 3a4 4 0 105.656 5.656l1.5-1.5a1 1 0 10-1.414-1.414l-1.5 1.5a2 2 0 11-2.828-2.828l3-3z"
              clip-rule="evenodd"
            />
          </svg>
        </a>
      </div>
      <div
        class="flex-none tooltip"
        :data-tip="t('user-messages.mark_read')"
      >
        <button
          class="btn btn-circle btn-xs"
          @click="updateMessage(message, !message.read)"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="h-5 w-5"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fill-rule="evenodd"
              d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
              clip-rule="evenodd"
            />
          </svg>
        </button>
      </div>
    </div>
  </div>
  <o-pagination
    v-if="messages.length > 0"
    :current="pageAsNumber"
    :total="total"
    order="centered"
    :per-page="perPage"
    @change="updatePage"
  />
  <o-loading
    v-model:active="getPageIsLoading"
    :full-page="true"
    :cancelable="true"
  />
</template>

<style scoped>

</style>
