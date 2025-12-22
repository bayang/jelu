<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next";
import IsbnVerify from '@w0s/isbn-verify';
import { useTitle } from '@vueuse/core';
import { computed, reactive, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { Author } from "../model/Author";
import { Wrapper } from "../model/autocomplete-wrapper";
import { UserBook } from "../model/Book";
import { Path } from "../model/DirectoryListing";
import { Metadata } from "../model/Metadata";
import { SeriesOrder } from "../model/Series";
import { Tag } from "../model/Tag";
import dataService from "../services/DataService";
import { key } from '../store';
import { ObjectUtils } from "../utils/ObjectUtils";
import { StringUtils } from "../utils/StringUtils";
import AutoImportFileModalVue from "./AutoImportFileModal.vue";
import AutoImportFormModalVue from "./AutoImportFormModal.vue";
import ImagePickerModal from "./ImagePickerModal.vue";
import SeriesCompleteInput from "./SeriesCompleteInput.vue";
import ClosableBadge from "./ClosableBadge.vue";
import FormField from "./FormField.vue";
import { Role } from "../model/Role";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

useTitle('Jelu | ' + t('nav.add_book'))

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
  isfdbId: "",
  openlibraryId: "",
  noosfereId: "",
  inventaireId: "",
  language: ""
});
const eventType = ref(null);
const eventDate: Ref<Date|null> = ref(new Date());
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

const filteredAuthors: Ref<Array<Wrapper>> = ref([]);
const authors: Ref<Array<Author>> = ref([]);

const filteredTags: Ref<Array<Wrapper>> = ref([]);
const tags: Ref<Array<Tag>> = ref([]);

const translators: Ref<Array<Author>> = ref([]);
const filteredTranslators: Ref<Array<Wrapper>> = ref([]);

const narrators: Ref<Array<Author>> = ref([]);
const filteredNarrators: Ref<Array<Wrapper>> = ref([]);

const filteredPublishers: Ref<Array<string>> = ref([])

const seriesCopy: Ref<Array<SeriesOrder>> = ref([])

const showModal: Ref<boolean> = ref(false)
const metadata: Ref<Metadata | null> = ref(null)

const showImagePickerModal: Ref<boolean> = ref(false)

const hasImage = computed(() => {
  return StringUtils.isNotBlank(metadata.value?.image)
})
const deleteImage: Ref<boolean> = ref(false)

function toggleRemoveImage() {
  deleteImage.value = !deleteImage.value
}

