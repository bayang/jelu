<script setup lang="ts">
import { computed, reactive, Ref, ref } from "vue";
import { StringUtils } from "../utils/StringUtils";
import dataService from "../services/DataService";
import { Metadata } from "../model/Metadata";
import useDates from '../composables/dates'
import { useI18n } from 'vue-i18n'
import { useProgrammatic } from "@oruga-ui/oruga-next";
import ScanModal from "./ScanModal.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const { formatDate, formatDateString } = useDates()

const { oruga } = useProgrammatic();

const form = reactive({
  title: "",
  isbn: "",
  authors: "",
});

const emit = defineEmits(['close', 'metadataReceived']);

const metadata: Ref<Metadata|null> = ref(null)

const errorMessage = ref("");

const displayForm: Ref<boolean> = ref(true)

const progress: Ref<boolean> = ref(false)

const fetchMetadata = async () => {
    console.log("fetch metadata")
    progress.value = true
    dataService.fetchMetadata(form.isbn, form.title, form.authors)
    .then(res => {
        console.log(res)
        progress.value = false
        metadata.value = res
        displayForm.value = false
        }
    )
}

const discard = () => {
    displayForm.value = true
    metadata.value = null
}

const importData = () => {
    emit('metadataReceived', metadata.value)
    emit('close')
}

const isValid = computed(() => StringUtils.isNotBlank(form.title) 
|| StringUtils.isNotBlank(form.isbn)
|| StringUtils.isNotBlank(form.authors))

function toggleScanModal() {
    oruga.modal.open({
      component: ScanModal,
      trapFocus: true,
      active: true,
      canCancel: ['x', 'button', 'outside'],
      scroll: 'keep',
      props: {
      },
      events: {
        decoded: (barcode: string|null) => {
          console.log("barcode " + barcode)
          if (barcode != null) {
            form.isbn = barcode
          }
      }
    },
      onClose: scanModalClosed
    });
}

function scanModalClosed() {
  console.log("scan modal closed")
}


</script>

