<script setup lang="ts">

import { computed, Ref, ref, watch } from "vue";
import { Author } from "../model/Author";
import { UserBook } from "../model/Book";
import { ReadingEventType } from "../model/ReadingEvent";
import dataService from "../services/DataService";
import { DateUtils } from "../utils/DateUtils";
import { ObjectUtils } from "../utils/ObjectUtils";
import { StringUtils } from "../utils/StringUtils";
import { useProgrammatic } from "@oruga-ui/oruga-next";
import { Tag } from "../model/Tag";

const props = defineProps<{ bookId: string, book: UserBook|null }>()
const oruga = useProgrammatic();
const emit = defineEmits(['close']);

let authors: Ref<Array<Author>|undefined> = ref(props.book?.book.authors);
let filteredAuthors: Ref<Array<Author>> = ref([]);
let tags: Ref<Array<Tag>|undefined> = ref(props.book?.book.tags);
let filteredTags: Ref<Array<Tag>> = ref([]);
let userbook: Ref<UserBook> = ref(copyInput(props.book))
let hasImage: Ref<boolean> = ref(userbook.value.book.image != null)
let deleteImage: Ref<boolean> = ref(false)

const publishedDate = ref(new Date(userbook.value.book.publishedDate ? userbook.value.book.publishedDate: ""))

