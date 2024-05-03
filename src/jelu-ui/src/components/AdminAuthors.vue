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

let filteredAuthors: Ref<Array<Author>> = ref([]);

function getFilteredAuthors(text: string) {
  isFetching.value = true
  dataService.findAuthorByCriteria(text).then((data) => filteredAuthors.value = data.content)
  isFetching.value = false
}

const leftAuthor: Ref<Author> = ref({ "name": "" })
const rightAuthor: Ref<Author> = ref({ "name": "" })
let leftHasImage = computed(() => {
  return StringUtils.isNotBlank(leftAuthor.value.image)
})
let rightHasImage = computed(() => {
  return StringUtils.isNotBlank(rightAuthor.value.image)
})
let leftDeleteImage: Ref<boolean> = ref(false)


function dispatchAuthor(author: Author, event: UIEvent) {
  console.log(author)
  console.log(`step ${activeStep.value}`)
  console.log(event)
  // we receive from oruga weird events while nothing is selected
  // so try to get rid of those null data we receive
  if (author != null && event != null) {
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
      <div class="field">
        <o-field :label="t('authors_merge.search_message')">
          <o-autocomplete
            :data="filteredAuthors" 
            :input-classes="{rootClass:'border-2 border-accent'}"
            :clear-on-select="true"
            field="name"
            :loading="isFetching"
            :debounce="100"
            @input="getFilteredAuthors"
            @select="dispatchAuthor"
          />
        </o-field>
      </div>
    </div>
    <div class="justify-self-center col-span-2 sm:col-span-1 w-11/12 sm:w-8/12">
      <div class="field">
        <o-field
          :label="t('author.name')"
          class="capitalize"
        >
          <o-input
            v-model="leftAuthor.name"
            maxlength="1000"
            name="name"
          />
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.date_of_birth')"
          class="capitalize"
        >
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
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.date_of_death')"
          class="capitalize"
        >
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
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.biography')"
          class="capitalize"
        >
          <o-input
            v-model="leftAuthor.biography"
            maxlength="5000"
            type="textarea"
          />
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.official_page')"
          class="capitalize"
        >
          <o-input
            v-model="leftAuthor.officialPage"
            maxlength="5000"
          />
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.wikipedia_page')"
          class="capitalize"
        >
          <o-input
            v-model="leftAuthor.wikipediaPage"
            maxlength="5000"
          />
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.goodreads_page')"
          class="capitalize"
        >
          <o-input
            v-model="leftAuthor.goodreadsPage"
            maxlength="5000"
          />
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.twitter_page')"
          class="capitalize"
        >
          <o-input
            v-model="leftAuthor.twitterPage"
            maxlength="5000"
          />
        </o-field>
      </div>
      <div
        :label="t('author.facebook_page')"
        class="capitalize"
      >
        <o-field label="Facebook page">
          <o-input
            v-model="leftAuthor.facebookPage"
            maxlength="5000"
          />
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.instagram_page')"
          class="capitalize"
        >
          <o-input
            v-model="leftAuthor.instagramPage"
            maxlength="5000"
          />
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.personal_notes')"
          class="capitalize"
        >
          <o-input
            v-model="leftAuthor.notes"
            maxlength="5000"
            type="textarea"
          />
        </o-field>
      </div>
      <div v-if="leftHasImage">
        <o-field>
          <div class="indicator">
            <span
              class="badge indicator-item indicator-bottom indicator-start cursor-pointer"
              @click="leftAuthor.image = undefined"
            >
              <i class="mdi mdi-delete" />
            </span>
            <figure class="small-cover">
              <img
                :src="'/files/' + leftAuthor.image"
                :class="leftDeleteImage ? 'altered' : ''"
                alt="cover image"
              >
            </figure>
          </div>
        </o-field>
      </div>
    </div>
    <div class="justify-self-center w-11/12 sm:w-8/12 col-span-2 sm:col-span-1">
      <div class="field">
        <o-field
          :label="t('author.name')"
          class="capitalize"
        >
          <o-input
            v-model="rightAuthor.name"
            maxlength="1000"
            name="name"
            icon-pack="mdi"
            readonly
            :icon="rightAuthor.name !== null ? 'arrow-left-bold' : ''"
            :icon-clickable="rightAuthor.name !== null"
            @icon-click="leftAuthor.name = rightAuthor.name"
          />
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.date_of_birth')"
          class="capitalize"
        >
          <o-input
            v-model="rightDoB"
            maxlength="1000"
            name="dob"
            icon-pack="mdi"
            expanded="true"
            readonly
            :icon="rightAuthor.dateOfBirth !== null ? 'arrow-left-bold' : ''"
            :icon-clickable="rightAuthor.dateOfBirth !== null"
            @icon-click="leftAuthor.dateOfBirth = rightAuthor.dateOfBirth"
          />
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.date_of_death')"
          class="capitalize"
        >
          <o-input
            v-model="rightDoD"
            maxlength="1000"
            name="dod"
            icon-pack="mdi"
            readonly
            :icon="rightAuthor.dateOfDeath !== null ? 'arrow-left-bold' : ''"
            :icon-clickable="rightAuthor.dateOfDeath !== null"
            @icon-click="leftAuthor.dateOfDeath = rightAuthor.dateOfDeath"
          />
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.biography')"
          class="capitalize"
        >
          <o-input
            v-model="rightAuthor.biography"
            maxlength="5000"
            type="textarea"
            icon-pack="mdi"
            :icon="rightAuthor.biography !== null ? 'arrow-left-bold' : ''"
            readonly
            :icon-clickable="rightAuthor.biography !== null"
            @icon-click="leftAuthor.biography = rightAuthor.biography"
          />
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.official_page')"
          class="capitalize"
        >
          <o-input
            v-model="rightAuthor.officialPage"
            maxlength="5000"
            icon-pack="mdi"
            :icon="rightAuthor.officialPage !== null ? 'arrow-left-bold' : ''"
            readonly
            :icon-clickable="rightAuthor.officialPage !== null"
            @icon-click="leftAuthor.officialPage = rightAuthor.officialPage"
          />
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.wikipedia_page')"
          class="capitalize"
        >
          <o-input
            v-model="rightAuthor.wikipediaPage"
            maxlength="5000"
            icon-pack="mdi"
            :icon="rightAuthor.wikipediaPage !== null ? 'arrow-left-bold' : ''"
            readonly
            :icon-clickable="rightAuthor.wikipediaPage !== null"
            @icon-click="leftAuthor.wikipediaPage = rightAuthor.wikipediaPage"
          />
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.goodreads_page')"
          class="capitalize"
        >
          <o-input
            v-model="rightAuthor.goodreadsPage"
            maxlength="5000"
            icon-pack="mdi"
            :icon="rightAuthor.goodreadsPage !== null ? 'arrow-left-bold' : ''"
            readonly
            :icon-clickable="rightAuthor.goodreadsPage !== null"
            @icon-click="leftAuthor.goodreadsPage = rightAuthor.goodreadsPage"
          />
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.twitter_page')"
          class="capitalize"
        >
          <o-input
            v-model="rightAuthor.twitterPage"
            maxlength="5000"
            icon-pack="mdi"
            :icon="rightAuthor.twitterPage !== null ? 'arrow-left-bold' : ''"
            readonly
            :icon-clickable="rightAuthor.twitterPage !== null"
            @icon-click="leftAuthor.twitterPage = rightAuthor.twitterPage"
          />
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.facebook_page')"
          class="capitalize"
        >
          <o-input
            v-model="rightAuthor.facebookPage"
            maxlength="5000"
            icon-pack="mdi"
            :icon="rightAuthor.facebookPage !== null ? 'arrow-left-bold' : ''"
            readonly
            :icon-clickable="rightAuthor.facebookPage !== null"
            @icon-click="leftAuthor.facebookPage = rightAuthor.facebookPage"
          />
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.instagram_page')"
          class="capitalize"
        >
          <o-input
            v-model="rightAuthor.instagramPage"
            maxlength="5000"
            icon-pack="mdi"
            :icon="rightAuthor.instagramPage !== null ? 'arrow-left-bold' : ''"
            readonly
            :icon-clickable="rightAuthor.instagramPage !== null"
            @icon-click="leftAuthor.instagramPage = rightAuthor.instagramPage"
          />
        </o-field>
      </div>
      <div class="field">
        <o-field
          :label="t('author.personal_notes')"
          class="capitalize"
        >
          <o-input
            v-model="rightAuthor.notes"
            maxlength="5000"
            type="textarea"
            icon-pack="mdi"
            :icon="rightAuthor.notes !== null ? 'arrow-left-bold' : ''"
            readonly
            :icon-clickable="rightAuthor.notes !== null"
            @icon-click="leftAuthor.notes = rightAuthor.notes"
          />
        </o-field>
      </div>
      <div v-if="rightHasImage">
        <o-field>
          <div class="indicator">
            <span
              class="badge indicator-item indicator-bottom indicator-start cursor-pointer"
              @click="leftAuthor.image = rightAuthor.image"
            >
              <i class="mdi mdi-arrow-left-bold" />
            </span>
            <figure class="small-cover">
              <img
                :src="'/files/' + rightAuthor.image"
                :class="leftDeleteImage ? 'altered' : ''"
                alt="cover image"
              >
            </figure>
          </div>
        </o-field>
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

<style lang="scss" scoped>
</style>
