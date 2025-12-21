<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next"
import { useTitle } from '@vueuse/core'
import { computed, Ref, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Author } from "../model/Author"
import dataService from "../services/DataService"
import { StringUtils } from "../utils/StringUtils"
import dayjs from "dayjs";
import useDates from '../composables/dates'
import { ObjectUtils } from "../utils/ObjectUtils";
import { useI18n } from 'vue-i18n'
import FormField from "./FormField.vue";

useTitle('Jelu | Authors admin')

const { t, locale, availableLocales } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const router = useRouter()
const oruga = useOruga();

const { formatDate, formatDateString } = useDates()

const isFetching = ref(false)

const isMerging = ref(false)

const activeStep = ref("left")

const filteredAuthors: Ref<Array<Author>> = ref([]);

function getFilteredAuthors(text: string) {
  isFetching.value = true
  dataService.findAuthorByCriteria(text).then((data) => filteredAuthors.value = data.content)
  isFetching.value = false
}

const leftAuthor: Ref<Author> = ref({ "name": "" })
const rightAuthor: Ref<Author> = ref({ "name": "" })
const leftHasImage = computed(() => {
  return StringUtils.isNotBlank(leftAuthor.value.image)
})
const rightHasImage = computed(() => {
  return StringUtils.isNotBlank(rightAuthor.value.image)
})
const leftDeleteImage: Ref<boolean> = ref(false)


function dispatchAuthor(author: Author) {
  console.log(author)
  console.log(`step ${activeStep.value}`)
  // we receive from oruga weird events while nothing is selected
  // so try to get rid of those null data we receive
  if (author != null) {
    if (author.dateOfBirth != null) {
      author.dateOfBirth = dayjs(author.dateOfBirth).toDate()
    }
    if (author.dateOfDeath != null) {
      author.dateOfDeath = dayjs(author.dateOfDeath).toDate()
    }
    if (activeStep.value === "left") {
      leftAuthor.value = author
      activeStep.value = "right"
    }
    else if (activeStep.value === "right") {
      rightAuthor.value = author
      activeStep.value = "merge"
    }
  }
}

function save() {
  if (leftAuthor.value.id != null && rightAuthor.value.id != null) {
    isMerging.value = true
    dataService.mergeAuthors(leftAuthor.value.id, rightAuthor.value.id, leftAuthor.value)
      .then(res => {
        console.log("merged author " + res)
        isMerging.value = false
        router.push({ name: 'author-detail', params: { authorId: leftAuthor.value.id } })
      })
      .catch(err => {
        console.log("merge failure " + err)
        isMerging.value = false
        ObjectUtils.toast(oruga, "danger", `Error ` + err.message, 4000)
      })
  }
  else {
    console.log("missing ids on authors")
  }
}

const rightDoB = computed(() => {
  if (rightAuthor.value.dateOfBirth != null) {
    return formatDate(rightAuthor.value.dateOfBirth)
  }
  return ""
})

const rightDoD = computed(() => {
  if (rightAuthor.value.dateOfDeath != null) {
    return formatDate(rightAuthor.value.dateOfDeath)
  }
  return ""
})

const options = computed(() => {
  return filteredAuthors.value.map(t => ObjectUtils.wrapForOptions(t))
})
</script>

