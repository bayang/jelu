<script setup lang="ts">
import { computed, Ref, ref } from "vue";
import { Author } from "../model/Author";
import { WikipediaSearchResult, WikipediaSearchResultElement } from "../model/WikipediaSearchResult";
import dataService from "../services/DataService";
import { StringUtils } from "../utils/StringUtils";
import { useI18n } from 'vue-i18n'
import { useLocalStorage } from '@vueuse/core'

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const props = defineProps<{
  author : Author
}>()

const currentAuthor: Ref<Author> = ref(props.author)
console.log(currentAuthor.value)
const progress: Ref<boolean> = ref(false)
const deleteImage: Ref<boolean> = ref(false)
const uploadFromWeb = ref(true);
const uploadlabel = computed(() => {
  if (uploadFromWeb.value) {
    return t('labels.upload_from_web')
  } else {
    return t('labels.upload_from_file')
  }
}) 
const imageUrl = ref<string | null>(null);
const file = ref(null);
const uploadPercentage = ref(0);

const storedLanguage = useLocalStorage("jelu_language", "en")
const searchlanguage = ref(storedLanguage.value);
const FORM = "FORM"
const SEARCH = "SEARCH"
const currentPhase = ref(FORM)
const searchResult: Ref<WikipediaSearchResult> = ref({pages: []})

const emit = defineEmits<{
  (e: 'close'): void
}>()

