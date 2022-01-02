<script setup lang="ts">
import { computed, reactive, Ref, ref } from "vue";
import { key } from "../store";
import { useStore } from "vuex";
import { UserBook } from "../model/Book";
import dataService from "../services/DataService";
import { StringUtils } from "../utils/StringUtils";
import { useProgrammatic } from "@oruga-ui/oruga-next";
import { Author } from "../model/Author";
import { Tag } from "../model/Tag";
import AutoImportFormModalVue from "./AutoImportFormModal.vue";
import { Metadata } from "../model/Metadata";
import { ObjectUtils } from "../utils/ObjectUtils";
import IsbnVerify from '@saekitominaga/isbn-verify';
import Swal from 'sweetalert2';

const {oruga} = useProgrammatic();

const datepicker = ref(null);
const publishedDate:Ref<Date|null> = ref(null)
const form = reactive({
  title: "",
  summary: "",
  isbn10: "",
  isbn13: "",
  publisher: "",
  pageCount: null,
  // publishedDate: <Date> null,
  series: "",
  numberInSeries: null,
  personalNotes: "",
  owned: null,
  toRead: null,
  googleId: "",
  amazonId : "",
  goodreadsId: "",
  librarythingId : ""
});
const eventType = ref(null);
const imageUrl = ref<string | null>(null);
const file = ref(null);
const isSwitchedCustom = ref("Upload from the web");
const uploadPercentage = ref(0);
const errorMessage = ref("");
const ownedDisplay = computed(() => {
    if (form.owned) {
      return "Owned"
    }
    return ""
  })
const toReadDisplay = computed(() => {
    if (form.toRead) {
      return "Book will be added to to-read list"
    }
    return ""
  })
let filteredAuthors: Ref<Array<Author>> = ref([]);
let authors: Ref<Array<Author>> = ref([]);

let filteredTags: Ref<Array<Tag>> = ref([]);
let tags: Ref<Array<Tag>> = ref([]);

const showModal: Ref<Boolean> = ref(false)
const metadata: Ref<Metadata|null> = ref(null)
// let hasImage: Ref<boolean> = ref(metadata?.value?.image != null)
let hasImage = computed(() => {
  return StringUtils.isNotBlank(metadata.value?.image)
})
let deleteImage: Ref<boolean> = ref(false)

function toggleRemoveImage() {
  deleteImage.value = !deleteImage.value
}

let swalMixin = Swal.mixin({
  background: '#404040',
  color: '#ffffff',
})

const importBook = async () => {
  console.log("import book");
  if (StringUtils.isNotBlank(form.title)) {
    let alreadyExisting = await checkIsbnExists(form.isbn10, form.isbn13)
    console.log('already existing')
    console.log(alreadyExisting)
    let saveBook = true
    if (alreadyExisting != null) {
      saveBook = false
      await swalMixin.fire({
        html: `<p>Book with same isbn already exists:<br>${alreadyExisting.title}<br>Do you want to save the save a new one anyway?</p>`,
        showDenyButton: true,
        confirmButtonText: 'Save',
        denyButtonText: `Don't save`,
      }).then((result) => {
        if (result.isConfirmed) {
          saveBook = true
        } else if (result.isDenied) {
          swalMixin.fire('', 'Changes are not saved', 'info')
        }
      })
    }
    console.log(`save book ${saveBook}`)
    if (! saveBook) {
      return
    }
      let userBook: UserBook = fillBook(form, publishedDate.value)
      authors.value.forEach((a) => userBook.book.authors?.push(a));
      tags.value.forEach((t) => userBook.book.tags?.push(t));
    if (StringUtils.isNotBlank(imageUrl.value)) {
      userBook.book.image = imageUrl.value;
    }
    else if (!deleteImage.value 
      && metadata.value?.image != null 
      && StringUtils.isNotBlank(metadata.value.image)) {
      userBook.book.image = metadata.value.image
    }
    if (eventType.value !== null && eventType.value !== "NONE") {
      console.log(
        "type " + StringUtils.readingEventTypeForValue(eventType.value)
      );
      userBook.lastReadingEvent = StringUtils.readingEventTypeForValue(eventType.value);
    }
    try {
      console.log(`push book ` + userBook);
      console.log(userBook);
      let res: UserBook = await dataService.saveUserBookImage(
        userBook,
        file.value,
        (event: { loaded: number; total: number }) => {
          let percent = Math.round((100 * event.loaded) / event.total);
          console.log("percent " + percent);
          uploadPercentage.value = percent;
        }
      );
      console.log(`saved book ${res.book.title}`);
      ObjectUtils.toast(oruga, "success", `Book ${res.book.title} imported !`, 4000)
      clearForm();
    } catch (error: any) {
      // errorMessage.value = error.message
      ObjectUtils.toast(oruga, "danger", `Error ` + error.message, 4000)
    }
  } else {
    errorMessage.value = "provide at least a title";
  }
};