const importBook = async () => {
  console.log("import book");
  if (StringUtils.isNotBlank(form.title)) {
    const alreadyExisting = await dataService.checkIsbnExists(form.isbn10, form.isbn13)
    console.log('already existing')
    console.log(alreadyExisting)
    let saveBook = true
    if (alreadyExisting != null) {
      saveBook = false
      await ObjectUtils.swalYesNoMixin.fire({
        html: `<p>${t('labels.book_with_same_isbn_already_exists')}:<br>${alreadyExisting.title}<br>${t('labels.save_new_anyway')}</p>`,
        showDenyButton: false,
        confirmButtonText: t('labels.save'),
        cancelButtonText: t('labels.dont_save')
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
    const userBook: UserBook = fillBook(form, publishedDate.value)
    authors.value.forEach((a) => {
        userBook.book.authors?.push(a)
    });
    tags.value.forEach((t) => userBook.book.tags?.push(t));
    translators.value.forEach((tr) => userBook.book.translators?.push(tr));
    narrators.value.forEach((n) => userBook.book.narrators?.push(n))
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
    if (eventType.value !== null && eventType.value !== "NONE" && eventDate.value != null) {
      console.log(
        "type " + StringUtils.readingEventTypeForValue(eventType.value)
      );
      userBook.lastReadingEvent = StringUtils.readingEventTypeForValue(eventType.value);
      userBook.lastReadingEventDate = eventDate.value?.toISOString()
    }
    try {
      console.log(`push book ` + userBook);
      console.log(userBook);
      progress.value = true
      const res: UserBook = await dataService.saveUserBookImage(
        userBook,
        file.value,
        (event: { loaded: number; total: number }) => {
          const percent = Math.round((100 * event.loaded) / event.total);
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
  const userBook: UserBook = {
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
      isfdbId: formdata.isfdbId,
      openlibraryId: formdata.openlibraryId,
      noosfereId: formdata.noosfereId,
      inventaireId: formdata.inventaireId,
      language: formdata.language,
      authors: [],
      translators: [],
      narrators: [],
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
  narrators.value = []
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
  form.isfdbId = ""
  form.openlibraryId = ""
  form.noosfereId = ""
  form.inventaireId = ""
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

function getFilteredData(text: string, target: Array<Wrapper>) {
  dataService.findAuthorByCriteria(Role.ANY, text).then((data) => {
    target.splice(0, target.length)
    data.content.forEach(a => target.push(ObjectUtils.wrapForOptions(a)))
  })
}

function getFilteredTags(text: string) {
  filteredTags.value.splice(0, filteredTags.value.length)
  dataService.findTagsByCriteria(text).then((data) => data.content.forEach(t => filteredTags.value.push(ObjectUtils.wrapForOptions(t))))
}

function getFilteredPublishers(text: string) {
  form.publisher = text
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

function selectPublisher(publisher: string) {
  // we receive from oruga weird events while nothing is selected
  // so try to get rid of those null data we receive
  if (publisher != null) {
    form.publisher = publisher
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
  for (const key in metadata.value) {
    console.log("key")
    console.log(key)
    if (key in form) {
      const castKey = key as (keyof typeof metadata.value & keyof typeof form);
      (form[castKey] as any) = metadata.value[castKey];
    }
  }
  if (metadata.value?.authors != null && metadata.value.authors.length > 0) {
    const auths: Array<Author> = []
    metadata.value.authors.forEach(a => auths.push(ObjectUtils.createNamedItem(a)))
    authors.value = auths
  }
  if (metadata.value?.tags != null && metadata.value.tags.length > 0) {
    const importedTags: Array<Tag> = []
    metadata.value.tags.forEach(t => importedTags.push(ObjectUtils.createNamedItem(t)))
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

const isbn13ValidationMessage = ref("")

const validateIsbn10 = (isbn: string) => {
  if (StringUtils.isNotBlank(isbn)) {
    const isbnVerify = new IsbnVerify(isbn);
    if (!isbnVerify.isIsbn10()) {
      isbn10ValidationMessage.value = t('labels.invalid_isbn10')
    }
    else {
      isbn10ValidationMessage.value = ""
    }
  }
  else {
    isbn10ValidationMessage.value = ""
  }
}

const validateIsbn13 = (isbn: string) => {
  if (StringUtils.isNotBlank(isbn)) {
    const isbnVerify = new IsbnVerify(isbn);
    if (!isbnVerify.isIsbn13()) {
      isbn13ValidationMessage.value = t('labels.invalid_isbn13')
    }
    else {
      isbn13ValidationMessage.value = ""
    }
  }
  else {
    isbn13ValidationMessage.value = ""
  }
}

const displayDatepicker = computed(() => {
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
      <div class="sm:w-8/12 justify-self-center">
        <FormField
          v-model="form.title"
          :legend="t('book.title')"
          placeholder=""
        />
        <fieldset class="fieldset jelu-authorinput">
          <legend class="fieldset-legend capitalize">
            {{ t('book.author', 2) }}
          </legend>
          <o-taginput
            v-model="authors"
            :allow-autocomplete="true"
            autocomplete="off"
            :allow-new="true"
            :allow-duplicates="false"
            :open-on-focus="true"
            :options="filteredAuthors"
            :validate-item="(item: Author|string) => beforeAdd(item, authors)"
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
            v-model="tags"
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
            field="name"
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
        <fieldset class="field jelu-authorinput pb-2">
          <legend class="fieldset-legend capitalize">
            {{ t('book.translator', 2) }}
          </legend>
          <o-taginput
            v-model="translators"
            :options="filteredTranslators"
            :allow-autocomplete="true"
            autocomplete="off"
            :allow-new="true"
            :allow-duplicates="false"
            :open-on-focus="true"
            :validate-item="(item: Author) => beforeAdd(item, translators)"
            :create-item="ObjectUtils.createNamedItem"
            icon-pack="mdi"
            icon="account-plus"
            field="name"
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
        <fieldset class="field jelu-authorinput pb-2">
          <legend class="fieldset-legend capitalize">
            {{ t('book.narrator', 2) }}
          </legend>
          <o-taginput
            v-model="narrators"
            :options="filteredNarrators"
            :allow-autocomplete="true"
            autocomplete="off"
            :allow-new="true"
            :allow-duplicates="false"
            :open-on-focus="true"
            :validate-item="(item: Author) => beforeAdd(item, narrators)"
            :create-item="ObjectUtils.createNamedItem"
            icon-pack="mdi"
            icon="account-plus"
            field="name"
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
            v-model="form.summary"
            maxlength="50000"
            class="textarea focus:textarea-accent w-full"
          />
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('book.isbn10') }}
          </legend>
          <input
            v-model="form.isbn10"
            type="text"
            name="isbn10"
            class="input w-full focus:input-accent validator"
            :valid="isbn10ValidationMessage.length < 1"
            @blur="validateIsbn10($event.target.value)"
          >
          <div class="text-error">
            {{ isbn10ValidationMessage }}
          </div>
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('book.isbn13') }}
          </legend>
          <input
            v-model="form.isbn13"
            type="text"
            name="isbn13"
            class="input w-full focus:input-accent validator"
            :valid="isbn13ValidationMessage.length < 1"
            @blur="validateIsbn13($event.target.value)"
          >
          <div class="text-error">
            {{ isbn13ValidationMessage }}
          </div>
        </fieldset>
        <fieldset class="fieldset sm:grid sm:grid-cols-3">
          <legend class="fieldset-legend capitalize providers-ids">
            {{ t('book.identifiers') }}
          </legend>
          <input
            v-model="form.googleId"
            name="googleId"
            :placeholder="t('book.google_id')"
            class="input focus:input-accent w-full"
          >
          <input
            v-model="form.goodreadsId"
            type="text"
            name="goodreadsId"
            :placeholder="t('book.goodreads_id')"
            class="input focus:input-accent w-full"
          >
          <input
            v-model="form.amazonId"
            type="text"
            name="amazonId"
            :placeholder="t('book.amazon_id')"
            class="input focus:input-accent w-full"
          >
          <input
            v-model="form.librarythingId"
            type="text"
            name="librarythingId"
            :placeholder="t('book.librarything_id')"
            class="input focus:input-accent w-full"
          >
          <input
            v-model="form.isfdbId"
            type="text"
            name="isfdbId"
            :placeholder="t('book.isfdb_id')"
            class="input focus:input-accent w-full"
          >
          <input
            v-model="form.openlibraryId"
            type="text"
            name="openlibraryId"
            :placeholder="t('book.openlibrary_id')"
            class="input focus:input-accent w-full"
          >
          <input
            v-model="form.noosfereId"
            type="text"
            name="noosfereId"
            :placeholder="t('book.noosfere_id')"
            class="input focus:input-accent w-full"
          >
          <input
            v-model="form.inventaireId"
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
            v-model="form.publisher"
            :root-class="'grow w-full'"
            :input-classes="{rootClass:'w-full border-2 border-accent', inputClass:'w-full'}"
            :clear-on-select="false"
            backend-filtering
            :debounce="100"
            :options="filteredPublishers"
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
            expanded
            @icon-right-click="clearDatePicker"
          />
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('book.page_count') }}
          </legend>
          <label class="input w-full">
            <input
              v-model="form.pageCount"
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
              @click="form.pageCount = null; form.currentPageNumber = null"
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
          v-model="form.language"
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
        <fieldset class="block fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('book.status') }}
          </legend>
          <div class="">
            <label class="label cursor-pointer justify-center gap-2 flex flex-wrap">
              <div>
                <input
                  v-model="eventType"
                  type="radio"
                  name="radio-10"
                  class="radio radio-primary mx-3"
                  value="FINISHED"
                >
                <span class="label-text">{{ t('reading_events.finished') }}</span>
              </div>
              <div>
                <input
                  v-model="eventType"
                  type="radio"
                  name="radio-10"
                  class="radio radio-primary mx-3"
                  value="CURRENTLY_READING"
                >
                <span class="label-text">{{ t('reading_events.currently_reading') }}</span>
              </div>
              <div>
                <input
                  v-model="eventType"
                  type="radio"
                  name="radio-10"
                  class="radio radio-primary mx-3"
                  value="DROPPED"
                >
                <span class="label-text">{{ t('reading_events.dropped') }}</span>
              </div>
              <div>
                <input
                  v-model="eventType"
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
        <fieldset
          v-if="displayDatepicker"
          class="fieldset"
        >
          <legend class="fieldset-legend capitalize">
            {{ t('labels.event_date') }}
          </legend>
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
            @icon-right-click="eventDate = null"
          />
        </fieldset>
        <fieldset class="fieldset">
          <legend class="fieldset-legend capitalize">
            {{ t('book.personal_notes') }}
          </legend>
          <textarea
            v-model="form.personalNotes"
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
                v-model="form.owned"
                type="checkbox"
                class="checkbox checkbox-primary"
              >
              {{ ownedDisplay }}
            </label>
          </fieldset>
          <fieldset class="fieldset">
            <legend class="fieldset-legend capitalize">
              {{ t('book.to_read') }}&nbsp;?
            </legend>
            <label class="label">
              <input
                v-model="form.toRead"
                type="checkbox"
                class="checkbox checkbox-primary"
              >
              {{ toReadDisplay }}
            </label>
          </fieldset>
          <fieldset class="fieldset">
            <legend class="fieldset-legend capitalize">
              {{ t('book.borrowed') }}&nbsp;?
            </legend>
            <label class="label">
              <input
                v-model="form.borrowed"
                type="checkbox"
                class="checkbox checkbox-primary"
              >
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
              v-model="form.currentPageNumber"
              type="number"
              class="input focus:input-accent"
              min="0"
              :disabled="form.pageCount == null"
              :max="form.pageCount"
            >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
              class="size-6 hover:cursor-pointer"
              @click="form.currentPageNumber = null"
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
            v-model="form.percentRead"
            type="range"
            min="0"
            max="100"
            class="w-full range range-primary range-xs"
          >
        </fieldset>
        <fieldset
          v-if="hasImage"
          class="fieldset"
        >
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
              position="right"
              multiline
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
                :src="metadata?.image?.startsWith('http') ? metadata?.image : '/files/' + metadata?.image"
                :class="deleteImage ? 'altered' : ''"
                alt="cover image"
              >
            </figure>
          </div>
        </fieldset>
        <div
          v-if="!hasImage || deleteImage"
        >
          <fieldset
            class="fieldset"
          >
            <legend class="fieldset-legend capitalize">
              {{ t('labels.upload_cover') }}
            </legend>
            <div class="">
              <label class="label cursor-pointer justify-center gap-2 flex flex-wrap">
                <div>
                  <input
                    v-model="uploadType"
                    type="radio"
                    name="radio-11"
                    class="radio radio-primary mx-3"
                    value="web"
                  >
                  <span class="label-text">{{ t('labels.upload_from_web') }}</span>
                </div>
                <div>
                  <input
                    v-model="uploadType"
                    type="radio"
                    name="radio-11"
                    class="radio radio-primary mx-3"
                    value="computer"
                  >
                  <span class="label-text">{{ t('labels.upload_from_computer') }}</span>
                </div>
                <div>
                  <input
                    v-model="uploadType"
                    type="radio"
                    name="radio-11"
                    class="radio radio-primary mx-3"
                    value="server"
                  >
                  <span class="label-text">{{ t('labels.upload_from_server') }}</span>
                </div>
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
            <p class="validator-hint">
              {{ t('labels.url_must_start') }}
            </p>
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
              class="btn btn-primary button"
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
