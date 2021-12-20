<script setup lang="ts">
import { computed, reactive, Ref, ref } from "vue";
import { useStore } from "vuex";
import { UserBook } from "../model/Book";
import dataService from "../services/DataService";
import { key } from "../store";
import { StringUtils } from "../utils/StringUtils";
import { useProgrammatic } from "@oruga-ui/oruga-next";
import { Author } from "../model/Author";

const oruga = useProgrammatic();

const datepicker = ref(null);
const form = reactive({
  title: "",
  summary: "",
  isbn10: "",
  isbn13: "",
  publisher: "",
  pageCount: null,
  publishedDate: null,
  series: "",
  numberInSeries: null,
  personalNotes: "",
  owned: null,
  toRead: null
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
// load existing authors from db here
// const data: Ref<Array<string>> = ref([]);
// let filteredAuthors: Ref<Array<string>> = ref(data);
let filteredAuthors: Ref<Array<Author>> = ref([]);
let authors: Ref<Array<Author>> = ref([]);

const importBook = async () => {
  console.log("import book");
  if (StringUtils.isNotBlank(form.title)) {
      let userBook: UserBook = fillBook(form)
      // authors.value.forEach((a) => userBook.book.authors?.push({ name: a }));
      authors.value.forEach((a) => userBook.book.authors?.push(a));
    if (StringUtils.isNotBlank(imageUrl.value)) {
      userBook.book.image = imageUrl.value;
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
      toast("success", `Book ${res.book.title} imported !`, 4000);
      clearForm();
    } catch (error: any) {
      // errorMessage.value = error.message
      toast("danger", `Error ` + error.message, 4000);
    }
  } else {
    errorMessage.value = "provide at least a title";
  }
};

const fillBook = (formdata: any): UserBook => {
  let userBook: UserBook = {
    book : {
      title : formdata.title,
      isbn10: formdata.isbn10,
      isbn13: formdata.isbn13,
      summary: formdata.summary,
      publisher: formdata.publisher,
      image : formdata.image,
      pageCount : formdata.pageCount,
      publishedDate : formdata.publishedDate,
      series : formdata.series,
      numberInSeries : formdata.numberInSeries,
      authors: []
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
  uploadPercentage.value = 0;
  form.title = "";
  form.summary = "";
  form.isbn10 = "";
  form.isbn13 = "";
  form.publisher = "";
  form.pageCount = null;
  form.publishedDate = null;
  form.series = ""
  form.numberInSeries = null
  form.owned = null
  form.personalNotes = ""
};

const toast = (variant: string, message: string, duration: number = 2000) => {
  oruga.oruga.notification.open({
    message: message,
    position: "top",
    variant: variant,
    duration: duration,
  });
};

const clearDatePicker = () => {
  // close datepicker on reset
  form.publishedDate = null;
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

function createAuthor(item: Author|string) {
  if (item instanceof Object) {
    return item
  }
  return {
    "name" : item
  }
}
</script>

<template>
  <h1 class="title">Add book</h1>
  <section>
    <div class="columns is-multiline is-centered">
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
      <p>auth <span v-for="author in authors" v-bind:key="author.id">{{author.name}}</span></p>
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
      <o-field horizontal label="ISBN">
        <o-input
          name="isbn10"
          v-model="form.isbn10"
          placeholder="isbn10"
        ></o-input>
        <o-input
          name="isbn13"
          v-model="form.isbn13"
          placeholder="isbn13"
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
          v-model="form.publishedDate"
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

// .dropdown-item {
//   color: #363636;
// }

// $inputitems-color: #363636;
// $inputitems-item-color: #363636;
// $inputitems-item-background-color: #363636;
// $inputitems-background-color: #363636;

</style>
