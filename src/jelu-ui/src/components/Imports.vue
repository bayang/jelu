<script setup lang="ts">
import { ref } from "vue";
import { ImportSource } from "../model/ImportConfiguration";
import dataService from "../services/DataService";
import { useTitle } from '@vueuse/core'

useTitle('Jelu | Imports')

const file = ref(new File([], "dummy"));
const importSource = ref(ImportSource.GOODREADS);
const fetchMetadata = ref(true)
const fetchCovers = ref(true)
const uploadPercentage = ref(0);
const errorMessage = ref("");

const handleFileUpload = (event: any) => {
  file.value = event.target.files[0];
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
  dataService.importCsv(
    importConfig,
        file.value,
        (event: { loaded: number; total: number }) => {
          let percent = Math.round((100 * event.loaded) / event.total);
          console.log("percent " + percent);
          uploadPercentage.value = percent;
        })
}

</script>

<template>
  <div class="grid grid-cols-1 justify-center justify-items-center justify-self-center columns is-centered">
    <h1 class="text-2xl typewriter w-11/12 sm:w-8/12 pb-4">
      Import Csv file
    </h1>
    <div class="w-11/12 sm:w-8/12">
      <div class="form-control">
        <o-field
          horizontal
          label="Import source : "
        >
          <o-radio
            v-model="importSource"
            name="source"
            native-value="GOODREADS"
          >
            Goodreads
          </o-radio>
        </o-field>
      </div>
      <div class="field">
        <o-field
          horizontal
          label="Automatically fetch metadata online"
        >
          <o-checkbox v-model="fetchMetadata">
            {{ fetchMetadata }}
          </o-checkbox>
        </o-field>
      </div>
      <div class="field">
        <o-field
          horizontal
          label="Also fetch covers ?"
        >
          <o-checkbox
            v-model="fetchCovers" 
            :disabled="!fetchMetadata"
          >
            {{ fetchCovers }}
          </o-checkbox>
        </o-field>
      </div>
        
      <div>
        <o-field
          horizontal
          label="Choose file to import"
          class="file is-primary has-name"
        >
          <input
            type="file"
            accept=".csv,.tsv"
            class="block w-full text-sm text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-gray-50 file:text-primary hover:file:bg-gray-300"
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
            class="btn btn-success"
            @click="importFile"
          >
            Import File
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
  </div>
</template>

<style scoped>


</style>
