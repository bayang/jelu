<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next";
import IsbnVerify from '@saekitominaga/isbn-verify';
import { useTitle } from '@vueuse/core';
import { computed, reactive, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { Author } from "../model/Author";
import { UserBook } from "../model/Book";
import { Metadata } from "../model/Metadata";
import { Tag } from "../model/Tag";
import dataService from "../services/DataService";
import { key } from '../store';
import { ObjectUtils } from "../utils/ObjectUtils";
import { StringUtils } from "../utils/StringUtils";
import AutoImportFormModalVue from "./AutoImportFormModal.vue";
import AutoImportFileModalVue from "./AutoImportFileModal.vue";
import { SeriesOrder } from "../model/Series";
import SeriesInput from "./SeriesInput.vue";
import ImagePickerModal from "./ImagePickerModal.vue";
import { Path } from "../model/DirectoryListing";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

useTitle('Jelu | Add book')

const store = useStore(key)
const router = useRouter()
const oruga = useOruga()

const datepicker = ref(null);
const publishedDate: Ref<Date | null> = ref(null)
const progress: Ref<boolean> = ref(false)
const form = reactive({
  title: "",
  summary: "",
  isbn10: "",
  isbn13: "",
  publisher: "",
  pageCount: null,
  personalNotes: "",
  owned: null,
  borrowed: null,
  toRead: null,
  percentRead: null,
  currentPageNumber: null,
  googleId: "",
  amazonId: "",
  goodreadsId: "",
  librarythingId: "",
  language: ""
});
const eventType = ref(null);
const eventDate = ref(new Date());
const imageUrl = ref<string | null>(null);
const imagePath = ref<string | null>(null);
const file = ref(null);
const uploadType = ref('web');

const uploadPercentage = ref(0);
const errorMessage = ref("");
const ownedDisplay = computed(() => {
  if (form.owned) {
    return t('book.owned')
  }
  return ""
})
const borrowedDisplay = computed(() => {
  if (form.borrowed) {
    return t('book.borrowed')
  }
  return ""
})
const toReadDisplay = computed(() => {
  if (form.toRead) {
    return t('labels.book_will_be_added')
  }
  return ""
})

watch(() => [form.currentPageNumber, form.percentRead, form.pageCount],(newVal, oldVal) => {
  if (form.pageCount != null) {
    ObjectUtils.computePages(newVal, oldVal, form, form.pageCount)
  }
})

let filteredAuthors: Ref<Array<Author>> = ref([]);
let authors: Ref<Array<Author>> = ref([]);

let filteredTags: Ref<Array<Tag>> = ref([]);
let tags: Ref<Array<Tag>> = ref([]);

let translators: Ref<Array<Author>> = ref([]);
let filteredTranslators: Ref<Array<Author>> = ref([]);

let seriesCopy: Ref<Array<SeriesOrder>> = ref([])

const showModal: Ref<boolean> = ref(false)
const metadata: Ref<Metadata | null> = ref(null)

const showImagePickerModal: Ref<boolean> = ref(false)

let hasImage = computed(() => {
  return StringUtils.isNotBlank(metadata.value?.image)
})
let deleteImage: Ref<boolean> = ref(false)

function toggleRemoveImage() {
  deleteImage.value = !deleteImage.value
}

const importBook = async () => {
  console.log("import book");
  if (StringUtils.isNotBlank(form.title)) {
    let alreadyExisting = await dataService.checkIsbnExists(form.isbn10, form.isbn13)
    console.log('already existing')
    console.log(alreadyExisting)
    let saveBook = true
    if (alreadyExisting != null) {
      saveBook = false
      await ObjectUtils.swalMixin.fire({
        html: `<p>${t('labels.book_with_same_isbn_already_exists')}:<br>${alreadyExisting.title}<br>${t('labels.save_new_anyway')}</p>`,
        showDenyButton: true,
        confirmButtonText: t('labels.save'),
        denyButtonText: t('labels.dont_save'),
      }).then((result) => {
        if (result.isConfirmed) {
          saveBook = true
        } else if (result.isDenied) {
          ObjectUtils.baseSwalMixin.fire('', t('labels.changes_not_saved'), 'info')
        }
      })
    }
    console.log(`save book ${saveBook}`)
    if (!saveBook) {
      return
    }
    let userBook: UserBook = fillBook(form, publishedDate.value)
    authors.value.forEach((a) => userBook.book.authors?.push(a));
    tags.value.forEach((t) => userBook.book.tags?.push(t));
    translators.value.forEach((tr) => userBook.book.translators?.push(tr));
    seriesCopy.value.forEach((s) => {
      if (s.name.trim().length > 0) {
        userBook.book.series?.push(s)
      }
    })
    if (StringUtils.isNotBlank(imageUrl.value)) {
      userBook.book.image = imageUrl.value;
    }
    else if (imagePath.value != null && StringUtils.isNotBlank(imagePath.value)) {
      userBook.book.image = imagePath.value
    }
    else if (!deleteImage.value
      && metadata.value != null
      && metadata.value?.image != null
      && StringUtils.isNotBlank(metadata.value.image)) {
      userBook.book.image = metadata.value.image
    }
    if (eventType.value !== null && eventType.value !== "NONE") {
      console.log(
        "type " + StringUtils.readingEventTypeForValue(eventType.value)
      );
      userBook.lastReadingEvent = StringUtils.readingEventTypeForValue(eventType.value);
      userBook.lastReadingEventDate = eventDate.value.toISOString()
    }
    try {
      console.log(`push book ` + userBook);
      console.log(userBook);
      progress.value = true
      let res: UserBook = await dataService.saveUserBookImage(
        userBook,
        file.value,
        (event: { loaded: number; total: number }) => {
          let percent = Math.round((100 * event.loaded) / event.total);
          console.log("percent " + percent);
          uploadPercentage.value = percent;
        }
      );
      progress.value = false
      console.log(`saved book ${res.book.title}`);
      ObjectUtils.toast(oruga, "success", t('labels.book_title_saved', {title : res.book.title}), 4000)
      clearForm();
      await router.push({name: 'my-books'})
    } catch (error: any) {
      progress.value = false
      ObjectUtils.toast(oruga, "danger", t('labels.error_message', {msg : error.message}), 4000)
    }
  } else {
    errorMessage.value = t('labels.provide_title');
  }
};

const fillBook = (formdata: any, publishedDate: Date | null): UserBook => {
  let userBook: UserBook = {
    book: {
      title: formdata.title,
      isbn10: formdata.isbn10,
      isbn13: formdata.isbn13,
      summary: formdata.summary,
      publisher: formdata.publisher,
      image: formdata.image,
      pageCount: formdata.pageCount,
      publishedDate: publishedDate?.toISOString(),
      series: [],
      googleId: formdata.googleId,
      amazonId: formdata.amazonId,
      goodreadsId: formdata.goodreadsId,
      librarythingId: formdata.librarythingId,
      language: formdata.language,
      authors: [],
      translators: [],
      tags: []
    },
    owned: formdata.owned,
    personalNotes: formdata.personalNotes,
    toRead: formdata.toRead,
    percentRead: formdata.percentRead,
    currentPageNumber: formdata.currentPageNumber,
    borrowed: formdata.borrowed
  }
  return userBook
}

const clearForm = () => {
  clearImageField();
  errorMessage.value = "";
  eventType.value = null;
  file.value = null;
  authors.value = [];
  tags.value = [];
  translators.value = [];
  uploadPercentage.value = 0;
  form.title = "";
  form.summary = "";
  form.isbn10 = "";
  form.isbn13 = "";
  form.publisher = "";
  form.pageCount = null;
  publishedDate.value = null
  seriesCopy.value = []
  form.language = ""
  form.owned = null
  form.personalNotes = ""
  form.amazonId = ""
  form.googleId = ""
  form.goodreadsId = ""
  form.librarythingId = ""
  form.percentRead = null
  form.currentPageNumber = null
};

const clearDatePicker = () => {
  // close datepicker on reset
  publishedDate.value = null
};

const handleFileUpload = (event: any) => {
  file.value = event.target.files[0];
};

const clearImageField = () => {
  imageUrl.value = "";
};

function getFilteredAuthors(text: string) {
  dataService.findAuthorByCriteria(text).then((data) => filteredAuthors.value = data.content)
}

function getFilteredTranslators(text: string) {
  dataService.findAuthorByCriteria(text).then((data) => filteredTranslators.value = data.content)
}

function getFilteredTags(text: string) {
  dataService.findTagsByCriteria(text).then((data) => filteredTags.value = data.content)
}

function beforeAdd(item: Author | string) {
  let shouldAdd = true
  if (item instanceof Object) {
    authors.value.forEach(author => {
      console.log(`author ${author.name}`)
      if (author.name === item.name) {
        console.log(`author ${author.name} item ${item.name}`)
        shouldAdd = false;
      }
    });
  }
  else {
    authors.value.forEach(author => {
      console.log(`author ${author.name}`)
      if (author.name === item) {
        console.log(`author ${author.name} item ${item}`)
        shouldAdd = false;
      }
    });
  }
  return shouldAdd
}

function beforeAddTranslator(item: Author | string) {
  let shouldAdd = true
  if (item instanceof Object) {
    translators.value.forEach(translator => {
      console.log(`translator ${translator.name}`)
      if (translator.name === item.name) {
        console.log(`translator ${translator.name} item ${item.name}`)
        shouldAdd = false;
      }
    });
  }
  else {
    translators.value.forEach(translator => {
      console.log(`translator ${translator.name}`)
      if (translator.name === item) {
        console.log(`translator ${translator.name} item ${item}`)
        shouldAdd = false;
      }
    });
  }
  return shouldAdd
}

function beforeAddTag(item: Tag | string) {
  let shouldAdd = true
  if (item instanceof Object) {
    tags.value.forEach(tag => {
      console.log(`tag ${tag.name}`)
      if (tag.name === item.name) {
        console.log(`tag ${tag.name} item ${item.name}`)
        shouldAdd = false;
      }
    });
  }
  else {
    tags.value.forEach(tag => {
      console.log(`tag ${tag.name}`)
      if (tag.name === item) {
        console.log(`tag ${tag.name} item ${item}`)
        shouldAdd = false;
      }
    });
  }
  return shouldAdd
}

function createAuthor(item: Author | string) {
  if (item instanceof Object) {
    return item
  }
  return {
    "name": item
  }
}

function createTag(item: Tag | string) {
  if (item instanceof Object) {
    return item
  }
  return {
    "name": item
  }
}

const toggleModal = (file: boolean) => {
  showModal.value = !showModal.value
  oruga.modal.open({
    parent: this,
    component: file ? AutoImportFileModalVue : AutoImportFormModalVue,
    trapFocus: true,
    active: true,
    canCancel: ['x', 'button', 'outside'],
    scroll: 'keep',
    props: {
        "book": null,
      },
    events: {
      metadataReceived: (modalMetadata: Metadata) => {
        console.log("received metadata")
        console.log(modalMetadata)
        metadata.value = modalMetadata
        mergeMetadata()
      }
    },
    onClose: modalClosed
  });
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

const mergeMetadata = () => {
  for (let key in metadata.value) {
    console.log("key")
    console.log(key)
    if (key in form) {
      let castKey = key as (keyof typeof metadata.value & keyof typeof form);
      (form[castKey] as any) = metadata.value[castKey];
    }
  }
  if (metadata.value?.authors != null && metadata.value.authors.length > 0) {
    let auths: Array<Author> = []
    metadata.value.authors.forEach(a => auths.push(createAuthor(a)))
    authors.value = auths
  }
  if (metadata.value?.tags != null && metadata.value.tags.length > 0) {
    let importedTags: Array<Tag> = []
    metadata.value.tags.forEach(t => importedTags.push(createTag(t)))
    tags.value = importedTags
  }
  if (metadata.value?.publishedDate && StringUtils.isNotBlank(metadata.value?.publishedDate)) {
    publishedDate.value = new Date(metadata.value?.publishedDate)
  }
  if (metadata.value?.series != null && metadata.value?.series?.length > 0) {
    seriesCopy.value.push({
      "name": metadata.value?.series,
      "numberInSeries" : metadata.value.numberInSeries
    })
  }
}

const isbn10ValidationMessage = ref("")
const isbn10LabelVariant = ref("")

const isbn13ValidationMessage = ref("")
const isbn13LabelVariant = ref("")

const validateIsbn10 = (isbn: string) => {
  if (StringUtils.isNotBlank(isbn)) {
    const isbnVerify = new IsbnVerify(isbn);
    if (!isbnVerify.isIsbn10()) {
      isbn10ValidationMessage.value = t('labels.invalid_isbn10')
      isbn10LabelVariant.value = "danger"
    }
    else {
      isbn10ValidationMessage.value = ""
      isbn10LabelVariant.value = ""
    }
  }
  else {
    isbn10ValidationMessage.value = ""
    isbn10LabelVariant.value = ""
  }
}

const validateIsbn13 = (isbn: string) => {
  if (StringUtils.isNotBlank(isbn)) {
    const isbnVerify = new IsbnVerify(isbn);
    if (!isbnVerify.isIsbn13()) {
      isbn13ValidationMessage.value = t('labels.invalid_isbn13')
      isbn13LabelVariant.value = "danger"
    }
    else {
      isbn13ValidationMessage.value = ""
      isbn13LabelVariant.value = ""
    }
  }
  else {
    isbn13ValidationMessage.value = ""
    isbn13LabelVariant.value = ""
  }
}

let displayDatepicker = computed(() => {
  return eventType.value !== null && eventType.value !== "NONE"
})

</script>

<template>
  <section>
    <div class="grid">
      <div class="grid sm:grid-cols-3 mb-4 sm:w-10/12 justify-center justify-items-center justify-self-center">
        <div />
        <h1 class="text-2xl typewriter capitalize">
          {{ t('nav.add_book') }}
        </h1>
        <div class="flex gap-2">
          <button
            v-tooltip="t('labels.auto_fill_doc')"
            class="btn btn-success button uppercase"
            :disabled="store != null && !store.getters.getMetadataFetchEnabled"
            @click="toggleModal(false)"
          >
            <span class="icon">
              <i class="mdi mdi-auto-fix mdi-18px" />
            </span>
            <span>{{ t('labels.auto_fill') }}</span>
          </button>
          <button
            v-tooltip="t('labels.auto_fill_book')"
            class="btn btn-primary button uppercase"
            @click="toggleModal(true)"
          >
            <span class="icon">
              <i class="mdi mdi-file-question mdi-18px" />
            </span>
            <span>{{ t('labels.auto_fill') }}</span>
          </button>
          <svg
            v-if="store != null && !store.getters.getMetadataFetchEnabled"
            v-tooltip="t('labels.auto_import_disabled')"
            xmlns="http://www.w3.org/2000/svg"
            class="h-5 w-5 text-warning"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fill-rule="evenodd"
              d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
              clip-rule="evenodd"
            />
          </svg>
        </div>
      </div>
      <div class="form-control sm:w-8/12 justify-self-center">
        <div class="field mb-3">
          <o-field
            horizontal
            :label="t('book.title')"
            class="capitalize"
          >
            <o-input 
              v-model="form.title" 
              class="input focus:input-accent"
            />
          </o-field>
        </div>

        <div class="field jelu-authorinput mb-3">
          <o-field 
            horizontal 
            :label="t('book.author', 2)"
            class="capitalize"
          >
            <o-taginput
              v-model="authors"
              :data="filteredAuthors"
              :allow-autocomplete="true"
              autocomplete="off"
              :allow-new="true"
              :allow-duplicates="false"
              :open-on-focus="true"
              :before-adding="beforeAdd"
              :create-item="createAuthor"
              icon-pack="mdi"
              icon="account-plus"
              field="name"
              :placeholder="t('labels.add_author')"
              @input="getFilteredAuthors"
            />
          </o-field>
        </div>
        <div class="field jelu-taginput mb-3">
          <o-field
            horizontal
            :label="t('book.tag', 2)"
            class="capitalize"
          >
            <o-taginput
              v-model="tags"
              :data="filteredTags"
              :allow-autocomplete="true"
              autocomplete="off"
              :allow-new="true"
              :allow-duplicates="false"
              :open-on-focus="true"
              :before-adding="beforeAddTag"
              :create-item="createTag"
              icon-pack="mdi"
              icon="tag-plus"
              field="name"
              :placeholder="t('labels.add_tag')"
              @input="getFilteredTags"
            />
          </o-field>
        </div>
        <div class="field jelu-authorinput pb-2">
          <o-field
            horizontal
            :label="t('book.translator', 2)"
            class="capitalize"
          >
            <o-taginput
              v-model="translators"
              :data="filteredTranslators"
              :allow-autocomplete="true"
              autocomplete="off"
              :allow-new="true"
              :allow-duplicates="false"
              :open-on-focus="true"
              :before-adding="beforeAddTranslator"
              :create-item="createAuthor"
              icon-pack="mdi"
              icon="account-plus"
              field="name"
              :placeholder="t('labels.add_translator')"
              @input="getFilteredTranslators"
            />
          </o-field>
        </div>
        <div class="field mb-3">
          <o-field
            horizontal
            :label="t('book.summary')"
            class="capitalize"
          >
            <o-input
              v-model="form.summary"
              maxlength="50000"
              type="textarea"
              class="textarea focus:textarea-accent"
            />
          </o-field>
        </div>
        <div class="field mb-3">
          <o-field
            horizontal
            :label="t('book.isbn10')"
            :message="isbn10ValidationMessage"
            :variant="isbn10LabelVariant"
            class="uppercase"
          >
            <o-input
              v-model="form.isbn10"
              name="isbn10"
              :placeholder="t('book.isbn10')"
              class="input focus:input-accent"
              @blur="validateIsbn10($event.target.value)"
            />
          </o-field>
        </div>
        <div class="field mb-3">
          <o-field
            horizontal
            :label="t('book.isbn13')"
            :message="isbn13ValidationMessage"
            :variant="isbn13LabelVariant"
            class="uppercase"
          >
            <o-input
              v-model="form.isbn13"
              name="isbn13"
              :placeholder="t('book.isbn13')"
              class="input focus:input-accent"
              @blur="validateIsbn13($event.target.value)"
            />
          </o-field>
        </div>
        <div class="field">
          <o-field
            horizontal
            :label="t('book.identifiers')"
            class="capitalize"
          >
            <o-input
              v-model="form.googleId"
              name="googleId"
              :placeholder="t('book.google_id')"
              class="input focus:input-accent"
            />
            <o-input
              v-model="form.goodreadsId"
              name="goodreadsId"
              :placeholder="t('book.goodreads_id')"
              class="input focus:input-accent"
            />
            <o-input
              v-model="form.amazonId"
              name="amazonId"
              :placeholder="t('book.amazon_id')"
              class="input focus:input-accent"
            />
            <o-input
              v-model="form.librarythingId"
              name="librarythingId"
              :placeholder="t('book.librarything_id')"
              class="input focus:input-accent"
            />
          </o-field>
        </div>
        <div class="field mb-3">
          <o-field
            horizontal
            :label="t('book.publisher')"
            class="capitalize"
          >
            <o-input
              v-model="form.publisher"
              class="input focus:input-accent"
            />
          </o-field>
        </div>
        <div class="field mb-3">
          <o-field
            horizontal
            :label="t('book.published_date')"
            class="capitalize"
          >
            <o-datepicker
              ref="datepicker"
              v-model="publishedDate"
              :show-week-number="false"
              :locale="undefined"
              :placeholder="t('labels.click_to_select')"
              icon="calendar"
              icon-right="close"
              :icon-right-clickable="true"
              trap-focus
              class="input focus:input-accent"
              @icon-right-click="clearDatePicker"
            />
          </o-field>
        </div>
        <div class="field mb-3">
          <o-field
            horizontal
            :label="t('book.page_count')"
            class="capitalize"
          >
            <o-input
              v-model="form.pageCount"
              type="number"
              min="0"
              class="input focus:input-accent"
            />
          </o-field>
        </div>
        <div class="field mb-3">
          <o-field
            horizontal
            :label="t('book.language')"
            class="capitalize"
          >
            <o-input
              v-model="form.language"
              type="text"
              class="input focus:input-accent"
            />
          </o-field>
        </div>
        <div class="field mb-3">
          <o-field
            :label="t('book.series')"
            horizontal
            class="capitalize"
          >
            <div
              v-for="seriesItem,idx in seriesCopy"
              :key="seriesItem.seriesId"
              class="flex flex-col sm:flex-row items-center grow w-full pb-2"
            >
              <SeriesInput
                :series="seriesItem"
                :parent-series="seriesCopy"
                @update-series="(series: SeriesOrder) => seriesCopy[idx] = series"
              />
              <button
                class="btn btn-error btn-outline sm:btn-sm px-1 sm:border-none"
                @click="seriesCopy.splice(idx, 1)"
              >
                <span class="icon">
                  <i class="mdi mdi-delete mdi-18px" />
                </span>
              </button>
            </div>
          </o-field>
        </div>
        <div class="field mb-3">
          <button
            class="btn btn-primary btn-circle p-2 btn-sm"
            @click="seriesCopy.push({'name' : ''})"
          >
            <span class="icon">
              <i class="mdi mdi-plus mdi-18px" />
            </span>
          </button>
        </div>
        <div class="block">
          <o-field
            horizontal
            :label="t('book.status') + ' :'"
            class="capitalize"
          >
            <o-radio
              v-model="eventType"
              name="type"
              native-value="FINISHED"
            >
              {{ t('reading_events.finished') }}
            </o-radio>
            <o-radio
              v-model="eventType"
              name="type"
              native-value="CURRENTLY_READING"
            >
              {{ t('reading_events.currently_reading') }}
            </o-radio>
            <o-radio
              v-model="eventType"
              name="type"
              native-value="DROPPED"
            >
              {{ t('reading_events.dropped') }}
            </o-radio>
            <o-radio
              v-model="eventType"
              name="type"
              native-value="NONE"
            >
              {{ t('reading_events.none') }}
            </o-radio>
          </o-field>
        </div>
        <div
          v-if="displayDatepicker"
          class="field"
        >
          <o-field
            horizontal
            :label="t('labels.event_date')"
            class="capitalize"
          >
            <o-datepicker
              ref="datepicker"
              v-model="eventDate"
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
            />
          </o-field>
        </div>
        <div class="field my-3">
          <o-field
            horizontal
            :label="t('book.personal_notes')"
            class="capitalize"
          >
            <o-input
              v-model="form.personalNotes"
              maxlength="5000"
              type="textarea"
              class="textarea focus:textarea-accent"
            />
          </o-field>
        </div>
        <div class="field mb-2">
          <o-field
            horizontal
            :label="t('book.owned')"
            class="capitalize"
          >
            <o-checkbox v-model="form.owned">
              {{ ownedDisplay }}
            </o-checkbox>
          </o-field>
        </div>
        <div class="field mb-3">
          <o-field
            horizontal
            :label="t('book.to_read') + ' ?'"
            class="capitalize"
          >
            <o-checkbox v-model="form.toRead">
              {{ toReadDisplay }}
            </o-checkbox>
          </o-field>
        </div>
        <div class="field mb-3">
          <o-field
            horizontal
            :label="t('book.borrowed') + ' ?'"
            class="capitalize"
          >
            <o-checkbox v-model="form.borrowed">
              {{ borrowedDisplay }}
            </o-checkbox>
          </o-field>
        </div>
        <div class="field mb-3">
          <o-field
            horizontal
            :label="t('book.current_page_number')"
            class="capitalize"
          >
            <o-input
              v-model="form.currentPageNumber"
              type="number"
              min="0"
              class="input focus:input-accent"
            />
          </o-field>
        </div>
        <div class="field mb-3">
          <o-field
            horizontal
            :label="t('book.percent_read')"
            class="capitalize"
          >
            <o-slider
              v-model="form.percentRead"
              :min="0"
              :max="100"
            />
          </o-field>
        </div>
        <div
          v-if="hasImage"
          class="mb-4"
        >
          <o-field horizontal>
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
                position="right"
                multiline
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
                  :src="metadata?.image?.startsWith('http') ? metadata?.image : '/files/' + metadata?.image"
                  :class="deleteImage ? 'altered' : ''"
                  alt="cover image"
                >
              </figure>
            </div>
          </o-field>
        </div>
        <div
          v-if="!hasImage || deleteImage"
          class="mb-4"
        >
          <o-field
            horizontal
            :label="t('labels.upload_cover')"
          >
            <div class="form-control">
              <label class="label cursor-pointer justify-center gap-2">
                <span class="label-text">From web</span> 
                <input
                  v-model="uploadType"
                  type="radio"
                  name="radio-10"
                  class="radio radio-primary"
                  value="web"
                >
                <span class="label-text">From computer</span> 
                <input
                  v-model="uploadType"
                  type="radio"
                  name="radio-10"
                  class="radio radio-primary"
                  value="computer"
                >
                <span class="label-text">From Jelu server</span> 
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
          >
            <o-input
              v-model="imageUrl"
              type="url"
              pattern="https?://.*"
              :clearable="true"
              icon-right-clickable
              title="Url must start with http or https"
              :placeholder="t('labels.url_must_start')"
              class="input focus:input-accent"
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
              class="btn btn-primary button"
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

        <div class="field">
          <button
            class="btn btn-success mb-3 uppercase"
            :disabled="!StringUtils.isNotBlank(form.title)"
            :class="{'btn-disabled' : progress}"
            @click="importBook"
          >
            <span
              v-if="progress"
              class="loading loading-spinner"
            />
            {{ t('labels.import_book') }}
          </button>
          <progress
            v-if="progress"
            class="progress progress-success mt-5"
            max="100"
          />
          <p
            v-if="errorMessage"
            class="text-error"
          >
            {{ errorMessage }}
          </p>
        </div>
      </div>
    </div>
  </section>
</template>

<style lang="scss">

</style>
