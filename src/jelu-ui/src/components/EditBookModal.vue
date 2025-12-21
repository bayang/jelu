<script setup lang="ts">

import { useOruga } from "@oruga-ui/oruga-next";
import dayjs from "dayjs";
import { computed, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import Datepicker from 'vue3-datepicker';
import { Author } from "../model/Author";
import { Wrapper } from "../model/autocomplete-wrapper";
import { UserBook } from "../model/Book";
import { Path } from "../model/DirectoryListing";
import { ReadingEventType } from "../model/ReadingEvent";
import { SeriesOrder } from "../model/Series";
import { Tag } from "../model/Tag";
import dataService from "../services/DataService";
import { ObjectUtils } from "../utils/ObjectUtils";
import { StringUtils } from "../utils/StringUtils";
import ImagePickerModal from "./ImagePickerModal.vue";
import SeriesCompleteInput from "./SeriesCompleteInput.vue";
import ClosableBadge from "./ClosableBadge.vue";
import FormField from "./FormField.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const props = defineProps<{ bookId: string, book: UserBook | null, canAddEvent: boolean }>()
const oruga = useOruga()
const emit = defineEmits(['close']);

const filteredAuthors: Ref<Array<Wrapper>> = ref([]);
const filteredTags: Ref<Array<Wrapper>> = ref([]);
const filteredTranslators: Ref<Array<Wrapper>> = ref([]);
const filteredNarrators: Ref<Array<Wrapper>> = ref([]);
const filteredPublishers: Ref<Array<string>> = ref([])
const userbook: Ref<UserBook> = ref(copyInput(props.book))
const hasImage: Ref<boolean> = ref(userbook.value.book.image != null)
const deleteImage: Ref<boolean> = ref(false)

const progress: Ref<boolean> = ref(false)

const publishedDate = ref(userbook.value.book.publishedDate ? new Date(userbook.value.book.publishedDate) : null)

function copyInput(book: UserBook | null): any {
  if (book == null) {
    return {}
  }
  const b = ObjectUtils.deepCopy(book)
  console.log("copy")
  console.log(b)
  return b
}

const handleFileUpload = (event: any) => {
  file.value = event.target.files[0];
};

const imageUrl = ref<string | null>(null);
const imagePath = ref<string | null>(null);
const uploadType = ref('web');

const clearImageField = () => {
  imageUrl.value = "";
};

const showImagePickerModal: Ref<boolean> = ref(false)

const file = ref(null);

const uploadPercentage = ref(0);
const errorMessage = ref("");
const ownedDisplay = computed(() => {
  if (userbook.value.owned) {
    return t('book.owned')
  }
  return ""
})
const borrowedDisplay = computed(() => {
  if (userbook.value.borrowed) {
    return t('book.borrowed')
  }
  return ""
})
const toReadDisplay = computed(() => {
  if (userbook.value.toRead) {
    return t('labels.book_will_be_added')
  }
  return ""
})

const seriesCopy: Array<SeriesOrder> = userbook.value.book.series ?? []

const importBook = () => {
  userbook.value.book.series = seriesCopy.filter(s => s.name != null && s.name.trim().length > 0)
  console.log("import")
  console.log(userbook)
  if (!props.canAddEvent || userbook.value.lastReadingEvent === ReadingEventType.NONE) {
    userbook.value.lastReadingEvent = null
  }
  if (StringUtils.isNotBlank(imageUrl.value)) {
    userbook.value.book.image = imageUrl.value
  } else if (imagePath.value != null && StringUtils.isNotBlank(imagePath.value)) {
      userbook.value.book.image = imagePath.value
  } else if (deleteImage.value) {
    userbook.value.book.image = null
  }

  console.log(`push book ` + userbook.value);
  console.log(userbook.value);
  let promise: Promise<UserBook>
  progress.value = true
  // no id on userbook -> we have a book and save the userbook
  if (StringUtils.isBlank(userbook.value.id)) {
    promise = dataService.saveUserBookImage(
      userbook.value,
      file.value,
      (event: { loaded: number; total: number }) => {
        const percent = Math.round((100 * event.loaded) / event.total);
        console.log("percent " + percent);
        uploadPercentage.value = percent;
      }
    )
  }
  // just update the existing userbook
  else {
    promise = dataService.updateUserBookImage(
      userbook.value,
      file.value,
      (event: { loaded: number; total: number }) => {
        const percent = Math.round((100 * event.loaded) / event.total);
        console.log("percent " + percent);
        uploadPercentage.value = percent;
      }
    )
  }
  promise
    .then(res => {
      console.log(`update book ${res.book.title}`);
      progress.value = false
      ObjectUtils.toast(oruga, "success", t('labels.book_title_updated', { title : res.book.title}), 4000);
      emit('close')
    })
    .catch(err => {
      progress.value = false
      ObjectUtils.toast(oruga, "danger", t('labels.error_message', {msg : err.message}), 4000);
    })

}

function getFilteredData(text: string, target: Array<Wrapper>) {
  dataService.findAuthorByCriteria(text).then((data) => {
    target.splice(0, target.length)
    data.content.forEach(a => target.push(ObjectUtils.wrapForOptions(a)))
  })
}

function getFilteredTags(text: string) {
  dataService.findTagsByCriteria(text).then((data) => {
    filteredTags.value.splice(0, filteredTags.value.length)
    data.content.forEach(t => filteredTags.value.push(ObjectUtils.wrapForOptions(t)))
  })
}

function getFilteredPublishers(text: string) {
  userbook.value.book.publisher = text
  dataService.findPublisherByCriteria(text).then(data => filteredPublishers.value = data.content)
}

function beforeAdd(item: Author | string, target: Array<Author>) {
  let shouldAdd = true
  if (item instanceof Object) {
    target.forEach(author => {
      console.log(`author ${author.name}`)
      if (author.name === item.name) {
        console.log(`author ${author.name} item ${item.name}`)
        shouldAdd = false;
      }
    });
  }
  else {
    target.forEach(author => {
      console.log(`author ${author.name}`)
      if (author.name === item) {
        console.log(`author ${author.name} item ${item}`)
        shouldAdd = false;
      }
    });
  }
  return shouldAdd
}

function beforeAddTag(item: Tag | string) {
  let shouldAdd = true
  if (item instanceof Object) {
    userbook.value.book?.tags?.forEach(tag => {
      console.log(`tag ${tag.name}`)
      if (tag.name === item.name) {
        console.log(`tag ${tag.name} item ${item.name}`)
        shouldAdd = false;
      }
    });
  }
  else {
    userbook.value.book?.tags?.forEach(tag => {
      console.log(`tag ${tag.name}`)
      if (tag.name === item) {
        console.log(`tag ${tag.name} item ${item}`)
        shouldAdd = false;
      }
    });
  }
  return shouldAdd
}

function selectPublisher(publisher: string) {
  // we receive from oruga weird events while nothing is selected
  // so try to get rid of those null data we receive
  if (publisher != null) {
    userbook.value.book.publisher = publisher
  }
}

const toggleImagePickerModal = () => {
  showImagePickerModal.value = !showImagePickerModal.value
  oruga.modal.open({
    parent: this,
    component: ImagePickerModal,
    trapFocus: true,
    active: true,
    canCancel: ['x', 'button', 'outside'],
    scroll: 'keep',
    events: {
      choose: (path: Path) => {
        console.log("received path")
        console.log(path)
        imagePath.value = path.path
      }
    },
    onClose: modalClosed
  });
}

function modalClosed() {
  console.log("modal closed")
}

function toggleRemoveImage() {
  deleteImage.value = !deleteImage.value
}

watch(() => [userbook.value.currentPageNumber, userbook.value.percentRead, userbook.value.book.pageCount],(newVal, oldVal) => {
  if (userbook.value.book.pageCount != null) {
    ObjectUtils.computePages(newVal, oldVal, userbook.value, userbook.value.book.pageCount)
  }
})

watch(() => publishedDate.value, (newVal, oldVal) => {
    if (newVal == null) {
        userbook.value.book.publishedDate = null
    } else {
        const formatted = dayjs(newVal).format('YYYY-MM-DD')
        userbook.value.book.publishedDate = formatted
    }
})

if (userbook.value.book.publisher != null) {
  filteredPublishers.value.push(userbook.value.book.publisher as string) // prefill editor autocomplete. oruga workaround
}
</script>

<template>
  <section class="edit-modal">
    <div class="">
      <div class="">
        <FormField
          v-model="userbook.book.title"
          :legend="t('book.title')"
          placeholder=""
        />
        <fieldset class="fieldset jelu-authorinput">
          <legend class="fieldset-legend capitalize">
            {{ t('book.author', 2) }}
          </legend>
          <o-taginput
            v-model="userbook.book.authors"
            :options="filteredAuthors"
            :allow-autocomplete="true"
            autocomplete="off"
            :allow-new="true"
            :allow-duplicates="false"
            :open-on-focus="true"
            :validate-item="(item: Author|string) => beforeAdd(item, userbook.book.authors as Array<Author>)"
            :create-item="ObjectUtils.createNamedItem"
            icon-pack="mdi"
            icon="account-plus"
            :placeholder="t('labels.add_author')"
            @input="(v: string) => getFilteredData(v, filteredAuthors)"
          >
            <template #default="{ value }">
              <div class="jl-taginput-item">
                {{ value.name }}
              </div>
            </template>
            <template #selected="{ removeItem, items }">
              <ClosableBadge
                v-for="(item, index) in items"
                :key="item.name"
                :content="item.name"
                class="badge-primary"
                @closed="removeItem(index, $event)"
              />
            </template>
          </o-taginput>
        </fieldset>
        <fieldset class="fieldset jelu-taginput">
          <legend class="fieldset-legend capitalize">
            {{ t('book.tag', 2) }}
          </legend>
          <o-taginput
            v-model="userbook.book.tags"
            :options="filteredTags"
            :allow-autocomplete="true"
            autocomplete="off"
            :allow-new="true"
            :allow-duplicates="false"
            :open-on-focus="true"
            :validate-item="beforeAddTag"
            :create-item="ObjectUtils.createNamedItem"
            icon-pack="mdi"
            icon="tag-plus"
            :placeholder="t('labels.add_tag')"
            @input="getFilteredTags"
          >
            <template #default="{ value }">
              <div class="jl-taginput-item">
                {{ value.name }}
              </div>
            </template>
            <template #selected="{ removeItem, items }">
              <ClosableBadge
                v-for="(item, index) in items"
                :key="item.name"
                :content="item.name"
                class="badge-secondary"
                @closed="removeItem(index, $event)"
              />
            </template>
          </o-taginput>
        </fieldset>
        <fieldset class="fieldset jelu-authorinput">
          <legend class="fieldset-legend capitalize">
            {{ t('book.translator', 2) }}
          </legend>
          <o-taginput
            v-model="userbook.book.translators"
            :options="filteredTranslators"
            :allow-autocomplete="true"
            autocomplete="off"
            :allow-new="true"
            :allow-duplicates="false"
            :open-on-focus="true"
            :validate-item="(item: Author|string) => beforeAdd(item, userbook.book.translators as Array<Author>)"
            :create-item="ObjectUtils.createNamedItem"
            icon-pack="mdi"
            icon="account-plus"
            :placeholder="t('labels.add_translator')"
            @input="(v: string) => getFilteredData(v, filteredTranslators)"
          >
            <template #default="{ value }">
              <div class="jl-taginput-item">
                {{ value.name }}
              </div>
            </template>
            <template #selected="{ removeItem, items }">
              <ClosableBadge
                v-for="(item, index) in items"
                :key="item.name"
                :content="item.name"
                class="badge-primary"
                @closed="removeItem(index, $event)"
              />
            </template>
          </o-taginput>
        </fieldset>
        <fieldset class="fieldset jelu-authorinput">
          <legend class="fieldset-legend capitalize">
            {{ t('book.narrator', 2) }}
          </legend>
          <o-taginput
            v-model="userbook.book.narrators"
            :options="filteredNarrators"
            :allow-autocomplete="true"
            autocomplete="off"
            :allow-new="true"
            :allow-duplicates="false"
            :open-on-focus="true"
            :validate-item="(item: Author|string) => beforeAdd(item, userbook.book.narrators as Array<Author>)"
            :create-item="ObjectUtils.createNamedItem"
            icon-pack="mdi"
            icon="account-plus"
            :placeholder="t('labels.add_narrator')"
            @input="(v: string) => getFilteredData(v, filteredNarrators)"
          >
            <template #default="{ value }">
              <div class="jl-taginput-item">
                {{ value.name }}
              </div>
            </template>
            <template #selected="{ removeItem, items }">
              <ClosableBadge
                v-for="(item, index) in items"
                :key="item.name"
                :content="item.name"
                class="badge-primary"
                @closed="removeItem(index, $event)"
              />
            </template>
          </o-taginput>
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('book.summary') }}
          </legend>
          <textarea
            v-model="userbook.book.summary"
            maxlength="50000"
            class="textarea focus:textarea-accent w-full"
          />
        </fieldset>
        <fieldset class="fieldset grid grid-cols-2">
          <legend class="fieldset-legend capitalize">
            {{ t('book.isbn') }}
          </legend>
          <input
            v-model="userbook.book.isbn10"
            type="text"
            name="isbn10"
            :placeholder="t('book.isbn10')"
            class="input focus:input-accent w-full"
          >
          <input
            v-model="userbook.book.isbn13"
            type="text"
            name="isbn13"
            :placeholder="t('book.isbn13')"
            class="input focus:input-accent w-full"
          >
        </fieldset>
        <fieldset class="fieldset sm:grid sm:grid-cols-3">
          <legend class="fieldset-legend capitalize providers-ids">
            {{ t('book.identifiers') }}
          </legend>
          <input
            v-model="userbook.book.goodreadsId"
            type="text"
            name="goodreadsId"
            :placeholder="t('book.goodreads_id')"
            class="input focus:input-accent w-full"
          >
          <input
            v-model="userbook.book.googleId"
            type="text"
            name="googleId"
            :placeholder="t('book.google_id')"
            class="input focus:input-accent w-full"
          >
          <input
            v-model="userbook.book.amazonId"
            type="text"
            name="amazonId"
            :placeholder="t('book.amazon_id')"
            class="input focus:input-accent w-full"
          >
          <input
            v-model="userbook.book.librarythingId"
            type="text"
            name="librarythingId"
            :placeholder="t('book.librarything_id')"
            class="input focus:input-accent w-full"
          >
          <input
            v-model="userbook.book.isfdbId"
            type="text"
            name="isfdbId"
            :placeholder="t('book.isfdb_id')"
            class="input focus:input-accent w-full"
          >
          <input
            v-model="userbook.book.openlibraryId"
            type="text"
            name="openlibraryId"
            :placeholder="t('book.openlibrary_id')"
            class="input focus:input-accent w-full"
          >
          <input
            v-model="userbook.book.noosfereId"
            type="text"
            name="noosfereId"
            :placeholder="t('book.noosfere_id')"
            class="input focus:input-accent w-full"
          >
          <input
            v-model="userbook.book.inventaireId"
            type="text"
            name="inventaireId"
            :placeholder="t('book.inventaire_id')"
            class="input focus:input-accent w-full"
          >
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('book.publisher') }}
          </legend>
          <o-autocomplete
            v-model="userbook.book.publisher"
            :root-class="'grow, w-full'"
            :input-classes="{rootClass:'w-full border-2 border-accent', inputClass:'w-full'}"
            :options="filteredPublishers"
            :clear-on-select="false"
            :debounce="100"
            @input="getFilteredPublishers"
            @select="selectPublisher"
          >
            <template #default="{ value }">
              <div class="jl-taginput-item">
                {{ value }}
              </div>
            </template>
          </o-autocomplete>
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('book.published_date') }}
          </legend>
          <!-- eslint-disable -->
            <datepicker v-model="publishedDate as Date"
              class="input input-primary w-11/12"
              :typeable="true"
              :clearable="true"
            >
            <!-- eslint-enable -->
            <template #clear="{ onClear }">
              <button @click="onClear">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke-width="1.5"
                  stroke="currentColor"
                  class="w-6 h-6"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M12 9.75L14.25 12m0 0l2.25 2.25M14.25 12l2.25-2.25M14.25 12L12 14.25m-2.58 4.92l-6.375-6.375a1.125 1.125 0 010-1.59L9.42 4.83c.211-.211.498-.33.796-.33H19.5a2.25 2.25 0 012.25 2.25v10.5a2.25 2.25 0 01-2.25 2.25h-9.284c-.298 0-.585-.119-.796-.33z"
                  />
                </svg>
              </button>
            </template>
          </datepicker>
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('book.page_count') }}
          </legend>
          <label class="input w-full">
            <input
              v-model="userbook.book.pageCount"
              type="number"
              class="input focus:input-accent"
              min="0"
            >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
              class="size-6 hover:cursor-pointer"
              @click="userbook.book.pageCount = null; userbook.currentPageNumber = null"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="m9.75 9.75 4.5 4.5m0-4.5-4.5 4.5M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
              />
            </svg>
          </label>
        </fieldset>
        <FormField
          v-model="userbook.book.language"
          :legend="t('book.language')"
          placeholder=""
        />
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('book.series') }}
          </legend>
          <div class="flex flex-col grow w-full">
            <SeriesCompleteInput v-model="seriesCopy" />
          </div>
        </fieldset>
        <fieldset
          v-if="props.canAddEvent"
          class="block fieldset"
        >
          <legend class="fieldset-legend capitalize">
            {{ t('book.status') }}&nbsp;:
          </legend>
          <div class="">
            <label class="label cursor-pointer justify-center gap-2 flex flex-wrap">
              <div>
                <input
                  v-model="userbook.lastReadingEvent"
                  type="radio"
                  name="radio-10"
                  class="radio radio-primary mx-3"
                  value="FINISHED"
                >
                <span class="label-text">{{ t('reading_events.finished') }}</span>
              </div>
              <div>
                <input
                  v-model="userbook.lastReadingEvent"
                  type="radio"
                  name="radio-10"
                  class="radio radio-primary mx-3"
                  value="CURRENTLY_READING"
                >
                <span class="label-text">{{ t('reading_events.currently_reading') }}</span>
              </div>
              <div>
                <input
                  v-model="userbook.lastReadingEvent"
                  type="radio"
                  name="radio-10"
                  class="radio radio-primary mx-3"
                  value="DROPPED"
                >
                <span class="label-text">{{ t('reading_events.dropped') }}</span>
              </div>
              <div>
                <input
                  v-model="userbook.lastReadingEvent"
                  type="radio"
                  name="radio-10"
                  class="radio radio-primary mx-3"
                  value="NONE"
                >
                <span class="label-text">{{ t('reading_events.none') }}</span>
              </div>
            </label>
          </div>
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('book.personal_notes') }}
          </legend>
          <textarea
            v-model="userbook.personalNotes"
            maxlength="5000"
            type="textarea"
            class="textarea focus:textarea-accent w-full"
          />
        </fieldset>
        <div class="grid grid-cols-3">
          <fieldset class="fieldset">
            <legend class="fieldset-legend capitalize">
              {{ t('book.owned') }}
            </legend>
            <label class="label">
              <input
                v-model="userbook.owned"
                type="checkbox"
                class="checkbox checkbox-primary"
              ></input>
              {{ ownedDisplay }}
            </label>
          </fieldset>
          <fieldset class="fieldset">
            <legend class="fieldset-legend capitalize">
              {{ t('book.to_read') }}&nbsp;?
            </legend>
            <label class="label">
              <input
                v-model="userbook.toRead"
                type="checkbox"
                class="checkbox checkbox-primary"
              ></input>
              {{ toReadDisplay }}
            </label>
          </fieldset>
          <fieldset class="fieldset">
            <legend class="fieldset-legend capitalize">
              {{ t('book.borrowed') }}&nbsp;?
            </legend>
            <label class="label">
              <input
                v-model="userbook.borrowed"
                type="checkbox"
                class="checkbox checkbox-primary"
              ></input>
              {{ borrowedDisplay }}
            </label>
          </fieldset>
        </div>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('book.current_page_number') }}
          </legend>
          <label class="input w-full">
            <input
              v-model="userbook.currentPageNumber"
              type="number"
              class="input focus:input-accent"
              min="0"
              :disabled="userbook.book.pageCount == null"
              :max="userbook.book.pageCount"
            >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
              class="size-6 hover:cursor-pointer"
              @click="userbook.currentPageNumber = null"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="m9.75 9.75 4.5 4.5m0-4.5-4.5 4.5M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
              />
            </svg>
          </label>
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('book.percent_read') }}
          </legend>
          <input
            v-model="userbook.percentRead"
            type="range"
            min="0"
            max="100"
            class="w-full range range-primary range-xs"
            :disabled="userbook.book.pageCount != null"
          >
        </fieldset>
        <fieldset v-if="hasImage">
          <legend class="fieldset-legend capitalize">
            {{ t('labels.actual_cover') }}
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
              multiline
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
                :src="'/files/' + userbook.book.image"
                :class="deleteImage ? 'altered' : ''"
                alt="cover image"
              >
            </figure>
          </div>
        </fieldset>
        <div
          v-if="!hasImage || deleteImage"
          class="pt-1"
        >
          <fieldset
            class="fieldset"
          >
            <legend class="fieldset-legend capitalize">
              {{ t('labels.upload_cover') }}
            </legend>
            <div class="">
              <label class="label cursor-pointer justify-center gap-2 flex flex-wrap">
                <span class="label-text">{{ t('labels.upload_from_web') }}</span>
                <input
                  v-model="uploadType"
                  type="radio"
                  name="radio-11"
                  class="radio radio-primary"
                  value="web"
                >
                <span class="label-text">{{ t('labels.upload_from_computer') }}</span>
                <input
                  v-model="uploadType"
                  type="radio"
                  name="radio-11"
                  class="radio radio-primary"
                  value="computer"
                >
                <span class="label-text">{{ t('labels.upload_from_server') }}</span>
                <input
                  v-model="uploadType"
                  type="radio"
                  name="radio-11"
                  class="radio radio-primary"
                  value="server"
                >
              </label>
            </div>
          </fieldset>
          <fieldset
            v-if="uploadType == 'web'"
            class="fieldset"
          >
            <legend
              class="fieldset-legend capitalize"
            >
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
          </fieldset>
          <fieldset
            v-else-if="uploadType == 'computer'"
            class="fieldset"
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
          <fieldset
            v-else
            class="fieldset"
          >
            <legend
              class="file fieldset-legend"
            >
              {{ t('labels.choose_file') }}
            </legend>
            <button
              class="btn btn-primary button uppercase"
              @click="toggleImagePickerModal()"
            >
              <span class="icon">
                <i class="mdi mdi-file-question mdi-18px" />
              </span>
              <span>{{ t('labels.choose_file') }}</span>
            </button>
            <span>{{ imagePath }}</span>
          </fieldset>
        </div>
      </div>
      <div class="flex flex-row justify-center pt-6">
        <button
          class="btn btn-primary uppercase"
          :class="{'btn-disabled' : progress}"
          @click="importBook"
        >
          <span
            v-if="progress"
            class="loading loading-spinner"
          />
          {{ t('labels.save_changes') }}
        </button>
        <p
          v-if="errorMessage"
          class="text-error"
        >
          {{ errorMessage }}
        </p>
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
