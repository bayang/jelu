<script setup lang="ts">
import { Ref, ref } from "vue";
import { useI18n } from 'vue-i18n';
import { DirectoryListing, Path } from "../model/DirectoryListing";
import { Metadata } from "../model/Metadata";
import dataService from "../services/DataService";
import MetadataDetail from "./MetadataDetail.vue";
import FilePickerElement from "./FilePickerElement.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const file = ref(null);
const uploadPercentage = ref(0);

const handleFileUpload = (event: any) => {
  file.value = event.target.files[0];
  dataService.getMetadataFromUploadedFile(file.value,
  (event: { loaded: number; total: number }) => {
          let percent = Math.round((100 * event.loaded) / event.total);
          uploadPercentage.value = percent;
        }).then(res => {
          metadata.value = res
          displayMetadata.value = true
        })
        .catch(e => console.log(e))
};

const fromServer = ref(false);

const directoryListing:Ref<DirectoryListing|null> = ref(null)

const directories = (root: string|undefined) => {
  if (root != null) {
    dataService.getDirectoryListing(root)
    .then(res => {
      directoryListing.value = res
    })
    .catch(err => console.log(err))
  }
}

const emit = defineEmits(['close', 'metadataReceived']);

const displayMetadata: Ref<boolean> = ref(false)
const metadata: Ref<Metadata|null> = ref(null)

const displayForm: Ref<boolean> = ref(true)

const discard = () => {
    displayForm.value = true
    metadata.value = null
    displayMetadata.value = false
}

const importData = () => {
    emit('metadataReceived', metadata.value)
    emit('close')
}

const selectPath = (elem: Path) => {
  if (elem.type == 'directory') {
    directories(elem.path)
  } else {
    dataService.getMetadataFromFile(elem.path)
      .then(res => {
        metadata.value = res
        displayMetadata.value = true
      })
      .catch(err => console.log(err))
  }
}

directories('/')

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
        v-if="displayMetadata && metadata != null"
        class="flex flex-col items-center"
      >
        <MetadataDetail :metadata="metadata" />
        <div
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
      <div v-else>
        <div class="field">
          <input
            v-model="fromServer"
            type="checkbox"
            class="toggle toggle-primary"
          >
          <span class="mx-2">{{ fromServer == true ? t('labels.upload_from_server') : t('labels.upload_from_computer') }}</span>
        </div>
        <div
          v-if="fromServer"
          class="mt-3"
        >
          <div
            v-if="directoryListing?.parent != null"
            class="flex items-center gap-2"
          >
            <button
              class="btn btn-square bg-base-100"
              @click="directories(directoryListing?.parent)"
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
                  d="M12 19.5v-15m0 0l-6.75 6.75M12 4.5l6.75 6.75"
                />
              </svg>
            </button>
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
                d="M2.25 12.75V12A2.25 2.25 0 014.5 9.75h15A2.25 2.25 0 0121.75 12v.75m-8.69-6.44l-2.12-2.12a1.5 1.5 0 00-1.061-.44H4.5A2.25 2.25 0 002.25 6v12a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9a2.25 2.25 0 00-2.25-2.25h-5.379a1.5 1.5 0 01-1.06-.44z"
              />
            </svg> {{ directoryListing.parent }}
          </div>
          <FilePickerElement
            v-for="elem of directoryListing?.directories"
            :key="elem.path"
            :elem="elem"
            @choose="selectPath"
          />
          <div
            v-if="directoryListing?.directories && 
              directoryListing?.directories.length < 1"
          >
            <div class="flex flex-row place-content-center mt-2">
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
                  d="M20.25 7.5l-.625 10.632a2.25 2.25 0 01-2.247 2.118H6.622a2.25 2.25 0 01-2.247-2.118L3.75 7.5m6 4.125l2.25 2.25m0 0l2.25 2.25M12 13.875l2.25-2.25M12 13.875l-2.25 2.25M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125z"
                />
              </svg> <span>No supported file here</span>
            </div>
          </div>
        </div>
        <div v-else>
          <input
            type="file"
            accept="image/*,.opf,.epub,.OPF,.EPUB"
            class="block file-input w-full text-sm text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold hover:file:bg-gray-300"
            @change="handleFileUpload($event)"
          >
          <br>
          <progress
            max="100"
            :value.prop="uploadPercentage"
            class="progress progress-primary"
          />
          <br>
        </div>
      </div>
    </div>
  </section>
</template>

<style lang="scss">

</style>
