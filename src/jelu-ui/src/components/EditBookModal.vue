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

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const props = defineProps<{ bookId: string, book: UserBook | null, canAddEvent: boolean }>()
const oruga = useOruga()
const emit = defineEmits(['close']);

let filteredAuthors: Ref<Array<Wrapper>> = ref([]);
let filteredTags: Ref<Array<Wrapper>> = ref([]);
let filteredTranslators: Ref<Array<Wrapper>> = ref([]);
let filteredNarrators: Ref<Array<Wrapper>> = ref([]);
let filteredPublishers: Ref<Array<string>> = ref([])
let userbook: Ref<UserBook> = ref(copyInput(props.book))
let hasImage: Ref<boolean> = ref(userbook.value.book.image != null)
let deleteImage: Ref<boolean> = ref(false)

const progress: Ref<boolean> = ref(false)

const publishedDate = ref(userbook.value.book.publishedDate ? new Date(userbook.value.book.publishedDate) : null)

function copyInput(book: UserBook | null): any {
  if (book == null) {
    return {}
  }
  let b = ObjectUtils.deepCopy(book)
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
        let percent = Math.round((100 * event.loaded) / event.total);
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
        let percent = Math.round((100 * event.loaded) / event.total);
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
        let formatted = dayjs(newVal).format('YYYY-MM-DD')
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
        <div class="field pb-2">
          <o-field
            horizontal
            :label="t('book.title')"
            class="capitalize"
          >
            <o-input
              v-model="userbook.book.title"
              expanded
              class="input focus:input-accent w-full"
            />
          </o-field>
        </div>

        <div class="field jelu-authorinput pb-2">
          <o-field
            horizontal
            :label="t('book.author', 2)"
            class="capitalize"
          >
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
                <div
                  v-for="(item, index) in items"
                  :key="item.name"
                  class="badge badge-primary badge-xl m-0.5"
                >
                  {{ item.name }}
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke-width="1.5"
                    stroke="currentColor"
                    class="size-6 hover:cursor-pointer"
                    @click="removeItem(index, $event)"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="m9.75 9.75 4.5 4.5m0-4.5-4.5 4.5M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
                    />
                  </svg>
                </div>
              </template>
            </o-taginput>
          </o-field>
        </div>
        <div class="field jelu-taginput pb-2">
          <o-field
            horizontal
            :label="t('book.tag', 2)"
            class="capitalize"
          >
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
                <div
                  v-for="(item, index) in items"
                  :key="item.name"
                  class="badge badge-secondary badge-xl m-0.5"
                >
                  {{ item.name }}
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke-width="1.5"
                    stroke="currentColor"
                    class="size-6 hover:cursor-pointer"
                    @click="removeItem(index, $event)"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="m9.75 9.75 4.5 4.5m0-4.5-4.5 4.5M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
                    />
                  </svg>
                </div>
              </template>
            </o-taginput>
          </o-field>
        </div>
        <div class="field jelu-authorinput pb-2">
          <o-field
            horizontal
            :label="t('book.translator', 2)"
            class="capitalize"
          >
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
                <div
                  v-for="(item, index) in items"
                  :key="item.name"
                  class="badge badge-primary badge-xl m-0.5"
                >
                  {{ item.name }}
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke-width="1.5"
                    stroke="currentColor"
                    class="size-6 hover:cursor-pointer"
                    @click="removeItem(index, $event)"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="m9.75 9.75 4.5 4.5m0-4.5-4.5 4.5M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
                    />
                  </svg>
                </div>
              </template>
            </o-taginput>
          </o-field>
        </div>
        <div class="field jelu-authorinput pb-2">
          <o-field
            horizontal
            :label="t('book.narrator', 2)"
            class="capitalize"
          >
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
                <div
                  v-for="(item, index) in items"
                  :key="item.name"
                  class="badge badge-primary badge-xl m-0.5"
                >
                  {{ item.name }}
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke-width="1.5"
                    stroke="currentColor"
                    class="size-6 hover:cursor-pointer"
                    @click="removeItem(index, $event)"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="m9.75 9.75 4.5 4.5m0-4.5-4.5 4.5M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
                    />
                  </svg>
                </div>
              </template>
            </o-taginput>
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            horizontal
            :label="t('book.summary')"
            class="capitalize"
          >
            <o-input
              v-model="userbook.book.summary"
              maxlength="50000"
              type="textarea"
              expanded
              class="textarea focus:textarea-accent"
            />
          </o-field>
        </div>
        <div class="field">
          <o-field
            horizontal
            :label="t('book.isbn')"
            class="uppercase"
          >
            <o-input
              v-model="userbook.book.isbn10"
              name="isbn10"
              expanded
              :placeholder="t('book.isbn10')"
              class="input focus:input-accent w-full"
            />
            <o-input
              v-model="userbook.book.isbn13"
              name="isbn13"
              expanded
              :placeholder="t('book.isbn13')"
              class="input focus:input-accent w-full"
            />
          </o-field>
        </div>
        <div class="field">
          <o-field
            horizontal
            :label="t('book.identifiers')"
            class="capitalize providers-ids"
          >
            <o-input
              v-model="userbook.book.goodreadsId"
              name="goodreadsId"
              :placeholder="t('book.goodreads_id')"
              class="input focus:input-accent w-full"
            />
            <o-input
              v-model="userbook.book.googleId"
              name="googleId"
              :placeholder="t('book.google_id')"
              class="input focus:input-accent w-full"
            />
            <o-input
              v-model="userbook.book.amazonId"
              name="amazonId"
              :placeholder="t('book.amazon_id')"
              class="input focus:input-accent w-full"
            />
            <o-input
              v-model="userbook.book.librarythingId"
              name="librarythingId"
              :placeholder="t('book.librarything_id')"
              class="input focus:input-accent w-full"
            />
            <o-input
              v-model="userbook.book.isfdbId"
              name="isfdbId"
              :placeholder="t('book.isfdb_id')"
              class="input focus:input-accent w-full"
            />
            <o-input
              v-model="userbook.book.openlibraryId"
              name="openlibraryId"
              :placeholder="t('book.openlibrary_id')"
              class="input focus:input-accent w-full"
            />
            <o-input
              v-model="userbook.book.noosfereId"
              name="noosfereId"
              :placeholder="t('book.noosfere_id')"
              class="input focus:input-accent w-full"
            />
            <o-input
              v-model="userbook.book.inventaireId"
              name="inventaireId"
              :placeholder="t('book.inventaire_id')"
              class="input focus:input-accent w-full"
            />
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            horizontal
            :label="t('book.publisher')"
            class="capitalize"
          >
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
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            horizontal
            :label="t('book.published_date')"
            class="capitalize"
          >
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
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            horizontal
            :label="t('book.page_count')"
            class="capitalize"
          >
            <o-input
              v-model="userbook.book.pageCount"
              type="number"
              number
              min="0"
              expanded
              icon-right="delete"
              icon-right-clickable
              class="input focus:input-accent sm:w-11/12"
              @icon-right-click="userbook.book.pageCount = null;userbook.currentPageNumber=null"
            />
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            horizontal
            :label="t('book.language')"
            class="capitalize"
          >
            <o-input
              v-model="userbook.book.language"
              type="text"
              expanded
              class="input focus:input-accent w-full"
            />
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            :label="t('book.series')"
            horizontal
            class="capitalize"
          >
            <div class="flex flex-col grow w-full">
              <div>
                <SeriesCompleteInput v-model="seriesCopy" />
              </div>
            </div>
          </o-field>
        </div>
        <div
          v-if="props.canAddEvent"
          class="block"
        >
          <o-field
            horizontal
            :label="t('book.status') + ' : '"
            class="capitalize"
          >
            <o-radio
              v-model="userbook.lastReadingEvent"
              name="type"
              native-value="FINISHED"
            >
              {{ t('reading_events.finished') }}
            </o-radio>
            <o-radio
              v-model="userbook.lastReadingEvent"
              name="type"
              native-value="CURRENTLY_READING"
            >
              {{ t('reading_events.currently_reading') }}
            </o-radio>
            <o-radio
              v-model="userbook.lastReadingEvent"
              name="type"
              native-value="DROPPED"
            >
              {{ t('reading_events.dropped') }}
            </o-radio>
            <o-radio
              v-model="userbook.lastReadingEvent"
              name="type"
              native-value="NONE"
            >
              {{ t('reading_events.none') }}
            </o-radio>
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            horizontal
            :label="t('book.personal_notes')"
            class="capitalize"
          >
            <o-input
              v-model="userbook.personalNotes"
              maxlength="5000"
              type="textarea"
              expanded
              class="textarea focus:textarea-accent"
            />
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            horizontal
            :label="t('book.owned')"
            class="capitalize"
          >
            <o-checkbox v-model="userbook.owned">
              {{ ownedDisplay }}
            </o-checkbox>
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            horizontal
            :label="t('book.to_read') + ' ?'"
            class="capitalize"
          >
            <o-checkbox v-model="userbook.toRead">
              {{ toReadDisplay }}
            </o-checkbox>
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            horizontal
            :label="t('book.borrowed') + ' ?'"
            class="capitalize"
          >
            <o-checkbox v-model="userbook.borrowed">
              {{ borrowedDisplay }}
            </o-checkbox>
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            horizontal
            :label="t('book.current_page_number')"
            class="capitalize"
          >
            <o-input
              v-model="userbook.currentPageNumber"
              type="number"
              number
              expanded
              min="0"
              :max="userbook.book.pageCount"
              :disabled="userbook.book.pageCount == null"
              class="input focus:input-accent w-11/12"
              icon-right="delete"
              icon-right-clickable
              @icon-right-click="userbook.currentPageNumber = null"
            />
          </o-field>
        </div>
        <div class="field pb-2">
          <o-field
            horizontal
            :label="t('book.percent_read')"
            class="capitalize"
          >
            <o-slider
              v-model="userbook.percentRead"
              :min="0"
              :max="100"
              :disabled="userbook.book.pageCount != null"
            />
          </o-field>
        </div>
        <div v-if="hasImage">
          <o-field
            horizontal
            class=" pb-6"
          >
            <template #label>
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
                multiline
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
                  :src="'/files/' + userbook.book.image"
                  :class="deleteImage ? 'altered' : ''"
                  alt="cover image"
                >
              </figure>
            </div>
          </o-field>
        </div>
        <div
          v-if="!hasImage || deleteImage"
          class=" pt-2"
        >
          <o-field
            horizontal
            :label="t('labels.upload_cover')"
          >
            <div class="">
              <label class="label cursor-pointer justify-center gap-2 flex flex-wrap">
                <span class="label-text">{{ t('labels.upload_from_web') }}</span>
                <input
                  v-model="uploadType"
                  type="radio"
                  name="radio-10"
                  class="radio radio-primary"
                  value="web"
                >
                <span class="label-text">{{ t('labels.upload_from_computer') }}</span>
                <input
                  v-model="uploadType"
                  type="radio"
                  name="radio-10"
                  class="radio radio-primary"
                  value="computer"
                >
                <span class="label-text">{{ t('labels.upload_from_server') }}</span>
                <input
                  v-model="uploadType"
                  type="radio"
                  name="radio-10"
                  class="radio radio-primary"
                  value="server"
                >
              </label>
            </div>
          </o-field>
          <o-field
            v-if="uploadType == 'web'"
            horizontal
            :label="t('labels.enter_image_address')"
            class="pb-2"
          >
            <o-input
              v-model="imageUrl"
              type="url"
              pattern="https?://.*"
              clearable="true"
              expanded
              icon-right-clickable
              title="Url must start with http or https"
              :placeholder="t('labels.url_must_start')"
              class="input focus:input-accent w-full"
              @icon-right-click="clearImageField"
            />
          </o-field>
          <o-field
            v-else-if="uploadType == 'computer'"
            horizontal
            :label="t('labels.choose_file')"
            class="file"
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
          <o-field
            v-else
            horizontal
            :label="t('labels.choose_file')"
            class="file"
          >
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
          </o-field>
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