const fillBook = (formdata: any, publishedDate : Date|null): UserBook => {
  let userBook: UserBook = {
    book : {
      title : formdata.title,
      isbn10: formdata.isbn10,
      isbn13: formdata.isbn13,
      summary: formdata.summary,
      publisher: formdata.publisher,
      image : formdata.image,
      pageCount : formdata.pageCount,
      publishedDate : publishedDate?.toISOString(),
      series : formdata.series,
      numberInSeries : formdata.numberInSeries,
      googleId: formdata.googleId,
      amazonId: formdata.amazonId,
      goodreadsId: formdata.goodreadsId,
      librarythingId: formdata.librarythingId,
      authors: [],
      tags: []
    },
    owned : formdata.owned,
    personalNotes: formdata.personalNotes,
    toRead: formdata.toRead
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
  uploadPercentage.value = 0;
  form.title = "";
  form.summary = "";
  form.isbn10 = "";
  form.isbn13 = "";
  form.publisher = "";
  form.pageCount = null;
  // form.publishedDate = null;
  publishedDate.value = null
  form.series = ""
  form.numberInSeries = null
  form.owned = null
  form.personalNotes = ""
  form.amazonId = ""
  form.googleId = ""
  form.goodreadsId = ""
  form.librarythingId = ""

};

const clearDatePicker = () => {
  // close datepicker on reset
  // form.publishedDate = null;
  publishedDate.value = null
};

const handleFileUpload = (event: any) => {
  file.value = event.target.files[0];
};

const clearImageField = () => {
  imageUrl.value = "";
};

function getFilteredAuthors(text: string) {
  dataService.findAuthorByCriteria(text).then((data) => filteredAuthors.value = data)
}

function getFilteredTags(text: string) {
  dataService.findTagsByCriteria(text).then((data) => filteredTags.value = data)
}

function beforeAdd(item: Author|string) {
  let shouldAdd = true
  if (item instanceof Object) {
    authors.value.forEach(author => {
      console.log(`author ${author.name}`)
      if(author.name === item.name) {
        console.log(`author ${author.name} item ${item.name}`)
        shouldAdd = false;
      }
    });
  }
  else {
    authors.value.forEach(author => {
      console.log(`author ${author.name}`)
      if(author.name === item) {
        console.log(`author ${author.name} item ${item}`)
        shouldAdd = false;
      }
    });
  }
    return shouldAdd
}

function beforeAddTag(item: Tag|string) {
  let shouldAdd = true
  if (item instanceof Object) {
    tags.value.forEach(tag => {
      console.log(`tag ${tag.name}`)
      if(tag.name === item.name) {
        console.log(`tag ${tag.name} item ${item.name}`)
        shouldAdd = false;
      }
    });
  }
  else {
    tags.value.forEach(tag => {
      console.log(`tag ${tag.name}`)
      if(tag.name === item) {
        console.log(`tag ${tag.name} item ${item}`)
        shouldAdd = false;
      }
    });
  }
    return shouldAdd
}

function createAuthor(item: Author|string) {
  if (item instanceof Object) {
    return item
  }
  return {
    "name" : item
  }
}

function createTag(item: Tag|string) {
  if (item instanceof Object) {
    return item
  }
  return {
    "name" : item
  }
}

const toggleModal = () => {
  showModal.value = ! showModal.value
  oruga.modal.open({
    parent: this,
          component: AutoImportFormModalVue,
          trapFocus: true,
          active: true,
          canCancel: ['x', 'button', 'outside'],
          scroll: 'keep',
          events: {
            metadataReceived : (modalMetadata: Metadata) => {
              console.log("received metadata")
              console.log(modalMetadata)
              metadata.value = modalMetadata
              mergeMetadata()
            }
          },
          onClose: modalClosed
        });
}

function modalClosed(args: any) {
  console.log("modal closed")
  // getBook()
}

const mergeMetadata = () => {
  for (let key in metadata.value) {
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
}

const isbn10ValidationMessage = ref("")
const isbn10LabelVariant = ref("")

const isbn13ValidationMessage = ref("")
const isbn13LabelVariant = ref("")

const validateIsbn10 = (isbn: string) => {
  if (StringUtils.isNotBlank(isbn)) {
    const isbnVerify = new IsbnVerify(isbn);
    if (! isbnVerify.isIsbn10()) {
      isbn10ValidationMessage.value = "Invalid isbn10"
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
    if (! isbnVerify.isIsbn13()) {
      isbn13ValidationMessage.value = "Invalid isbn13"
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


async function checkIsbnExists(isbn10: string, isbn13: string) {
  console.log(isbn10 + " " + isbn13)
  if (StringUtils.isNotBlank(isbn10)) {
    let res = await dataService.findBooks(undefined, isbn10, undefined)
    console.log(res.empty)
    if (!res.empty) {
      return res.content[0]
    }
  }
  if (StringUtils.isNotBlank(isbn13)) {
    console.log(isbn13)
    let res = await dataService.findBooks(undefined, undefined, isbn13)
    console.log(res.empty)
    if (!res.empty) {
      return res.content[0]
    }
  }
  return null
}
</script>

<template>
  <section>
    <div class="columns is-multiline is-centered">
      <div class="column is-centered is-offset-one-fifth is-three-fifths">
        <h1 class="title">Add book</h1>
      </div>
      <div class="column is-one-fifth">
        <o-tooltip label="Try to auto fill some fields from the web, given a isbn or a title" multiline>
        <button @click="toggleModal" class="button is-primary is-light">Auto fill</button>
        </o-tooltip>
      </div>
    <div class="column is-two-thirds">
<div class="field">
      <o-field horizontal label="Title">
        <o-input v-model="form.title"></o-input>
      </o-field>
    </div>

    <div class="field">
      <o-field horizontal label="Authors">
        <o-inputitems
          v-model="authors"
          :data="filteredAuthors"
          :autocomplete="true"
          :allow-new="true"
          :allow-duplicates="false"
          :open-on-focus="true"
          :beforeAdding="beforeAdd"
          :createItem="createAuthor"
          iconPack="mdi"
          icon="account-plus"
          field="name"
          placeholder="Add an author"
          @typing="getFilteredAuthors"
        >
        </o-inputitems>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="Tags">
        <o-inputitems
          v-model="tags"
          :data="filteredTags"
          :autocomplete="true"
          :allow-new="true"
          :allow-duplicates="false"
          :open-on-focus="true"
          :beforeAdding="beforeAddTag"
          :createItem="createTag"
          iconPack="mdi"
          icon="account-plus"
          field="name"
          placeholder="Add a tag"
          @typing="getFilteredTags"
        >
        </o-inputitems>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="Summary">
        <o-input
          maxlength="200"
          type="textarea"
          v-model="form.summary"
        ></o-input>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="ISBN10"
      :message="isbn10ValidationMessage"
      :variant="isbn10LabelVariant">
        <o-input
          name="isbn10"
          v-model="form.isbn10"
          placeholder="isbn10"
          @blur="validateIsbn10($event.target.value)"
        ></o-input>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="ISBN13"
      :message="isbn13ValidationMessage"
      :variant="isbn13LabelVariant">
        <o-input
          name="isbn13"
          v-model="form.isbn13"
          placeholder="isbn13"
          @blur="validateIsbn13($event.target.value)"
        ></o-input>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="Identifiers">
        <o-input
          name="googleId"
          v-model="form.googleId"
          placeholder="googleId"
        ></o-input>
        <o-input
          name="goodreadsId"
          v-model="form.goodreadsId"
          placeholder="goodreadsId"
        ></o-input>
        <o-input
          name="amazonId"
          v-model="form.amazonId"
          placeholder="amazonId"
        ></o-input>
        <o-input
          name="librarythingId"
          v-model="form.librarythingId"
          placeholder="librarythingId"
        ></o-input>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="Publisher">
        <o-input v-model="form.publisher"></o-input>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="Published date">
        <o-datepicker
          v-model="publishedDate"
          :show-week-number="false"
          :locale="undefined"
          placeholder="Click to select..."
          icon="calendar"
          ref="datepicker"
          iconRight="close"
          iconRightClickable="true"
          @icon-right-click="clearDatePicker"
          trap-focus
        >
        </o-datepicker>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="Page count">
        <o-input v-model="form.pageCount" type="number" min="0"></o-input>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="Series">
        <o-input v-model="form.series"></o-input>
        <o-input v-model="form.numberInSeries" type="number" min="0" step="0.1"></o-input>
      </o-field>
    </div>
    <div class="block">
      <o-field horizontal label="Status : ">
        <o-radio v-model="eventType" name="type" native-value="FINISHED">
          Finished
        </o-radio>
        <o-radio
          v-model="eventType"
          name="type"
          native-value="CURRENTLY_READING"
        >
          Currently reading
        </o-radio>
        <o-radio v-model="eventType" name="type" native-value="DROPPED">
          Dropped
        </o-radio>
        <o-radio v-model="eventType" name="type" native-value="NONE">
          None
        </o-radio>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="Personal notes">
        <o-input
          maxlength="200"
          type="textarea"
          v-model="form.personalNotes"
        ></o-input>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="Owned">
      <o-checkbox v-model="form.owned">
        {{ ownedDisplay }}
      </o-checkbox>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="To read ?">
      <o-checkbox v-model="form.toRead">
        {{ toReadDisplay }}
      </o-checkbox>
      </o-field>
    </div>
    <div v-if="hasImage">
  <o-field horizontal>
    <template v-slot:label>
        Actual cover :
        <o-tooltip v-if="!deleteImage" label="Click bin to remove current cover and upload another one" position="right">
          <span class="icon">
          <i class="mdi mdi-information-outline"></i> </span>
        </o-tooltip>
          <o-tooltip v-if="deleteImage" label="Press refresh to restore cover" position="right">
            <span class="icon">
          <i class="mdi mdi-information-outline"></i> </span>
        </o-tooltip>
      </template>
  <figure class="small-cover">
          <img
            :src="'/files/' + metadata?.image"
            :class="deleteImage ? 'altered' : ''"
            alt="cover image"
            
          />
          <!-- <button v-if="!deleteImage" @click="toggleRemoveImage" class="delete is-large overlay-button"></button> -->
          <span v-if="!deleteImage" @click="toggleRemoveImage" class="icon overlay-button">
          <i class="mdi mdi-delete"></i> </span>  
          <span v-if="deleteImage" @click="toggleRemoveImage" class="icon overlay-button">
          <i class="mdi mdi-autorenew"></i> </span>
        </figure>
  </o-field>
</div>
    <div v-if="!hasImage || deleteImage">
    <o-field horizontal label="Upload book cover">
      <o-switch
        v-model="isSwitchedCustom"
        true-value="Upload from file"
        false-value="Upload from the web"
        :leftLabel="true"
      >
        {{ isSwitchedCustom }}
      </o-switch>
    </o-field>
    <o-field
      v-if="isSwitchedCustom == 'Upload from the web'"
      horizontal
      label="Enter image adress"
    >
      <o-input
        v-model="imageUrl"
        type="url"
        pattern="https?://.*"
        clearable="true"
        icon-right-clickable
        @icon-right-click="clearImageField"
        title="Url must start with http or https"
        placeholder="Url must start with http or https"
      >
      </o-input>
    </o-field>
    <o-field
      v-else
      horizontal
      label="Choose file"
      class="file is-primary has-name"
    >
      <input type="file" accept="image/*" @change="handleFileUpload($event)" />
      <br />
      <progress max="100" :value.prop="uploadPercentage"></progress>
      <br />
    </o-field>
    </div>

    <div class="field">
      <p class="control">
        <button @click="importBook" class="button is-success">
          Import book
        </button>
      </p>
      <p v-if="errorMessage" class="has-text-danger">{{ errorMessage }}</p>
    </div>
    </div>
    </div>

  </section>
</template>

<style lang="scss">
@import "../assets/style.scss";

</style>
