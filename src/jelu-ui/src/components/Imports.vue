<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useStore } from 'vuex'
import { key } from '../store'
import { ImportSource } from "../model/ImportConfiguration";
import dataService from "../services/DataService";
import { useTitle } from '@vueuse/core'
import { useI18n } from 'vue-i18n'
import { ObjectUtils } from '../utils/ObjectUtils'
import { useOruga } from "@oruga-ui/oruga-next";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const oruga = useOruga();

useTitle('Jelu | Imports')

const store = useStore(key)
const file = ref(new File([], "dummy"));
const importSource = ref(ImportSource.GOODREADS);
const fetchMetadata = ref(true)
const fetchCovers = ref(true)
const uploadPercentage = ref(0);
const errorMessage = ref("");
const exportErrorMessage = ref("");
const exportMessage = ref("");
const noFile = ref(true)

const handleFileUpload = (event: any) => {
  file.value = event.target.files[0];
  noFile.value = false
};

const importFile = async () => {
  if (file.value == null || file.value.name === 'dummy') {
    return
  }
  const importConfig = {
    shouldFetchMetadata: fetchMetadata.value,
    shouldFetchCovers: fetchCovers.value, 
    importSource: importSource.value
    }
    if (importSource.value == ImportSource.ISBN_LIST) {
      importConfig.shouldFetchMetadata = true
    }
  dataService.importCsv(
    importConfig,
        file.value,
        (event: { loaded: number; total: number }) => {
          let percent = Math.round((100 * event.loaded) / event.total);
          console.log("percent " + percent);
          uploadPercentage.value = percent;
        })
        .then(res => {
          ObjectUtils.toast(oruga, "success", t('csv_import.import_ok'), 6000);
        })
        .catch(err => {
          console.log(err)
        })
}

const exportFile =async () => {
  console.log("export requested")
  try {
    await dataService.exportCsv()
    exportMessage.value = t('csv_import.export_ok')
  } catch (error) {
    exportErrorMessage.value = t('csv_import.export_ko')
  }
}

let fetchCoversDisabled = computed(() => {
  if (store != null && !store.getters.getMetadataFetchEnabled) {
    return true
  } else {
    return !fetchMetadata.value
  }
})

watch(file, (newVal, oldVal) => {
  console.log("file ")
  console.log(newVal)
  console.log(oldVal)
})

</script>

<template>
  <div class="grid grid-cols-1 justify-center justify-items-center justify-self-center">
    <div
      v-tooltip="t('csv_import.import_help')"
      class="w-11/12 sm:w-8/12 pb-4 flex justify-center items-center"
    >
      <h1 class="text-2xl typewriter capitalize px-2">
        {{ t('csv_import.import_csv') }}
      </h1>
      <svg
        xmlns="http://www.w3.org/2000/svg"
        class="h-5 w-5"
        viewBox="0 0 20 20"
        fill="currentColor"
      >
        <path
          fill-rule="evenodd"
          d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
          clip-rule="evenodd"
        />
      </svg>
    </div>
    <div class="w-11/12 sm:w-8/12">
      <div
        v-if="store != null && !store.getters.getMetadataFetchEnabled"
        class="mb-3 text-warning font-bold"
      >
        !! {{ t('labels.auto_import_disabled') }}
      </div>
      <div class="form-control">
        <o-field
          horizontal
          :label="t('csv_import.import_source') + ' : '"
          class="capitalize"
        >
          <o-radio
            v-model="importSource"
            name="source"
            native-value="GOODREADS"
          >
            Goodreads
          </o-radio>
          <o-radio
            v-model="importSource"
            name="source"
            native-value="ISBN_LIST"
          >
            List of ISBN in a file
          </o-radio>
        </o-field>
      </div>
      <div
        v-if="importSource != ImportSource.ISBN_LIST"
        class="field"
      >
        <o-field
          horizontal
          :label="t('csv_import.auto_fetch_online')"
          class="capitalize"
        >
          <o-checkbox
            v-model="fetchMetadata"
            :disabled="store != null && !store.getters.getMetadataFetchEnabled"
          >
            {{ fetchMetadata }}
          </o-checkbox>
        </o-field>
      </div>
      <div
        class="field"
      >
        <o-field
          horizontal
          :label="t('csv_import.fetch_covers') + ' ?'"
          class="capitalize"
        >
          <o-checkbox
            v-model="fetchCovers" 
            :disabled="fetchCoversDisabled"
          >
            {{ fetchCovers }}
          </o-checkbox>
        </o-field>
      </div>
        
      <div>
        <o-field
          horizontal
          :label="t('csv_import.choose_file')"
          class="file"
        >
          <input
            type="file"
            accept=".csv,.tsv,.txt"
            class="block w-full text-sm text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-accent file:text-accent-content hover:file:bg-gray-300"
            @change="handleFileUpload($event)"
          >
          <br>
          <progress
            v-if="uploadPercentage > 0"
            max="100"
            :value.prop="uploadPercentage"
            class="progress progress-primary"
          />
          <br>
        </o-field>
      </div>

      <div class="field">
        <p class="control">
          <button
            class="btn btn-success uppercase"
            :disabled="noFile"
            @click="importFile"
          >
            {{ t('csv_import.import_file') }}
          </button>
        </p>
        <p
          v-if="errorMessage"
          class="text-error"
        >
          {{ errorMessage }}
        </p>
      </div>
    </div>
    <h1 class="text-2xl typewriter w-11/12 sm:w-8/12 py-4 capitalize">
      {{ t('csv_import.export') }}
    </h1>
    <div class="w-11/12 sm:w-8/12">
      <p class="first-letter:capitalize pb-2">
        {{ t('csv_import.export_message') }}
      </p>
      <div class="field">
        <p class="control">
          <button
            class="btn btn-info uppercase"
            @click="exportFile"
          >
            {{ t('csv_import.export_file') }}
          </button>
        </p>
        <p
          v-if="exportMessage"
          class=""
        >
          {{ exportMessage }}
        </p>
        <p
          v-if="exportErrorMessage"
          class="text-error"
        >
          {{ exportErrorMessage }}
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>
