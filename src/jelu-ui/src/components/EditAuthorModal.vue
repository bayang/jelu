<script setup lang="ts">
import { computed, Ref, ref } from "vue";
import { Author } from "../model/Author";
import { WikipediaSearchResult, WikipediaSearchResultElement } from "../model/WikipediaSearchResult";
import dataService from "../services/DataService";
import { StringUtils } from "../utils/StringUtils";

const props = defineProps<{
  author : Author
}>()

const currentAuthor: Ref<Author> = ref(props.author)
console.log(currentAuthor.value)
const progress: Ref<boolean> = ref(false)
let deleteImage: Ref<boolean> = ref(false)
const isSwitchedCustom = ref("Upload from the web");
const imageUrl = ref<string | null>(null);
const file = ref(null);
const uploadPercentage = ref(0);
const searchlanguage = ref("en");
const FORM = "FORM"
const SEARCH = "SEARCH"
const currentPhase = ref(FORM)
const searchResult: Ref<WikipediaSearchResult> = ref({pages: []})

const emit = defineEmits<{
  (e: 'close'): void
}>()

let hasImage = computed(() => {
  return StringUtils.isNotBlank(currentAuthor.value.image)
})

function toggleRemoveImage() {
  deleteImage.value = !deleteImage.value
}

const clearImageField = () => {
  imageUrl.value = "";
};

const handleFileUpload = (event: any) => {
  file.value = event.target.files[0];
};

const update = () => {
  progress.value = true
  if (imageUrl.value != null) {
    currentAuthor.value.image = imageUrl.value
  }
  dataService.updateAuthor(currentAuthor.value, file.value, (event: { loaded: number; total: number }) => {
          let percent = Math.round((100 * event.loaded) / event.total);
          console.log("percent " + percent);
          uploadPercentage.value = percent;
        })
    .then(res => {
      progress.value = false
      emit('close')
    })
    .catch(e => {
      progress.value = false
    })
}

const search = () => {
  progress.value = true
  dataService.wikipediaSearch(currentAuthor.value.name, searchlanguage.value)
  .then(res => {
    console.log(res)
    progress.value = false
    searchResult.value = res
  })
  .catch(e => {
    progress.value = false
  })
}

const toHtmlLink = (link: string) => {
  if (link != null) {
    return `<a href="${link}" target="_blank">${link}</a>`
  }
  return ''
}

const fillFormWithEntry = (entry: WikipediaSearchResultElement) => {
  progress.value = true
  dataService.wikipediaPage(entry.title, searchlanguage.value)
  .then(res => {
    currentAuthor.value.biography = res.extractHtml
    currentAuthor.value.wikipediaPage = res.contentUrls.desktop.page
    imageUrl.value = res.thumbnail?.source
    // we have a picture in search result but not in page result...
    // so use the search result picture as final picture
    if ((imageUrl.value === null || imageUrl.value === undefined) && entry.thumbnail !== null && entry.thumbnail !== undefined && entry.thumbnail.url !== null) {
      imageUrl.value = 'https:' + entry.thumbnail?.url
    }
    progress.value = false
    currentPhase.value = FORM
  })
  .catch(e => {
    progress.value = false
  })
}

</script>

