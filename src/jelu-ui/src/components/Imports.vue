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
  <div class="columns is-centered">
    <div class="column is-centered is-one-third">
      <h1 class="title has-text-weight-normal typewriter">
        Import Csv file
      </h1>
    </div>
  </div>
  <div class="columns is-multiline is-centered">
    <div class="column is-9-desktop">
      <div class="block">
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
            @change="handleFileUpload($event)"
          >
          <br>
          <progress
            v-if="uploadPercentage > 0"
            max="100"
            :value.prop="uploadPercentage"
          />
          <br>
        </o-field>
      </div>

      <div class="field">
        <p class="control">
          <button
            class="button is-success"
            @click="importFile"
          >
            Import File
          </button>
        </p>
        <p
          v-if="errorMessage"
          class="has-text-danger"
        >
          {{ errorMessage }}
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>


</style>
