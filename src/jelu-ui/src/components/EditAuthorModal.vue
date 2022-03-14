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
// const currentCreateEvent: Ref<CreateReadingEvent> = ref(props.readingEvent)
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
      class="author-modal-body"
    >
      <div>
        <progress
          v-if="progress"
          class="progress is-small is-success"
          max="100"
        />
        <div>
          <h1 class="title has-text-weight-normal typewriter">
            Edit author
          </h1>
        </div>
      </div>
      <div>
        <div class="field">
          <o-field
            label="Name"
          >
            <o-input
              v-model="currentAuthor.name"
              maxlength="1000"
              name="name"
            />
          </o-field>
        </div>
        <div class="field">
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
              @icon-right-click="currentAuthor.dateOfBirth = undefined"
            />
          </o-field>
        </div>
        <div class="field">
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
              @icon-right-click="currentAuthor.dateOfDeath = undefined"
            />
          </o-field>
        </div>
        <div class="field">
          <o-field
            label="Biography"
          >
            <o-input
              v-model="currentAuthor.biography"
              maxlength="5000"
              type="textarea"
            />
          </o-field>
        </div>
        <div class="field">
          <o-field
            label="Official page"
          >
            <o-input
              v-model="currentAuthor.officialPage"
              maxlength="5000"
            />
          </o-field>
        </div>
        <div class="field">
          <o-field
            label="Wikipedia page"
          >
            <o-input
              v-model="currentAuthor.wikipediaPage"
              maxlength="5000"
            />
          </o-field>
        </div>
        <div class="field">
          <o-field
            label="Goodreads page"
          >
            <o-input
              v-model="currentAuthor.goodreadsPage"
              maxlength="5000"
            />
          </o-field>
        </div>
        <div class="field">
          <o-field
            label="Twitter page"
          >
            <o-input
              v-model="currentAuthor.twitterPage"
              maxlength="5000"
            />
          </o-field>
        </div>
        <div class="field">
          <o-field
            label="Facebook page"
          >
            <o-input
              v-model="currentAuthor.facebookPage"
              maxlength="5000"
            />
          </o-field>
        </div>
        <div class="field">
          <o-field
            label="Instagram page"
          >
            <o-input
              v-model="currentAuthor.instagramPage"
              maxlength="5000"
            />
          </o-field>
        </div>
        <div class="field">
          <o-field
            label="Personal notes"
          >
            <o-input
              v-model="currentAuthor.notes"
              maxlength="5000"
              type="textarea"
            />
          </o-field>
        </div>
        <div v-if="hasImage">
          <o-field>
            <template #label>
              Actual cover :
              <o-tooltip
                v-if="!deleteImage"
                label="Click bin to remove current image and upload another one"
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
            <figure class="small-cover">
              <img
                :src="'/files/' + author.image"
                :class="deleteImage ? 'altered' : ''"
                alt="cover image"
              >
              <span
                v-if="!deleteImage"
                class="icon overlay-button"
                @click="toggleRemoveImage"
              >
                <i class="mdi mdi-delete" />
              </span>
              <span
                v-if="deleteImage"
                class="icon overlay-button"
                @click="toggleRemoveImage"
              >
                <i class="mdi mdi-autorenew" />
              </span>
            </figure>
          </o-field>
        </div>
        <div v-if="!hasImage || deleteImage">
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
              @change="handleFileUpload($event)"
            >
            <br>
            <progress
              max="100"
              :value.prop="uploadPercentage"
            />
            <br>
          </o-field>
        </div>
        <div class="mt-2">
          <button
            class="button is-primary is-light mr-2"
            @click="update"
          >
            <span class="icon">
              <i class="mdi mdi-content-save" />
            </span>
            <span>Save changes</span>
          </button>
          <button
            class="button is-info is-light"
            @click="currentPhase = SEARCH"
          >
            <span class="icon">
              <i class="mdi mdi-auto-fix" />
            </span>
            <span>Auto fill</span>
          </button>
        </div>
      </div>
    </div>
    <div
      v-else-if="currentPhase == SEARCH"
      class="author-modal-body"
    >
      <div>
        <div>
          <h1 class="title has-text-weight-normal typewriter">
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
            />
          </o-field>
        </div>
        <div class="mb-2">
          <button
            class="button is-info is-light mr-2"
            @click="search"
          >
            <span class="icon">
              <i class="mdi mdi-magnify" />
            </span>
            <span>Search</span>
          </button>
          <button
            class="button is-warning is-light"
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
            class="box hoverable lighter-background mb-2"
            @click="fillFormWithEntry(res)"
          >
            <article class="media">
              <div class="media-left">
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
              </div>
              <div class="media-content">
                <div class="content">
                  <p>
                    <strong>{{ res.title }}</strong>
                    <br>
                    {{ res.description }}
                  </p>
                </div>
              </div>
            </article>
          </div>
        </div>
        <div v-else>
          Choose in which language the search should happen.
          <br>
          If result is empty try another language.
        </div>
      </div>
    </div>
    <!-- <progress
      v-if="progress"
      class="progress is-small is-success"
      max="100"
    /> -->
  </section>
</template>

<style lang="scss">

</style>