<template>
  <section class="author-modal">
    <div
      v-if="currentPhase == FORM"
    >
      <div>
        <div>
          <h1 class="text-2xl typewriter">
            Edit author
          </h1>
        </div>
      </div>
      <div class="form-control">
        <div class="field pb-2">
          <o-field
            label="Name"
          >
            <o-input
              v-model="currentAuthor.name"
              maxlength="1000"
              name="name"
              class="input focus:input-accent"
            />
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            label="Date of birth"
          >
            <o-datepicker
              ref="datepicker"
              v-model="currentAuthor.dateOfBirth"
              :show-week-number="false"
              :locale="undefined"
              placeholder="Click to select..."
              :expanded="true"
              icon="calendar"
              icon-right="close"
              icon-right-clickable="true"
              mobile-native="false"
              mobile-modal="false"
              trap-focus
              class="input focus:input-accent"
              @icon-right-click="currentAuthor.dateOfBirth = undefined"
            />
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            label="Date of death"
          >
            <o-datepicker
              ref="datepicker"
              v-model="currentAuthor.dateOfDeath"
              :show-week-number="false"
              :locale="undefined"
              placeholder="Click to select..."
              :expanded="true"
              icon="calendar"
              icon-right="close"
              icon-right-clickable="true"
              mobile-native="false"
              mobile-modal="false"
              trap-focus
              class="input focus:input-accent"
              @icon-right-click="currentAuthor.dateOfDeath = undefined"
            />
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            label="Biography"
          >
            <o-input
              v-model="currentAuthor.biography"
              maxlength="5000"
              type="textarea"
              class="textarea focus:textarea-accent"
            />
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            label="Official page"
          >
            <o-input
              v-model="currentAuthor.officialPage"
              maxlength="5000"
              class="input focus:input-accent"
            />
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            label="Wikipedia page"
          >
            <o-input
              v-model="currentAuthor.wikipediaPage"
              maxlength="5000"
              class="input focus:input-accent"
            />
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            label="Goodreads page"
          >
            <o-input
              v-model="currentAuthor.goodreadsPage"
              maxlength="5000"
              class="input focus:input-accent"
            />
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            label="Twitter page"
          >
            <o-input
              v-model="currentAuthor.twitterPage"
              maxlength="5000"
              class="input focus:input-accent"
            />
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            label="Facebook page"
          >
            <o-input
              v-model="currentAuthor.facebookPage"
              maxlength="5000"
              class="input focus:input-accent"
            />
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            label="Instagram page"
          >
            <o-input
              v-model="currentAuthor.instagramPage"
              maxlength="5000"
              class="input focus:input-accent"
            />
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            label="Personal notes"
          >
            <o-input
              v-model="currentAuthor.notes"
              maxlength="5000"
              type="textarea"
              class="textarea focus:textarea-accent"
            />
          </o-field>
        </div>
        <div v-if="hasImage">
          <o-field class="pb-6">
            <template #label>
              Actual cover :
              <o-tooltip
                v-if="!deleteImage"
                label="Click bin to remove current image and upload another one"
                multiline
                position="right"
              >
                <span class="icon">
                  <i class="mdi mdi-information-outline" />
                </span>
              </o-tooltip>
              <o-tooltip
                v-if="deleteImage"
                label="Press refresh to restore image"
                position="right"
              >
                <span class="icon">
                  <i class="mdi mdi-information-outline" />
                </span>
              </o-tooltip>
            </template>
            <div class="indicator">
              <span
                v-if="!deleteImage"
                class="badge indicator-item indicator-bottom indicator-start"
                @click="toggleRemoveImage"
              >
                <i class="mdi mdi-delete" />
              </span>
              <span
                v-if="deleteImage"
                class="badge indicator-item indicator-bottom indicator-start"
                @click="toggleRemoveImage"
              >
                <i class="mdi mdi-autorenew" />
              </span>
              <figure class="small-cover">
                <img
                  :src="'/files/' + author.image"
                  :class="deleteImage ? 'altered' : ''"
                  alt="cover image"
                >
              </figure>
            </div>
          </o-field>
        </div>
        <div
          v-if="!hasImage || deleteImage"
          class="py-2"
        >
          <o-field
            label="Upload image"
          >
            <o-switch
              v-model="isSwitchedCustom"
              true-value="Upload from file"
              false-value="Upload from the web"
              :left-label="true"
            >
              {{ isSwitchedCustom }}
            </o-switch>
          </o-field>
          <o-field
            v-if="isSwitchedCustom == 'Upload from the web'"
            label="Enter image adress"
          >
            <o-input
              v-model="imageUrl"
              type="url"
              pattern="https?://.*"
              clearable="true"
              icon-right-clickable
              title="Url must start with http or https"
              placeholder="Url must start with http or https"
              class="input focus:input-accent"
              @icon-right-click="clearImageField"
            />
          </o-field>
          <o-field
            v-else
            label="Choose file"
            class="file is-primary has-name"
          >
            <input
              type="file"
              accept="image/*"
              class="block w-full text-sm text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-gray-50 file:text-primary hover:file:bg-gray-300"
              @change="handleFileUpload($event)"
            >
            <br>
            <progress
              max="100"
              :value.prop="uploadPercentage"
              class="progress progress-primary"
            />
            <br>
          </o-field>
        </div>
        <div class="mt-2 flex flex-row justify-center space-x-8">
          <button
            class="btn btn-primary button btn-outline"
            @click="update"
          >
            <span class="icon">
              <i class="mdi mdi-content-save mdi-18px" />
            </span>
            <span>Save changes</span>
          </button>
          <button
            class="btn btn-info btn-outline"
            @click="currentPhase = SEARCH"
          >
            <span class="icon">
              <i class="mdi mdi-auto-fix mdi-18px" />
            </span>
            <span>Auto fill</span>
          </button>
        </div>
      </div>
      <progress
        v-if="progress"
        class="animate-pulse progress progress-success mt-5"
        max="100"
      />
    </div>
    <div
      v-else-if="currentPhase == SEARCH"
    >
      <div>
        <div>
          <h1 class="text-2xl typewriter">
            Search
          </h1>
        </div>
      </div>
      <div class="mt-2">
        <div class="field">
          <o-field
            label="Short language code"
          >
            <o-input
              v-model="searchlanguage"
              maxlength="4"
              name="language"
              class="input focus:input-accent"
            />
          </o-field>
        </div>
        <div class="mb-2 flex flex-row justify-center space-x-8 my-4">
          <button
            class="btn btn-info btn-outline"
            @click="search"
          >
            <span class="icon">
              <i class="mdi mdi-magnify" />
            </span>
            <span>Search</span>
          </button>
          <button
            class="btn btn-warning btn-outline"
            @click="currentPhase = FORM"
          >
            <span class="icon">
              <i class="mdi mdi-arrow-u-left-top" />
            </span>
            <span>Back</span>
          </button>
        </div>
        <div v-if="searchResult.pages.length > 0">
          <div 
            v-for="res in searchResult.pages"
            :key="res.id"
            v-tooltip="'click to import'"
            class="card card-side box mb-2 shadow-lg shadow-base-300 hover:shadow-2xl hover:border-2 hover:border-accent"
            @click="fillFormWithEntry(res)"
          >
            <figure
              v-if="res.thumbnail?.url"
              class="small-cover"
            >
              <img
                :src="'https:' + res.thumbnail?.url"
                :class="deleteImage ? 'altered' : ''"
                alt="search thumbnail"
              >
            </figure>
            <div class="card-body media-content">
              <h2 class="card-title">
                {{ res.title }}
              </h2>
              <div class="content">
                <p>
                  {{ res.description }}
                </p>
              </div>
            </div>
          </div>
        </div>
        <div v-else>
          Choose in which language the search should happen.
          <br>
          If result is empty try another language.
        </div>
      </div>
      <progress
        v-if="progress"
        class="animate-pulse progress progress-success mt-5"
        max="100"
      />
    </div>
  </section>
</template>

<style lang="scss">

</style>