<template>
  <section class="edit-modal">
    <div class="grid justify-center justify-items-center columns is-centered is-multiline">
      <div class="mb-2">
        <h1 class="text-2xl title has-text-weight-normal typewriter capitalize">
          {{ t('labels.import_book') }}
        </h1>
      </div>
      <div
        v-if="displayForm"
        class="column is-centered is-full"
      >
        <div class="field mb-2">
          <o-field
            horizontal
            :label="t('book.isbn')"
            class="capitalize"
          >
            <o-input
              v-model="form.isbn"
              class="input focus:input-accent"
              @keyup.enter="fetchMetadata"
            />
          </o-field>
        </div>
        <div class="field mb-2">
          <o-field
            horizontal
            :label="t('book.title')"
            class="capitalize"
          >
            <o-input
              v-model="form.title"
              class="input focus:input-accent"
              @keyup.enter="fetchMetadata"
            />
          </o-field>
        </div>
        <div class="field mb-3">
          <o-field
            horizontal
            :label="t('book.author', 2)"
            class="capitalize"
          >
            <o-input
              v-model="form.authors"
              class="input focus:input-accent"
              @keyup.enter="fetchMetadata"
            />
          </o-field>
        </div>
      </div>
      <div
        v-if="displayForm"
      >
        <div class="field">
          <div class="flex gap-1">
            <button
              :disabled="!isValid"
              class="btn btn-success"
              :class="{'loading btn-disabled' : progress}"
              @click="fetchMetadata"
            >
              {{ t('labels.fetch_book') }}
            </button>
            <button
              class="btn btn-warning p-2"
              :class="{'btn-disabled' : progress}"
              @click="toggleScanModal"
            >
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
                  d="M3.75 4.875c0-.621.504-1.125 1.125-1.125h4.5c.621 0 1.125.504 1.125 1.125v4.5c0 .621-.504 1.125-1.125 1.125h-4.5A1.125 1.125 0 013.75 9.375v-4.5zM3.75 14.625c0-.621.504-1.125 1.125-1.125h4.5c.621 0 1.125.504 1.125 1.125v4.5c0 .621-.504 1.125-1.125 1.125h-4.5a1.125 1.125 0 01-1.125-1.125v-4.5zM13.5 4.875c0-.621.504-1.125 1.125-1.125h4.5c.621 0 1.125.504 1.125 1.125v4.5c0 .621-.504 1.125-1.125 1.125h-4.5A1.125 1.125 0 0113.5 9.375v-4.5z"
                />
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M6.75 6.75h.75v.75h-.75v-.75zM6.75 16.5h.75v.75h-.75v-.75zM16.5 6.75h.75v.75h-.75v-.75zM13.5 13.5h.75v.75h-.75v-.75zM13.5 19.5h.75v.75h-.75v-.75zM19.5 13.5h.75v.75h-.75v-.75zM19.5 19.5h.75v.75h-.75v-.75zM16.5 16.5h.75v.75h-.75v-.75z"
                />
              </svg>
            </button>
          </div>
          <p
            v-if="errorMessage"
            class="text-error"
          >
            {{ errorMessage }}
          </p>
        </div>
      </div>
      <div
        v-if="displayForm"
      >
        <progress
          v-if="progress"
          class="animate-pulse progress progress-success mt-5"
          max="100"
        />
      </div>
    </div>
    <div class="grid grid-cols-5 justify-center justify-items-center justify-self-center">
      <div
        v-if="!displayForm"
        class="col-span-1 mr-3"
      >
        <figure>
          <img
            :src="metadata?.image?.startsWith('http') ? metadata?.image : '/files/' + metadata?.image"
            alt="cover image"
          >
        </figure>
      </div>
      <div
        v-if="!displayForm"
        class="col-span-4"
      >
        <p
          v-if="metadata?.title"
          class="mb-2"
        >
          <span class="font-semibold capitalize">{{ t('book.title') }} : </span>{{ metadata.title }}
        </p>
        <p
          v-if="metadata?.authors != null && metadata?.authors?.length > 0"
        >
          <span class="font-semibold capitalize">{{ t('book.author', 2) }} : </span>
        </p>
        <ul
          v-if="metadata?.authors != null && metadata?.authors?.length > 0"
        >
          <li
            v-for="author in metadata?.authors"
            :key="author"
          >
            {{ author }}
          </li>
        </ul>
        <p
          v-if="metadata?.publisher"
          class="my-2"
        >
          <span class="font-semibold capitalize">{{ t('book.publisher') }} : </span>{{ metadata.publisher }}
        </p>
        <p
          v-if="metadata?.isbn10"
          class="mb-2"
        >
          <span class="font-semibold uppercase">{{ t('book.isbn10') }} : </span>{{ metadata.isbn10 }}
        </p>
        <p
          v-if="metadata?.isbn13"
          class="mb-2"
        >
          <span class="font-semibold uppercase">{{ t('book.isbn13') }} : </span>{{ metadata.isbn13 }}
        </p>
        <p
          v-if="metadata?.pageCount"
          class="mb-2"
        >
          <span class="font-semibold capitalize">{{ t('book.page', 2) }} : </span>{{ metadata.pageCount }}
        </p>
        <p
          v-if="metadata?.publishedDate"
          class="mb-2"
        >
          <span class="font-semibold capitalize">{{ t('book.published_date') }} : </span>{{ formatDateString(metadata.publishedDate) }}
        </p>
        <p
          v-if="metadata?.series"
          class="mb-2"
        >
          <span class="font-semibold capitalize">{{ t('book.series') }} : </span>{{ metadata.series }}
        </p>
        <p
          v-if="metadata?.numberInSeries"
          class="mb-2"
        >
          <span class="font-semibold">{{ t('book.nb_in_series') }} : </span>{{ metadata.numberInSeries }}
        </p>
        <p
          v-if="metadata?.language"
          class="mb-2"
        >
          <span class="font-semibold capitalize">{{ t('book.language') }} : </span>{{ metadata.language }}
        </p>
        <p
          v-if="metadata?.goodreadsId"
          class="mb-2"
        >
          <span class="font-semibold">{{ t('book.goodreads_id') }} : </span>{{ metadata.goodreadsId }}
        </p>
        <p
          v-if="metadata?.googleId"
          class="mb-2"
        >
          <span class="font-semibold">{{ t('book.google_id') }} : </span>{{ metadata.googleId }}
        </p>
        <p
          v-if="metadata?.amazonId"
          class="mb-2"
        >
          <span class="font-semibold">{{ t('book.amazon_id') }} : </span>{{ metadata.amazonId }}
        </p>
        <p
          v-if="metadata?.tags != null && metadata?.tags?.length > 0"
        >
          <span class="font-semibold capitalize">{{ t('book.tag', 2) }} : </span>
        </p>
        <p v-if="metadata?.tags != null && metadata?.tags?.length > 0">
          <span
            v-for="tag in metadata?.tags"
            :key="tag"
            class="badge badge-accent badge-outline font-semibold border-2"
          >{{ tag }}&nbsp;</span>
        </p>
        <p
          v-if="metadata?.summary"
          class="my-2"
        >
          <span class="font-semibold capitalize">{{ t('book.summary') }} : </span>{{ metadata.summary }}
        </p>
      </div>
      <div
        v-if="!displayForm"
        class="col-span-5 space-x-5 mt-3"
      >
        <button
          class="btn btn-primary"
          @click="importData"
        >
          <span class="icon">
            <i class="mdi mdi-check mdi-18px" />
          </span><span>{{ t('labels.import') }}</span>
        </button>
        <button
          class="btn btn-warning"
          @click="discard"
        >
          <span class="icon">
            <i class="mdi mdi-cancel mdi-18px" />
          </span><span>{{ t('labels.discard') }}</span>
        </button>
      </div>
    </div>
  </section>
</template>

<style lang="scss">

</style>
