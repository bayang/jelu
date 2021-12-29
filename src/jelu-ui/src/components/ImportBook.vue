<script setup lang="ts">
import { computed, reactive, Ref, ref } from "vue";
import { key } from "../store";
import { useStore } from "vuex";
import { StringUtils } from "../utils/StringUtils";
import dataService from "../services/DataService";
import { Metadata } from "../model/Metadata";

const form = reactive({
  title: "",
  isbn: "",
  authors: "",
});

const metadata: Ref<Metadata|null> = ref(null)

const errorMessage = ref("");

const fetchMetadata = async () => {
    console.log("fetch metadata")
    dataService.fetchMetadata(form.isbn, form.title, form.authors)
    .then(res => {
        console.log(res)
        metadata.value = res
        }
    )
}

const isValid = computed(() => StringUtils.isNotBlank(form.title) 
|| StringUtils.isNotBlank(form.isbn)
|| StringUtils.isNotBlank(form.authors))

</script>

<template>
<h1 class="title">Import book</h1>
  <section>
    <div class="columns is-multiline is-centered">
    <div class="column is-two-thirds">
<div class="field">
      <o-field horizontal label="Isbn">
        <o-input v-model="form.isbn"></o-input>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="Title">
        <o-input v-model="form.title"></o-input>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="Authors">
        <o-input v-model="form.authors"></o-input>
      </o-field>
    </div>
    <div class="field">
      <p class="control">
        <button :disabled="!isValid" @click="fetchMetadata" class="button is-success">
          Fetch book
        </button>
      </p>
      <p v-if="errorMessage" class="has-text-danger">{{ errorMessage }}</p>
    </div>
    </div>
    </div>
  </section>
</template>

<style lang="scss">
@import "../assets/style.scss";

</style>
