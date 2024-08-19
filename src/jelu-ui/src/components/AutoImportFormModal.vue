<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next";
import { ComputedRef, Ref, computed, reactive, ref } from "vue";
import { useI18n } from 'vue-i18n';
import { useStore } from 'vuex';
import { Book } from "../model/Book";
import { Metadata } from "../model/Metadata";
import { PluginInfo } from "../model/PluginInfo";
import { ServerSettings } from "../model/ServerSettings";
import dataService from "../services/DataService";
import { key } from '../store';
import { StringUtils } from "../utils/StringUtils";
import MetadataDetail from "./MetadataDetail.vue";
import MetadataPluginsModal from "./MetadataPluginsModal.vue";
import ScanModal from "./ScanModal.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })
const store = useStore(key)

const oruga = useOruga();

const props = defineProps<{
  book: Book|undefined,
}>()

const form = reactive({
  title: props.book?.title,
  isbn: props.book?.isbn10?.length != undefined && props.book?.isbn10?.length > 0 ? props.book?.isbn10 : props.book?.isbn13,
  authors: props.book?.authors?.map(a => a.name).join(','),
});

const serverSettings: ComputedRef<ServerSettings> = computed(() => {
  return store != undefined && store.getters.getSettings
})

const emit = defineEmits(['close', 'metadataReceived']);

const metadata: Ref<Metadata|null> = ref(null)

const errorMessage = ref("");

const displayForm: Ref<boolean> = ref(true)

const progress: Ref<boolean> = ref(false)

let plugins: Array<PluginInfo> = []

const fetchMetadata = async () => {
    console.log("fetch metadata")
    progress.value = true
    dataService.fetchMetadataWithPlugins({isbn: form.isbn, title: form.title, authors: form.authors, plugins: plugins})
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

let barcodeReader: any = null

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
      },
      barcodeLoaded: (reader: any) => {
        barcodeReader = reader
      }
    },
      onClose: scanModalClosed
    });
}

function togglePluginsModal() {
    oruga.modal.open({
      component: MetadataPluginsModal,
      trapFocus: true,
      active: true,
      canCancel: ['x', 'button', 'outside'],
      scroll: 'keep',
      props: {
      },
      events: {
        plugins: (received: Array<PluginInfo>) => {
          console.log("plugins ")
          console.log(received)
          plugins = received
      }
    },
      onClose: pluginsModalClosed
    });
}

function scanModalClosed() {
  console.log("scan modal closed")
    barcodeReader.codeReader.stream
        .getTracks()
        .forEach(function (track: any) {
            track.stop();
        });
}

function pluginsModalClosed() {
  console.log("plugins modal closed")
}

</script>

<template>
  <section class="edit-modal">
    <div class="grid justify-center justify-items-center">
      <div class="mb-2">
        <h1 class="text-2xl typewriter capitalize">
          {{ t('labels.import_book') }}
        </h1>
      </div>
      <div
        v-if="displayForm"
        class="column"
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
              class="btn btn-success uppercase"
              :class="{'btn-disabled' : progress}"
              @click="fetchMetadata"
            >
              <span
                v-if="progress"
                class="loading loading-spinner"
              />
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
            <button
              v-if="serverSettings.metadataFetchEnabled && serverSettings.metadataPlugins.length > 1"
              class="btn btn-secondary p-2"
              :class="{'btn-disabled' : progress}"
              @click="togglePluginsModal"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                class="w-6 h-6"
              >
                <path d="M6 12a.75.75 0 01-.75-.75v-7.5a.75.75 0 111.5 0v7.5A.75.75 0 016 12zM18 12a.75.75 0 01-.75-.75v-7.5a.75.75 0 011.5 0v7.5A.75.75 0 0118 12zM6.75 20.25v-1.5a.75.75 0 00-1.5 0v1.5a.75.75 0 001.5 0zM18.75 18.75v1.5a.75.75 0 01-1.5 0v-1.5a.75.75 0 011.5 0zM12.75 5.25v-1.5a.75.75 0 00-1.5 0v1.5a.75.75 0 001.5 0zM12 21a.75.75 0 01-.75-.75v-7.5a.75.75 0 011.5 0v7.5A.75.75 0 0112 21zM3.75 15a2.25 2.25 0 104.5 0 2.25 2.25 0 00-4.5 0zM12 11.25a2.25 2.25 0 110-4.5 2.25 2.25 0 010 4.5zM15.75 15a2.25 2.25 0 104.5 0 2.25 2.25 0 00-4.5 0z" />
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
    <div 
      class="flex flex-col items-center"
    >
      <MetadataDetail
        v-if="metadata != null"
        :metadata="metadata"
      />
      <div
        v-if="!displayForm"
        class="col-span-5 space-x-5 mt-3"
      >
        <button
          class="btn btn-primary uppercase"
          @click="importData"
        >
          <span class="icon">
            <i class="mdi mdi-check mdi-18px" />
          </span><span>{{ t('labels.import') }}</span>
        </button>
        <button
          class="btn btn-warning uppercase"
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