<template>
  <div class="grid grid-cols-2 gap-10 mr-2">
    <div class="col-span-2 justify-self-center">
      <h1 class="text-2xl typewriter mb-4">
        {{ t('authors_merge.authors_merging_utility') }}
      </h1>
      <o-steps
        v-model="activeStep"
        :animated="true"
        :rounded="true"
        :has-navigation="false"
        label-position="bottom"
        selectable-footer="false"
        selectable-header="false"
        select-on-click-outside="false"
        check-infinite-scroll="false"
        keep-first="false"
      >
        <o-step-item
          step="1"
          value="left"
          :label="t('authors_merge.author_nb', {nb : 1})"
          :clickable="false"
          variant="success"
        >
          <h1 class="text-xl">
            {{ t('authors_merge.author_left_subtitle') }}
          </h1>{{ t('authors_merge.author_left_description') }}
        </o-step-item>

        <o-step-item
          step="2"
          value="right"
          :label="t('authors_merge.author_nb', {nb : 2})"
          :clickable="false"
          variant="success"
        >
          <h1 class="text-xl">
            {{ t('authors_merge.author_right_subtitle') }}
          </h1>{{ t('authors_merge.author_right_description') }}
        </o-step-item>

        <o-step-item
          step="3"
          value="merge"
          :label="t('authors_merge.authors_merge_subtitle')"
          :clickable="false"
          variant="success"
        >
          <h1 class="text-xl">
            {{ t('authors_merge.authors_merge_subtitle') }}
          </h1>{{ t('authors_merge.authors_merge_description') }}
        </o-step-item>
      </o-steps>
      <fieldset class="fieldset">
        <legend class="fieldset-legend capitalize">
          {{ t('authors_merge.search_message') }}
        </legend>
        <o-autocomplete
          :options="options" 
          :input-classes="{rootClass:'border-2 border-accent w-full', inputClass: 'w-full'}"
          clear-on-select
          :loading="isFetching"
          :debounce="100"
          @input="getFilteredAuthors"
          @select="dispatchAuthor"
        >
          <template #default="{ value }">
            <div class="jl-taginput-item">
              {{ value.name }}
            </div>
          </template>
        </o-autocomplete>
      </fieldset>
    </div>
    <div class="justify-self-center col-span-2 sm:col-span-1 w-11/12 sm:w-8/12">
      <FormField
        v-model="leftAuthor.name"
        :legend="t('author.name')"
        placeholder=""
      />
      <fieldset class="fieldset">
        <legend class="fieldset-legend capitalize">
          {{ t('author.date_of_birth') }}
        </legend>
        <o-datepicker
          ref="datepicker"
          v-model="leftAuthor.dateOfBirth"
          :show-week-number="false"
          :locale="undefined"
          :placeholder="t('labels.click_to_select')"
          :expanded="true"
          icon="calendar"
          icon-right="close"
          icon-right-clickable="true"
          :mobile-native="false"
          mobile-modal="false"
          trap-focus
          @icon-right-click="leftAuthor.dateOfBirth = undefined"
        />
      </fieldset>
      <fieldset class="fieldset">
        <legend class="fieldset-legend capitalize">
          {{ t('author.date_of_death') }}
        </legend>
        <o-datepicker
          ref="datepicker"
          v-model="leftAuthor.dateOfDeath"
          :show-week-number="false"
          :locale="undefined"
          :placeholder="t('labels.click_to_select')"
          :expanded="true"
          icon="calendar"
          icon-right="close"
          icon-right-clickable="true"
          mobile-native="false"
          mobile-modal="false"
          trap-focus
          @icon-right-click="leftAuthor.dateOfDeath = undefined"
        />
      </fieldset>
      <fieldset class="fieldset">
        <legend class="fieldset-legend capitalize">
          {{ t('author.biography') }}
        </legend>
        <textarea
          v-model="leftAuthor.biography"
          class="textarea focus:textarea-accent w-full"
          maxlength="5000"
        />
      </fieldset>
      <form-field
        v-model="leftAuthor.officialPage"
        :legend="t('author.official_page')"
      />
      <form-field
        v-model="leftAuthor.wikipediaPage"
        :legend="t('author.wikipedia_page')"
      />
      <FormField
        v-model="leftAuthor.goodreadsPage"
        :legend="t('author.goodreads_page')"
        placeholder=""
      />
      <FormField
        v-model="leftAuthor.twitterPage"
        :legend="t('author.x_page')"
        placeholder=""
      />
      <FormField
        v-model="leftAuthor.facebookPage"
        :legend="t('author.facebook_page')"
        placeholder=""
      />
      <FormField
        v-model="leftAuthor.instagramPage"
        :legend="t('author.instagram_page')"
        placeholder=""
      />
      <fieldset class="fieldset">
        <legend class="fieldset-legend capitalize">
          {{ t('author.personal_notes') }}
        </legend>
        <textarea
          v-model="leftAuthor.notes"
          class="textarea w-full focus:textarea-accent"
          maxlength="5000"
        />
      </fieldset>
      <div
        v-if="leftHasImage"
        class="w-full"
      >
        <div class="indicator w-full">
          <span
            class="badge indicator-item indicator-bottom indicator-start cursor-pointer"
            @click="leftAuthor.image = undefined"
          >
            <i class="mdi mdi-delete" />
          </span>
          <figure class="">
            <img
              :src="'/files/' + leftAuthor.image"
              :class="leftDeleteImage ? 'altered' : ''"
              alt="cover image"
            >
          </figure>
        </div>
      </div>
    </div>
    <div class="justify-self-center w-11/12 sm:w-8/12 col-span-2 sm:col-span-1">
      <fieldset class="fieldset">
        <legend class="fieldset-legend capitalize">
          {{ t('author.name') }}
        </legend>
        <label class="input w-full">
          <svg 
            v-if="rightAuthor.name"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke-width="1.5"
            stroke="currentColor"
            class="size-6 hover:cursor-pointer"
            @click="leftAuthor.name = rightAuthor.name"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18"
            />
          </svg>
          <input
            v-model="rightAuthor.name"
            type="text"
            class="grow input" 
            disabled
          >
        </label>
      </fieldset>
      <fieldset class="fieldset">
        <legend class="fieldset-legend capitalize">
          {{ t('author.date_of_birth') }}
        </legend>
        <label class="input w-full">
          <svg 
            v-if="rightAuthor.dateOfBirth"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke-width="1.5"
            stroke="currentColor"
            class="size-6 hover:cursor-pointer"
            @click="leftAuthor.dateOfBirth = rightAuthor.dateOfBirth"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18"
            />
          </svg>
          <input
            v-model="rightDoB"
            type="text"
            class="grow input" 
            disabled
          >
        </label>
      </fieldset>
      <fieldset class="fieldset">
        <legend class="fieldset-legend capitalize">
          {{ t('author.date_of_death') }}
        </legend>
        <label class="input w-full">
          <svg 
            v-if="rightAuthor.dateOfDeath"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke-width="1.5"
            stroke="currentColor"
            class="size-6 hover:cursor-pointer"
            @click="leftAuthor.dateOfDeath = rightAuthor.dateOfDeath"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18"
            />
          </svg>
          <input
            v-model="rightDoD"
            type="text"
            class="grow input" 
            disabled
          >
        </label>
      </fieldset>
      <fieldset class="fieldset sm:mb-5">
        <legend class="fieldset-legend capitalize">
          {{ t('author.biography') }}
        </legend>
        <label class="input w-full">
          <svg 
            v-if="rightAuthor.biography"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke-width="1.5"
            stroke="currentColor"
            class="size-6 hover:cursor-pointer"
            @click="leftAuthor.biography = rightAuthor.biography"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18"
            />
          </svg>
          <input
            v-model="rightAuthor.biography"
            type="text"
            class="grow input" 
            disabled
          >
        </label>
      </fieldset>
      <fieldset class="fieldset">
        <legend class="fieldset-legend capitalize">
          {{ t('author.official_page') }}
        </legend>
        <label class="input w-full">
          <svg 
            v-if="rightAuthor.officialPage"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke-width="1.5"
            stroke="currentColor"
            class="size-6 hover:cursor-pointer"
            @click="leftAuthor.officialPage = rightAuthor.officialPage"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18"
            />
          </svg>
          <input
            v-model="rightAuthor.officialPage"
            type="text"
            class="grow input" 
            disabled
          >
        </label>
      </fieldset>
      <fieldset class="fieldset">
        <legend class="fieldset-legend capitalize">
          {{ t('author.wikipedia_page') }}
        </legend>
        <label class="input w-full">
          <svg 
            v-if="rightAuthor.wikipediaPage"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke-width="1.5"
            stroke="currentColor"
            class="size-6 hover:cursor-pointer"
            @click="leftAuthor.wikipediaPage = rightAuthor.wikipediaPage"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18"
            />
          </svg>
          <input
            v-model="rightAuthor.wikipediaPage"
            type="text"
            class="grow input" 
            disabled
          >
        </label>
      </fieldset>
      <fieldset class="fieldset">
        <legend class="fieldset-legend capitalize">
          {{ t('author.goodreads_page') }}
        </legend>
        <label class="input w-full">
          <svg 
            v-if="rightAuthor.goodreadsPage"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke-width="1.5"
            stroke="currentColor"
            class="size-6 hover:cursor-pointer"
            @click="leftAuthor.goodreadsPage = rightAuthor.goodreadsPage"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18"
            />
          </svg>
          <input
            v-model="rightAuthor.goodreadsPage"
            type="text"
            class="input" 
            disabled
          >
        </label>
      </fieldset>
      <fieldset class="fieldset">
        <legend class="fieldset-legend capitalize">
          {{ t('author.x_page') }}
        </legend>
        <label class="input w-full">
          <svg 
            v-if="rightAuthor.twitterPage"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke-width="1.5"
            stroke="currentColor"
            class="size-6 hover:cursor-pointer"
            @click="leftAuthor.twitterPage = rightAuthor.twitterPage"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18"
            />
          </svg>
          <input
            v-model="rightAuthor.twitterPage"
            type="text"
            class="input" 
            disabled
          >
        </label>
      </fieldset>
      <fieldset class="fieldset">
        <legend class="fieldset-legend capitalize">
          {{ t('author.facebook_page') }}
        </legend>
        <label class="input w-full">
          <svg 
            v-if="rightAuthor.facebookPage"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke-width="1.5"
            stroke="currentColor"
            class="size-6 hover:cursor-pointer"
            @click="leftAuthor.facebookPage = rightAuthor.facebookPage"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18"
            />
          </svg>
          <input
            v-model="rightAuthor.facebookPage"
            type="text"
            class="input" 
            disabled
          >
        </label>
      </fieldset>
      <fieldset class="fieldset">
        <legend class="fieldset-legend capitalize">
          {{ t('author.instagram_page') }}
        </legend>
        <label class="input w-full">
          <svg 
            v-if="rightAuthor.instagramPage"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke-width="1.5"
            stroke="currentColor"
            class="size-6 hover:cursor-pointer"
            @click="leftAuthor.instagramPage = rightAuthor.instagramPage"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18"
            />
          </svg>
          <input
            v-model="rightAuthor.instagramPage"
            type="text"
            class="input" 
            disabled
          >
        </label>
      </fieldset>
      <fieldset class="fieldset">
        <legend class="fieldset-legend capitalize">
          {{ t('author.personal_notes') }}
        </legend>
        <label class="input w-full">
          <svg 
            v-if="rightAuthor.notes"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke-width="1.5"
            stroke="currentColor"
            class="size-6 hover:cursor-pointer"
            @click="leftAuthor.notes = rightAuthor.notes"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18"
            />
          </svg>
          <input
            v-model="rightAuthor.notes"
            type="text"
            class="input" 
            disabled
          >
        </label>
      </fieldset>
      <div
        v-if="rightHasImage"
        class="sm:mt-3"
      >
        <div class="indicator w-full">
          <span
            class="badge indicator-item indicator-bottom indicator-start cursor-pointer"
            @click="leftAuthor.image = rightAuthor.image"
          >
            <i class="mdi mdi-arrow-left-bold" />
          </span>
          <figure class="">
            <img
              :src="'/files/' + rightAuthor.image"
              :class="leftDeleteImage ? 'altered' : ''"
              alt="cover image"
            >
          </figure>
        </div>
      </div>
    </div>
    <div class="mt-2 col-span-2">
      <button
        class="btn btn-primary mb-4 uppercase"
        :disabled="activeStep !== 'merge'"
        @click="save"
      >
        <span class="icon">
          <i class="mdi mdi-content-save mdi-18px" />
        </span>
        <span>{{ t('author.merge_changes') }}</span>
      </button>
    </div>
  </div>
  <o-loading
    v-model:active="isMerging"
    :full-page="true"
    :cancelable="true"
  />
</template>

<style scoped>
  .o-dropdown.o-dropdown--position-auto.o-autocomplete, 
  .o-dropdown.o-dropdown--position-bottom.o-autocomplete {
    @apply w-full;
  }
</style>
