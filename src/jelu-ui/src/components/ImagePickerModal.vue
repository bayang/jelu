<script setup lang="ts">
import { Ref, ref } from "vue";
import { useI18n } from 'vue-i18n';
import { DirectoryListing, Path } from "../model/DirectoryListing";
import dataService from "../services/DataService";
import FilePickerElement from "./FilePickerElement.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const directoryListing:Ref<DirectoryListing|null> = ref(null)

const directories = (root: string|undefined) => {
  if (root != null) {
    dataService.getDirectoryListing(root, 'pictures')
    .then(res => {
      directoryListing.value = res
    })
    .catch(err => console.log(err))
  }
}

const selectPath = (elem: Path) => {
  if (elem.type == 'directory') {
    directories(elem.path)
  } else {
    emit('choose', elem)
    emit('close')
  }
}

const emit = defineEmits<{
  (e: 'choose', targetElement: Path): void,
  (e: 'close'): void
}>();

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
      <div>
        <div
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
            @choose="elem => selectPath(elem)"
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
      </div>
    </div>
  </section>
</template>

<style lang="scss">

</style>