const hasImage = computed(() => {
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
          const percent = Math.round((100 * event.loaded) / event.total);
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
          <h1 class="text-2xl typewriter first-letter:capitalize">
            {{ t('labels.edit_author') }}
          </h1>
        </div>
      </div>
      <div class="form-control">
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('author.name') }}
          </legend>
          <input
            v-model="currentAuthor.name"
            type="text"
            maxlength="1000"
            name="name"
            class="input focus:input-accent w-full"
          >
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('author.date_of_birth') }}
          </legend>
          <o-datepicker
            ref="datepicker"
            v-model="currentAuthor.dateOfBirth"
            :show-week-number="false"
            :locale="undefined"
            :placeholder="t('labels.click_to_select')"
            expanded
            icon="calendar"
            icon-right="close"
            :icon-right-clickable="true"
            :mobile-native="false"
            :mobile-modal="false"
            trap-focus
            @icon-right-click="currentAuthor.dateOfBirth = undefined"
          />
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('author.date_of_death') }}
          </legend>
          <o-datepicker
            ref="datepicker"
            v-model="currentAuthor.dateOfDeath"
            :show-week-number="false"
            :locale="undefined"
            :placeholder="t('labels.click_to_select')"
            expanded
            icon="calendar"
            icon-right="close"
            :icon-right-clickable="true"
            :mobile-native="false"
            :mobile-modal="false"
            trap-focus
            @icon-right-click="currentAuthor.dateOfDeath = undefined"
          />
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('author.biography') }}
          </legend>
          <textarea
            v-model="currentAuthor.biography"
            maxlength="5000"
            type="textarea"
            class="textarea focus:textarea-accent w-full"
          />
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('author.official_page') }}
          </legend>
          <input
            v-model="currentAuthor.officialPage"
            type="text"
            maxlength="5000"
            class="input focus:input-accent w-full"
          >
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('author.wikipedia_page') }}
          </legend>
          <input
            v-model="currentAuthor.wikipediaPage"
            type="text"
            maxlength="5000"
            class="input focus:input-accent w-full"
          >
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('author.goodreads_page') }}
          </legend>
          <input
            v-model="currentAuthor.goodreadsPage"
            maxlength="5000"
            type="text"
            class="input focus:input-accent w-full"
          >
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('author.x_page') }}
          </legend>
          <input
            v-model="currentAuthor.twitterPage"
            maxlength="5000"
            type="text"
            class="input focus:input-accent w-full"
          >
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('author.facebook_page') }}
          </legend>
          <input
            v-model="currentAuthor.facebookPage"
            maxlength="5000"
            type="text"
            class="input focus:input-accent w-full"
          >
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('author.instagram_page') }}
          </legend>
          <input
            v-model="currentAuthor.instagramPage"
            maxlength="5000"
            type="text"
            class="input focus:input-accent w-full"
          >
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('author.personal_notes') }}
          </legend>
          <textarea
            v-model="currentAuthor.notes"
            maxlength="5000"
            type="textarea"
            class="textarea focus:textarea-accent w-full"
          />
        </fieldset>
        <fieldset
          v-if="hasImage"
          class="fieldset"
        >
          <legend class="fieldset-legend capitalize">
            {{ t('labels.actual_cover') }} :
            <o-tooltip
              v-if="!deleteImage"
              :label="t('labels.click_bin_to_remove')"
              multiline
              position="right"
            >
              <span class="icon">
                <i class="mdi mdi-information-outline" />
              </span>
            </o-tooltip>
            <o-tooltip
              v-if="deleteImage"
              :label="t('labels.refresh_to_restore')"
              position="right"
            >
              <span class="icon">
                <i class="mdi mdi-information-outline" />
              </span>
            </o-tooltip>
          </legend>
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
        </fieldset>
        <div
          v-if="!hasImage || deleteImage"
          class="py-2"
        >
          <fieldset class="fieldset">
            <legend class="fieldset-legend capitalize">
              {{ t('labels.upload_image') }}
            </legend>
            <label class="label">
              <input
                v-model="uploadFromWeb"
                type="checkbox"
                class="toggle toggle-primary"
              >
              {{ uploadlabel }}
            </label>
          </fieldset>
          <fieldset
            v-if="uploadFromWeb"
            class="fieldset"
          >
            <legend class="fieldset-legend capitalize">
              {{ t('labels.enter_image_address') }}
            </legend>
            <label class="input validator w-full">
              <svg
                class="h-[1em] opacity-50"
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
              >
                <g
                  stroke-linejoin="round"
                  stroke-linecap="round"
                  stroke-width="2.5"
                  fill="none"
                  stroke="currentColor"
                >
                  <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
                  <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
                </g>
              </svg>
              <input
                v-model="imageUrl"
                type="url"
                required
                class="w-full"
                :placeholder="t('labels.url_must_start')"
                pattern="https?://.*"
              >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.5"
                stroke="currentColor"
                class="size-6 hover:cursor-pointer"
                @click="clearImageField"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="m9.75 9.75 4.5 4.5m0-4.5-4.5 4.5M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
                />
              </svg>
            </label>
            <p class="validator-hint">
              {{ t('labels.url_must_start') }}
            </p>
          </fieldset>
          <fieldset
            v-else
            class="file fieldset"
          >
            <legend
              class="file fieldset-legend"
            >
              {{ t('labels.choose_file') }}
            </legend>
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
          </fieldset>
        </div>
        <div class="mt-2 flex flex-row justify-center space-x-8">
          <button
            class="btn btn-primary button btn-outline uppercase"
            :class="{'btn-disabled' : progress}"
            @click="update"
          >
            <span
              v-if="progress"
              class="loading loading-spinner"
            />
            <span class="icon">
              <i class="mdi mdi-content-save mdi-18px" />
            </span>
            <span>{{ t('labels.save_changes') }}</span>
          </button>
          <button
            class="btn btn-info btn-outline uppercase"
            @click="currentPhase = SEARCH"
          >
            <span class="icon">
              <i class="mdi mdi-auto-fix mdi-18px" />
            </span>
            <span>{{ t('labels.auto_fill') }}</span>
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
            {{ t('labels.search') }}
          </h1>
        </div>
      </div>
      <div class="mt-2">
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('labels.short_language_code') }}
          </legend>
          <input
            v-model="searchlanguage"
            type="text"
            maxlength="4"
            name="language"
            class="input focus:input-accent"
          >
        </fieldset>
        <div class="mb-2 flex flex-row justify-center space-x-8 my-4">
          <button
            class="btn btn-info btn-outline uppercase"
            @click="search"
          >
            <span class="icon">
              <i class="mdi mdi-magnify" />
            </span>
            <span>{{ t('labels.search') }}</span>
          </button>
          <button
            class="btn btn-warning btn-outline uppercase"
            @click="currentPhase = FORM"
          >
            <span class="icon">
              <i class="mdi mdi-arrow-u-left-top" />
            </span>
            <span>{{ t('labels.back') }}</span>
          </button>
        </div>
        <div v-if="searchResult.pages.length > 0">
          <div 
            v-for="res in searchResult.pages"
            :key="res.id"
            v-tooltip="t('labels.click_to_import')"
            class="card card-side mb-2 shadow-lg shadow-base-300 hover:shadow-2xl hover:border-2 hover:border-accent"
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
          {{ t('labels.author_import_help1') }}
          <br>
          {{ t('labels.author_import_help2') }}
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