function copyInput(book: UserBook|null): any {
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

const clearImageField = () => {
  imageUrl.value = "";
};

const imageUrl = ref<string | null>(null);
const file = ref(null);
const isSwitchedCustom = ref("Upload from the web");
const uploadPercentage = ref(0);
const errorMessage = ref("");
const datepicker = ref(null)
const ownedDisplay = computed(() => {
    if (userbook.value.owned) {
      return "Owned"
    }
    return ""
  })
const toReadDisplay = computed(() => {
    if (userbook.value.toRead) {
      return "Book will be added to to-read list"
    }
    return ""
  })

const importBook = () => {
  console.log("import")
  console.log(userbook)
  if (userbook.value.lastReadingEvent === ReadingEventType.NONE) {
    userbook.value.lastReadingEvent = null
  }
  userbook.value.book.image = null
  if (StringUtils.isNotBlank(imageUrl.value)) {
    userbook.value.book.image = imageUrl.value
  }
  
      console.log(`push book ` + userbook.value);
      console.log(userbook.value);
      let promise: Promise<UserBook>
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
      // dataService.updateUserBookImage(
      //   userbook.value,
      //   file.value,
      //   (event: { loaded: number; total: number }) => {
      //     let percent = Math.round((100 * event.loaded) / event.total);
      //     console.log("percent " + percent);
      //     uploadPercentage.value = percent;
      //   }
      // )
      promise
      .then(res => 
        {
          console.log(`update book ${res.book.title}`);
          ObjectUtils.toast(oruga.oruga, "success", `Book ${res.book.title} updated !`, 4000);
          emit('close')
      })
    .catch (err => {
      ObjectUtils.toast(oruga.oruga, "danger", `Error ` + err.message, 4000);
    })

}

function getFilteredAuthors(text: string) {
  console.log("option " + text)
  dataService.findAuthorByCriteria(text).then((data) => filteredAuthors.value = data)
}

function getFilteredTags(text: string) {
  dataService.findTagsByCriteria(text).then((data) => filteredTags.value = data)
}

const clearDatePicker = () => {
  // close datepicker on reset
    userbook.value.book.publishedDate = null;
};

const format = (dateString: string) => {
  return DateUtils.formatDate(dateString)
}

function itemAdded() {
  console.log("added")
  console.log(userbook.value.book.authors)
}

function beforeAdd(item: Author|string) {
  let shouldAdd = true
  if (item instanceof Object) {
    userbook.value.book?.authors?.forEach(author => {
      console.log(`author ${author.name}`)
      if(author.name === item.name) {
        console.log(`author ${author.name} item ${item.name}`)
        shouldAdd = false;
      }
    });
  }
  else {
    userbook.value.book?.authors?.forEach(author => {
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
    userbook.value.book?.tags?.forEach(tag => {
      console.log(`tag ${tag.name}`)
      if(tag.name === item.name) {
        console.log(`tag ${tag.name} item ${item.name}`)
        shouldAdd = false;
      }
    });
  }
  else {
    userbook.value.book?.tags?.forEach(tag => {
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

function toggleRemoveImage() {
  deleteImage.value = !deleteImage.value
}

</script>

<template>
<section class="edit-modal">
    <div class="columns">
    <div class="column is-centered is-full">
<div class="field">
      <o-field horizontal label="Title">
        <o-input v-model="userbook.book.title"></o-input>
      </o-field>
    </div>

    <div class="field">
      <o-field horizontal label="Authors">
        <o-inputitems
          v-model="userbook.book.authors"
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
          @add="itemAdded"
        >
        </o-inputitems>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="Tags">
        <o-inputitems
          v-model="userbook.book.tags"
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
          v-model="userbook.book.summary"
        ></o-input>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="ISBN">
        <o-input
          name="isbn10"
          v-model="userbook.book.isbn10"
          placeholder="isbn10"
        ></o-input>
        <o-input
          name="isbn13"
          v-model="userbook.book.isbn13"
          placeholder="isbn13"
        ></o-input>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="Identifiers">
        <o-input
          name="goodreadsId"
          v-model="userbook.book.goodreadsId"
          placeholder="goodreadsId"
        ></o-input>
        <o-input
          name="googleId"
          v-model="userbook.book.googleId"
          placeholder="googleId"
        ></o-input>
        <o-input
          name="amazonId"
          v-model="userbook.book.amazonId"
          placeholder="amazonId"
        ></o-input>
        <o-input
          name="librarythingId"
          v-model="userbook.book.librarythingId"
          placeholder="librarythingId"
        ></o-input>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="Publisher">
        <o-input v-model="userbook.book.publisher"></o-input>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="Published date">
        <o-datepicker
          v-model="publishedDate"
          v-show="true"
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
        <o-input v-model="userbook.book.pageCount" type="number" min="0"></o-input>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="Series">
        <o-input v-model="userbook.book.series"></o-input>
        <o-input v-model="userbook.book.numberInSeries" type="number" min="0" step="0.1"></o-input>
      </o-field>
    </div>
    <div class="block">
      <o-field horizontal label="Status : ">
        <o-radio v-model="userbook.lastReadingEvent" name="type" native-value="FINISHED">
          Finished
        </o-radio>
        <o-radio
          v-model="userbook.lastReadingEvent"
          name="type"
          native-value="CURRENTLY_READING"
        >
          Currently reading
        </o-radio>
        <o-radio v-model="userbook.lastReadingEvent" name="type" native-value="DROPPED">
          Dropped
        </o-radio>
        <o-radio v-model="userbook.lastReadingEvent" name="type" native-value="NONE">
          None
        </o-radio>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="Personal notes">
        <o-input
          maxlength="200"
          type="textarea"
          v-model="userbook.personalNotes"
        ></o-input>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="Owned">
      <o-checkbox v-model="userbook.owned">
        {{ ownedDisplay }}
      </o-checkbox>
      </o-field>
    </div>
    <div class="field">
      <o-field horizontal label="To read ?">
      <o-checkbox v-model="userbook.toRead">
        {{ toReadDisplay }}
      </o-checkbox>
      </o-field>
    </div>
<div v-if="hasImage">
  <o-field horizontal>
    <template v-slot:label>
        Actual cover :
        <o-tooltip v-if="!deleteImage" label="Click bin to remove current cover" position="right">
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
            :src="'/files/' + userbook.book.image"
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
        <button @click="importBook" class="button is-primary centered-button">
          Save changes
        </button>
      <p v-if="errorMessage" class="has-text-danger">{{ errorMessage }}</p>
    </div>
    </div>
  </section>

</template>

<style lang="scss">
@import "../assets/style.scss";

// .edit-modal {
//     position: relative;
//     overflow-y: scroll;
//     background-color: $jelu_background_accent;
//     padding: 20px;
//     max-height: calc(100vh - 40px);
// }

// .small-cover {
//   width: 100px;
// }

// .small-cover img {
//   max-height: 100%;
//   max-width: 100%;
//   position: relative;
// }

// .overlay-button {
//   position: absolute;
//   // background: white;
//   bottom: 5%;
//   left: 2%;
// }

// .altered {
//   filter: blur(4px) invert(38%);
//   -webkit-filter: blur(4px) invert(38%);
//   -moz-filter: blur(4px) invert(38%);
// }

// find another way to center the submit button (try like AutoImportFormModal)
.centered-button {
  position: relative;
  left: 40%;
}

</style>
