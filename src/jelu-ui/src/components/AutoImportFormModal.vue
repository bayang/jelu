<script setup lang="ts">
import { computed, reactive, Ref, ref } from "vue";
import { StringUtils } from "../utils/StringUtils";
import dataService from "../services/DataService";
import { Metadata } from "../model/Metadata";
import { DateUtils } from "../utils/DateUtils";

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

const format = (dateString: string|null|undefined) => {
  if (dateString != null) {
    return DateUtils.formatDate(dateString)
  }
  return ''
}

const isValid = computed(() => StringUtils.isNotBlank(form.title) 
|| StringUtils.isNotBlank(form.isbn)
|| StringUtils.isNotBlank(form.authors))

</script>

<template>
  <section class="edit-modal">
    <div class="columns is-centered is-multiline">
      <div class="column is-centered is-two-fifths">
        <h1 class="title has-text-weight-normal typewriter">
          Import book
        </h1>
      </div>
      <div
        v-if="displayForm"
        class="column is-centered is-full"
      >
        <div class="field">
          <o-field
            horizontal
            label="Isbn"
          >
            <o-input v-model="form.isbn" />
          </o-field>
        </div>
        <div class="field">
          <o-field
            horizontal
            label="Title"
          >
            <o-input v-model="form.title" />
          </o-field>
        </div>
        <div class="field">
          <o-field
            horizontal
            label="Authors"
          >
            <o-input v-model="form.authors" />
          </o-field>
        </div>
      </div>
      <div
        v-if="displayForm"
        class="column is-centered is-one-fifth"
      >
        <div class="field">
          <p class="control">
            <button
              :disabled="!isValid"
              class="button is-success"
              @click="fetchMetadata"
            >
              Fetch book
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
      <div
        v-if="displayForm"
        class="column is-centered is-full"
      >
        <progress
          v-if="progress"
          class="progress is-small is-success"
          max="100"
        />
      </div>
    </div>
    <div class="columns is-centered is-multiline">
      <div
        v-if="!displayForm"
        class="column is-one-fifth"
      >
        <figure>
          <img
            :src="'/files/' + metadata?.image"
            alt="cover image"
          >
        </figure>
      </div>
      <div
        v-if="!displayForm"
        class="column is-four-fifths"
      >
        <p
          v-if="metadata?.title"
          class="has-text-left"
        >
          <span class="has-text-weight-semibold">Title : </span>{{ metadata.title }}
        </p>
        <p
          v-if="metadata?.authors != null && metadata?.authors?.length > 0"
          class="has-text-left"
        >
          <span class="has-text-weight-semibold">Authors : </span>
        </p>
        <ul
          v-if="metadata?.authors != null && metadata?.authors?.length > 0"
          class="has-text-left"
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
          class="has-text-left"
        >
          <span class="has-text-weight-semibold">Publisher : </span>{{ metadata.publisher }}
        </p>
        <p
          v-if="metadata?.isbn10"
          class="has-text-left"
        >
          <span class="has-text-weight-semibold">Isbn10 : </span>{{ metadata.isbn10 }}
        </p>
        <p
          v-if="metadata?.isbn13"
          class="has-text-left"
        >
          <span class="has-text-weight-semibold">Isbn13 : </span>{{ metadata.isbn13 }}
        </p>
        <p
          v-if="metadata?.pageCount"
          class="has-text-left"
        >
          <span class="has-text-weight-semibold">Pages : </span>{{ metadata.pageCount }}
        </p>
        <p
          v-if="metadata?.publishedDate"
          class="has-text-left"
        >
          <span class="has-text-weight-semibold">Published date : </span>{{ format(metadata.publishedDate) }}
        </p>
        <p
          v-if="metadata?.series"
          class="has-text-left"
        >
          <span class="has-text-weight-semibold">Series : </span>{{ metadata.series }}
        </p>
        <p
          v-if="metadata?.numberInSeries"
          class="has-text-left"
        >
          <span class="has-text-weight-semibold"># in series : </span>{{ metadata.numberInSeries }}
        </p>
        <p
          v-if="metadata?.language"
          class="has-text-left"
        >
          <span class="has-text-weight-semibold">Language : </span>{{ metadata.language }}
        </p>
        <p
          v-if="metadata?.goodreadsId"
          class="has-text-left"
        >
          <span class="has-text-weight-semibold">Goodreads id : </span>{{ metadata.goodreadsId }}
        </p>
        <p
          v-if="metadata?.googleId"
          class="has-text-left"
        >
          <span class="has-text-weight-semibold">Google id : </span>{{ metadata.googleId }}
        </p>
        <p
          v-if="metadata?.amazonId"
          class="has-text-left"
        >
          <span class="has-text-weight-semibold">Amazon id : </span>{{ metadata.amazonId }}
        </p>
        <p
          v-if="metadata?.tags != null && metadata?.tags?.length > 0"
          class="has-text-left"
        >
          <span class="has-text-weight-semibold">Tags : </span>
        </p>
        <p v-if="metadata?.tags != null && metadata?.tags?.length > 0">
          <span
            v-for="tag in metadata?.tags"
            :key="tag"
            class="tag is-primary is-light"
          >{{ tag }}&nbsp;</span>
        </p>
        <p
          v-if="metadata?.summary"
          class="has-text-left"
        >
          <span class="has-text-weight-semibold">Summary : </span>{{ metadata.summary }}
        </p>
      </div>
      <div
        v-if="!displayForm"
        class="column is-one-fifth"
      >
        <button
          class="button"
          @click="importData"
        >
          <span class="icon">
            <i class="mdi mdi-check" />
          </span><span>Import</span>
        </button>
      </div>
      <div
        v-if="!displayForm"
        class="column is-one-fifth is-offset-one-fifth"
      >
        <button
          class="button"
          @click="discard"
        >
          <span class="icon">
            <i class="mdi mdi-cancel" />
          </span><span>Discard</span>
        </button>
      </div>
    </div>
  </section>
</template>

<style lang="scss">

</style>
