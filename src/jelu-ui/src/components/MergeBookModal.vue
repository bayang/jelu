<script setup lang="ts">
import { Ref, ref } from "vue";
import { useI18n } from 'vue-i18n';
import { Author } from "../model/Author";
import { Wrapper } from "../model/autocomplete-wrapper";
import { Book } from "../model/Book";
import { Metadata } from "../model/Metadata";
import { SeriesOrder } from "../model/Series";
import { Tag } from "../model/Tag";
import dataService from "../services/DataService";
import { ObjectUtils } from "../utils/ObjectUtils";
import SeriesCompleteInput from "./SeriesCompleteInput.vue";
import { Role } from "../model/Role";

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const props = defineProps<{
  book: Book,
  metadata: Metadata
}>()

const book: Ref<Book> = ref(props.book)
const initialIsbn10: Ref<string|undefined> = ref(book.value.isbn10)
const initialIsbn13: Ref<string|undefined> = ref(book.value.isbn13)
const seriesCopy: Array<SeriesOrder> = book.value.series ?? []

const emit = defineEmits<{
  (e: 'close'): void
}>()

let authors: Ref<Array<string|Author>> = ref([]);
let tags: Ref<Array<string|Tag>> = ref([])

let filteredAuthors: Ref<Array<Wrapper>> = ref([]);
let filteredTags: Ref<Array<Wrapper>> = ref([]);

const progress: Ref<boolean> = ref(false)

const discard = () => {
  emit('close')
}

const isbnHasChanged = () => {
  return (book.value.isbn10 != undefined && book.value.isbn10 !== initialIsbn10.value) ||
  (book.value.isbn13 != undefined && book.value.isbn13 !== initialIsbn13.value)
}

const importData = async () => {
  if (book.value.id != null) {
    progress.value = true
    let saveBook = true
    // only check if isbn has changed, otherwise we could
    // only be editing an already existing book
    // with an isbn already set. And we don't want to warn in that case
    if (isbnHasChanged()) {
      let alreadyExisting = await dataService.checkIsbnExists(book.value.isbn10, book.value.isbn13)
      console.log('already existing')
      console.log(alreadyExisting)
      if (alreadyExisting != null) {
        saveBook = false
        await ObjectUtils.swalYesNoMixin.fire({
          html: `<p>${t('labels.book_with_same_isbn_already_exists')}:<br>${alreadyExisting.title}<br>${t('labels.save_new_anyway')}</p>`,
          showDenyButton: false,
          confirmButtonText: t('labels.save'),
        }).then((result) => {
          if (result.isConfirmed) {
            saveBook = true
          } else if (result.isDenied) {
            ObjectUtils.baseSwalMixin.fire('', t('labels.changes_not_saved'), 'info')
          }
        })
      }
    }
    console.log(`save book ${saveBook}`)
    if (!saveBook) {
      progress.value = false
      return
    }
    if (seriesCopy.length > 0) {
      if (book.value.series == null) {
        book.value.series = []
      }
      seriesCopy.forEach(s => {
        if (s.name.trim().length > 0){
          book.value.series?.push(s)
        }
      })
    }
    dataService.updateBook(book.value.id, {...book.value})
    .then(res => {
          progress.value = false
          emit('close')
        })
        .catch(err => {
          progress.value = false
          console.log(err)
        })
  }
}

function beforeAdd(item: Author | string) {
  let shouldAdd = true
  if (item instanceof Object) {
    book.value.authors?.forEach(author => {
      console.log(`author ${author.name}`)
      if (author.name === item.name) {
        console.log(`author ${author.name} item ${item.name}`)
        shouldAdd = false;
      }
    });
  }
  else {
    book.value.authors?.forEach(author => {
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
    book.value.tags?.forEach(tag => {
      console.log(`tag ${tag.name}`)
      if (tag.name === item.name) {
        console.log(`tag ${tag.name} item ${item.name}`)
        shouldAdd = false;
      }
    });
  }
  else {
    book.value.tags?.forEach(tag => {
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
    return item
}

function createTag(item: Tag | string) {
    return item
}

function getFilteredAuthors(text: string) {
  console.log("option " + text)
  dataService.findAuthorByCriteria(Role.ANY, text).then((data) => {
    filteredAuthors.value.splice(filteredAuthors.value.length)
    data.content.forEach(a => filteredAuthors.value.push(ObjectUtils.wrapForOptions(a)))
  })
}

function getFilteredTags(text: string) {
  dataService.findTagsByCriteria(text).then((data) => data.content.forEach(t => filteredTags.value.push(ObjectUtils.wrapForOptions(t))))
}

function authorAdded(item: string|Author) {
  itemAdded(item, book.value.authors as Array<Author>)
}

function tagAdded(item: string|Tag) {
  itemAdded(item, book.value.tags as Array<Tag>)
}

function itemAdded(item: string|Author|Tag, target: Array<Author|Tag>) {
  console.log("added")
  console.log(item)
  console.log(book.value.authors)
  console.log(authors.value)
    if (typeof item === 'string') {
      console.log("item is string : " + item)
      target.push({"name": item})
    } else {
      console.log("item is author")
      console.log(item)
      target.push(item)
    }
  console.log(book.value.authors)
}

function authorRemoved(item: string|Author) {
  console.log("removed")
  console.log(item)
  if (typeof item === 'string') {
    console.log("type string")
    const toKeep = book.value.authors?.filter(a => a.name !== item)
    book.value.authors = toKeep
  } else {
    console.log('type author')
    const toKeep = book.value.authors?.filter(a => a.id !== item.id)
    book.value.authors = toKeep
  }
  console.log(book.value.authors)
}

function tagRemoved(item: string|Tag) {
  console.log("removed")
  console.log(item)
  if (typeof item === 'string') {
    console.log("type string")
    const toKeep = book.value.tags?.filter(a => a.name !== item)
    book.value.tags = toKeep
  } else {
    console.log('type tag')
    const toKeep = book.value.tags?.filter(a => a.id !== item.id)
    book.value.tags = toKeep
  }
  console.log(book.value.tags)
}

book.value.authors?.forEach(a => authors.value.push(a.name))
console.log("authors")
console.log(authors.value)
book.value.tags?.forEach(t => tags.value.push(t.name))
</script>

<template>
  <section class="review-modal">
    <div class="w-full">
      <div>
        <h1
          class="typewriter text-2xl first-letter:capitalize"
        >
          {{ t('book_merge.merge_books') }}
        </h1>
      </div>
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 items-end">
        <div class="form-control w-full">
          <label class="label">
            <span class="label-text first-letter:capitalize">{{ t('book.title') }}</span>
          </label>
          <input
            v-model="book.title"
            type="text"
            class="input input-primary w-full"
          >
        </div>
        <div class="form-control w-full">
          <div class="join w-full">
            <button
              class="btn btn-square btn-ghost btn-outline btn-secondary join-item z-0"
              @click="book.title = props.metadata.title != null ? props.metadata.title : ''"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                class="w-6 h-6"
              >
                <path
                  fill-rule="evenodd"
                  d="M11.03 3.97a.75.75 0 010 1.06l-6.22 6.22H21a.75.75 0 010 1.5H4.81l6.22 6.22a.75.75 0 11-1.06 1.06l-7.5-7.5a.75.75 0 010-1.06l7.5-7.5a.75.75 0 011.06 0z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <input
              type="text"
              :value="props.metadata.title"
              disabled
              class="jelu-cursor-text input input-secondary w-full join-item"
            >
          </div>
        </div>
        <div class="w-full jelu-authorinput">
          <label class="label">
            <span class="label-text first-letter:capitalize">{{ t('book.author', 2) }}</span>
          </label>
          <o-taginput
            v-model="authors"
            :options="filteredAuthors"
            :allow-autocomplete="true"
            autocomplete="off"
            :allow-new="true"
            :allow-duplicates="false"
            :open-on-focus="true"
            :validate-item="beforeAdd"
            :create-item="createAuthor"
            icon-pack="mdi"
            icon="account-plus"
            :placeholder="t('labels.add_author')"
            @input="getFilteredAuthors"
            @add="authorAdded"
            @remove="authorRemoved"
          >
            <template #default="{ value }">
              <div class="jl-taginput-item">
                {{ value.name }}
              </div>
            </template>
          </o-taginput>
        </div>
        <div class="form-control w-full">
          <div class="flex">
            <div class="m-2">
              {{ metadata.authors.join(',') }}
            </div>
          </div>
        </div>
        <div class="w-full jelu-taginput">
          <label class="label">
            <span class="label-text first-letter:capitalize">{{ t('book.tag', 2) }}</span>
          </label>
          <o-taginput
            v-model="tags"
            :options="filteredTags"
            :allow-autocomplete="true"
            autocomplete="off"
            :allow-new="true"
            :allow-duplicates="false"
            :open-on-focus="true"
            :validate-item="beforeAddTag"
            :create-item="createTag"
            icon-pack="mdi"
            icon="tag-plus"
            :placeholder="t('labels.add_tag')"
            @input="getFilteredTags"
            @add="tagAdded"
            @remove="tagRemoved"
          >
            <template #default="{ value }">
              <div class="jl-taginput-item">
                {{ value.name }}
              </div>
            </template>
          </o-taginput>
        </div>
        <div class="form-control w-full">
          <div class="flex">
            <div class="m-2">
              {{ metadata.tags.join(',') }}
            </div>
          </div>
        </div>
        <div class="form-control w-full">
          <label class="label">
            <span class="label-text first-letter:capitalize">{{ t('book.isbn10') }}</span>
          </label>
          <input
            v-model="book.isbn10"
            type="text"
            class="input input-primary w-full"
          >
        </div>
        <div class="form-control w-full">
          <div class="join w-full">
            <button
              class="btn btn-square btn-ghost btn-outline btn-secondary join-item z-0"
              @click="book.isbn10 = props.metadata.isbn10"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                class="w-6 h-6"
              >
                <path
                  fill-rule="evenodd"
                  d="M11.03 3.97a.75.75 0 010 1.06l-6.22 6.22H21a.75.75 0 010 1.5H4.81l6.22 6.22a.75.75 0 11-1.06 1.06l-7.5-7.5a.75.75 0 010-1.06l7.5-7.5a.75.75 0 011.06 0z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <input
              type="text"
              :value="props.metadata.isbn10"
              disabled
              class="jelu-cursor-text input input-secondary w-full join-item"
            >
          </div>
        </div>
        <div class="form-control w-full">
          <label class="label">
            <span class="label-text first-letter:capitalize">{{ t('book.isbn13') }}</span>
          </label>
          <input
            v-model="book.isbn13"
            type="text"
            class="input input-primary w-full"
          >
        </div>
        <div class="form-control w-full">
          <div class="join w-full">
            <button
              class="btn btn-square btn-ghost btn-outline btn-secondary join-item z-0"
              @click="book.isbn13 = props.metadata.isbn13"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                class="w-6 h-6"
              >
                <path
                  fill-rule="evenodd"
                  d="M11.03 3.97a.75.75 0 010 1.06l-6.22 6.22H21a.75.75 0 010 1.5H4.81l6.22 6.22a.75.75 0 11-1.06 1.06l-7.5-7.5a.75.75 0 010-1.06l7.5-7.5a.75.75 0 011.06 0z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <input
              type="text"
              :value="props.metadata.isbn13"
              disabled
              class="jelu-cursor-text input input-secondary w-full join-item"
            >
          </div>
        </div>
        <div class="form-control w-full">
          <label class="label">
            <span class="label-text first-letter:capitalize">{{ t('book.publisher') }}</span>
          </label>
          <input
            v-model="book.publisher"
            type="text"
            class="input input-primary w-full"
          >
        </div>
        <div class="form-control w-full">
          <div class="join w-full">
            <button
              class="btn btn-square btn-ghost btn-outline btn-secondary join-item z-0"
              @click="book.publisher = props.metadata.publisher"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                class="w-6 h-6"
              >
                <path
                  fill-rule="evenodd"
                  d="M11.03 3.97a.75.75 0 010 1.06l-6.22 6.22H21a.75.75 0 010 1.5H4.81l6.22 6.22a.75.75 0 11-1.06 1.06l-7.5-7.5a.75.75 0 010-1.06l7.5-7.5a.75.75 0 011.06 0z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <input
              type="text"
              :value="props.metadata.publisher"
              disabled
              class="jelu-cursor-text input input-secondary w-full join-item"
            >
          </div>
        </div>
        <div class="form-control w-full">
          <label class="label">
            <span class="label-text first-letter:capitalize">{{ t('book.page_count') }}</span>
          </label>
          <input
            v-model="book.pageCount"
            type="number"
            class="input input-primary w-full"
          >
        </div>
        <div class="form-control w-full">
          <div class="join w-full">
            <button
              class="btn btn-square btn-ghost btn-outline btn-secondary join-item z-0"
              @click="book.pageCount = props.metadata.pageCount"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                class="w-6 h-6"
              >
                <path
                  fill-rule="evenodd"
                  d="M11.03 3.97a.75.75 0 010 1.06l-6.22 6.22H21a.75.75 0 010 1.5H4.81l6.22 6.22a.75.75 0 11-1.06 1.06l-7.5-7.5a.75.75 0 010-1.06l7.5-7.5a.75.75 0 011.06 0z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <input
              type="text"
              :value="props.metadata.pageCount"
              disabled
              class="jelu-cursor-text input input-secondary w-full join-item"
            >
          </div>
        </div>
        <div class="form-control w-full">
          <label class="label">
            <span class="label-text first-letter:capitalize">{{ t('book.published_date') }}</span>
          </label>
          <input
            v-model="book.publishedDate"
            type="text"
            class="input input-primary w-full"
          >
        </div>
        <div class="form-control w-full">
          <div class="join w-full">
            <button
              class="btn btn-square btn-ghost btn-outline btn-secondary join-item z-0"
              @click="book.publishedDate = props.metadata.publishedDate"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                class="w-6 h-6"
              >
                <path
                  fill-rule="evenodd"
                  d="M11.03 3.97a.75.75 0 010 1.06l-6.22 6.22H21a.75.75 0 010 1.5H4.81l6.22 6.22a.75.75 0 11-1.06 1.06l-7.5-7.5a.75.75 0 010-1.06l7.5-7.5a.75.75 0 011.06 0z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <input
              type="text"
              :value="props.metadata.publishedDate"
              disabled
              class="jelu-cursor-text input  input-secondary w-full join-item"
            >
          </div>
        </div>
        <div class="form-control w-full">
          <label class="label">
            <span class="label-text first-letter:capitalize">{{ t('book.series') }}</span>
          </label>
          <div>
            <SeriesCompleteInput v-model="seriesCopy" />
          </div>
        </div>
        <div class="form-control w-full">
          <div class="flex">
            <div class="m-2">
              {{ props.metadata.series }} <span v-if="props.metadata.numberInSeries">#{{ props.metadata.numberInSeries }}</span>
            </div>
          </div>
        </div>
        <div class="form-control w-full">
          <label class="label">
            <span class="label-text first-letter:capitalize">{{ t('book.language') }}</span>
          </label>
          <input
            v-model="book.language"
            type="text"
            class="input  input-primary w-full"
          >
        </div>
        <div class="form-control w-full">
          <div class="join w-full">
            <button
              class="btn btn-square btn-ghost btn-outline btn-secondary join-item z-0"
              @click="book.language = props.metadata.language"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                class="w-6 h-6"
              >
                <path
                  fill-rule="evenodd"
                  d="M11.03 3.97a.75.75 0 010 1.06l-6.22 6.22H21a.75.75 0 010 1.5H4.81l6.22 6.22a.75.75 0 11-1.06 1.06l-7.5-7.5a.75.75 0 010-1.06l7.5-7.5a.75.75 0 011.06 0z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <input
              type="text"
              :value="props.metadata.language"
              disabled
              class="jelu-cursor-text input  input-secondary w-full join-item"
            >
          </div>
        </div>
        <div class="form-control w-full">
          <label class="label">
            <span class="label-text">{{ t('book.google_id') }}</span>
          </label>
          <input
            v-model="book.googleId"
            type="text"
            class="jelu-cursor-text input  input-primary w-full"
          >
        </div>
        <div class="form-control w-full">
          <div class="join w-full">
            <button
              class="btn btn-square btn-ghost btn-outline btn-secondary join-item z-0"
              @click="book.googleId = props.metadata.googleId"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                class="w-6 h-6"
              >
                <path
                  fill-rule="evenodd"
                  d="M11.03 3.97a.75.75 0 010 1.06l-6.22 6.22H21a.75.75 0 010 1.5H4.81l6.22 6.22a.75.75 0 11-1.06 1.06l-7.5-7.5a.75.75 0 010-1.06l7.5-7.5a.75.75 0 011.06 0z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <input
              type="text"
              :value="props.metadata.googleId"
              disabled
              class="jelu-cursor-text input  input-secondary w-full join-item"
            >
          </div>
        </div>
        <div class="form-control w-full">
          <label class="label">
            <span class="label-text">{{ t('book.goodreads_id') }}</span>
          </label>
          <input
            v-model="book.goodreadsId"
            type="text"
            class="input  input-primary w-full"
          >
        </div>
        <div class="form-control w-full">
          <div class="join w-full">
            <button
              class="btn btn-square btn-ghost btn-outline btn-secondary join-item z-0"
              @click="book.goodreadsId = props.metadata.goodreadsId"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                class="w-6 h-6"
              >
                <path
                  fill-rule="evenodd"
                  d="M11.03 3.97a.75.75 0 010 1.06l-6.22 6.22H21a.75.75 0 010 1.5H4.81l6.22 6.22a.75.75 0 11-1.06 1.06l-7.5-7.5a.75.75 0 010-1.06l7.5-7.5a.75.75 0 011.06 0z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <input
              type="text"
              :value="props.metadata.goodreadsId"
              disabled
              class="jelu-cursor-text input  input-secondary w-full join-item"
            >
          </div>
        </div>
        <div class="form-control w-full">
          <label class="label">
            <span class="label-text">{{ t('book.amazon_id') }}</span>
          </label>
          <input
            v-model="book.amazonId"
            type="text"
            class="input  input-primary w-full"
          >
        </div>
        <div class="form-control w-full">
          <div class="join w-full">
            <button
              class="btn btn-square btn-ghost btn-outline btn-secondary join-item z-0"
              @click="book.amazonId = props.metadata.amazonId"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                class="w-6 h-6"
              >
                <path
                  fill-rule="evenodd"
                  d="M11.03 3.97a.75.75 0 010 1.06l-6.22 6.22H21a.75.75 0 010 1.5H4.81l6.22 6.22a.75.75 0 11-1.06 1.06l-7.5-7.5a.75.75 0 010-1.06l7.5-7.5a.75.75 0 011.06 0z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <input
              type="text"
              :value="props.metadata.amazonId"
              disabled
              class="jelu-cursor-text input  input-secondary w-full join-item"
            >
          </div>
        </div>

        <div class="form-control w-full">
          <label class="label">
            <span class="label-text">{{ t('book.librarything_id') }}</span>
          </label>
          <input
            v-model="book.librarythingId"
            type="text"
            class="input  input-primary w-full"
          >
        </div>
        <div class="form-control w-full">
          <div class="join w-full">
            <button
              class="btn btn-square btn-ghost btn-outline btn-secondary join-item z-0"
              @click="book.librarythingId = props.metadata.librarythingId"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                class="w-6 h-6"
              >
                <path
                  fill-rule="evenodd"
                  d="M11.03 3.97a.75.75 0 010 1.06l-6.22 6.22H21a.75.75 0 010 1.5H4.81l6.22 6.22a.75.75 0 11-1.06 1.06l-7.5-7.5a.75.75 0 010-1.06l7.5-7.5a.75.75 0 011.06 0z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <input
              type="text"
              :value="props.metadata.librarythingId"
              disabled
              class="jelu-cursor-text input  input-secondary w-full join-item"
            >
          </div>
        </div>

        <div class="form-control w-full">
          <label class="label">
            <span class="label-text">{{ t('book.isfdb_id') }}</span>
          </label>
          <input
            v-model="book.isfdbId"
            type="text"
            class="input  input-primary w-full"
          >
        </div>
        <div class="form-control w-full">
          <div class="join w-full">
            <button
              class="btn btn-square btn-ghost btn-outline btn-secondary join-item z-0"
              @click="book.isfdbId = props.metadata.isfdbId"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                class="w-6 h-6"
              >
                <path
                  fill-rule="evenodd"
                  d="M11.03 3.97a.75.75 0 010 1.06l-6.22 6.22H21a.75.75 0 010 1.5H4.81l6.22 6.22a.75.75 0 11-1.06 1.06l-7.5-7.5a.75.75 0 010-1.06l7.5-7.5a.75.75 0 011.06 0z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <input
              type="text"
              :value="props.metadata.isfdbId"
              disabled
              class="jelu-cursor-text input  input-secondary w-full join-item"
            >
          </div>
        </div>

        <div class="form-control w-full">
          <label class="label">
            <span class="label-text">{{ t('book.openlibrary_id') }}</span>
          </label>
          <input
            v-model="book.openlibraryId"
            type="text"
            class="input  input-primary w-full"
          >
        </div>
        <div class="form-control w-full">
          <div class="join w-full">
            <button
              class="btn btn-square btn-ghost btn-outline btn-secondary join-item z-0"
              @click="book.openlibraryId = props.metadata.openlibraryId"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                class="w-6 h-6"
              >
                <path
                  fill-rule="evenodd"
                  d="M11.03 3.97a.75.75 0 010 1.06l-6.22 6.22H21a.75.75 0 010 1.5H4.81l6.22 6.22a.75.75 0 11-1.06 1.06l-7.5-7.5a.75.75 0 010-1.06l7.5-7.5a.75.75 0 011.06 0z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <input
              type="text"
              :value="props.metadata.openlibraryId"
              disabled
              class="jelu-cursor-text input  input-secondary w-full join-item"
            >
          </div>
        </div>

        <div class="form-control w-full">
          <label class="label">
            <span class="label-text">{{ t('book.noosfere_id') }}</span>
          </label>
          <input
            v-model="book.noosfereId"
            type="text"
            class="input  input-primary w-full"
          >
        </div>
        <div class="form-control w-full">
          <div class="join w-full">
            <button
              class="btn btn-square btn-ghost btn-outline btn-secondary join-item z-0"
              @click="book.noosfereId = props.metadata.noosfereId"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                class="w-6 h-6"
              >
                <path
                  fill-rule="evenodd"
                  d="M11.03 3.97a.75.75 0 010 1.06l-6.22 6.22H21a.75.75 0 010 1.5H4.81l6.22 6.22a.75.75 0 11-1.06 1.06l-7.5-7.5a.75.75 0 010-1.06l7.5-7.5a.75.75 0 011.06 0z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <input
              type="text"
              :value="props.metadata.noosfereId"
              disabled
              class="jelu-cursor-text input  input-secondary w-full join-item"
            >
          </div>
        </div>

        <div class="form-control w-full">
          <label class="label">
            <span class="label-text">{{ t('book.inventaire_id') }}</span>
          </label>
          <input
            v-model="book.inventaireId"
            type="text"
            class="input  input-primary w-full"
          >
        </div>
        <div class="form-control w-full">
          <div class="join w-full">
            <button
              class="btn btn-square btn-ghost btn-outline btn-secondary join-item z-0"
              @click="book.inventaireId = props.metadata.inventaireId"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                class="w-6 h-6"
              >
                <path
                  fill-rule="evenodd"
                  d="M11.03 3.97a.75.75 0 010 1.06l-6.22 6.22H21a.75.75 0 010 1.5H4.81l6.22 6.22a.75.75 0 11-1.06 1.06l-7.5-7.5a.75.75 0 010-1.06l7.5-7.5a.75.75 0 011.06 0z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <input
              type="text"
              :value="props.metadata.inventaireId"
              disabled
              class="jelu-cursor-text input  input-secondary w-full join-item"
            >
          </div>
        </div>
        <div class="form-control w-full">
          <label class="label">
            <span class="label-text first-letter:capitalize">{{ t('book.summary') }}</span>
          </label>
          <textarea
            v-model="book.summary"
            class="textarea textarea-primary w-full"
          />
        </div>
        <div class="form-control w-full">
          <div class="join w-full">
            <button
              class="btn btn-square btn-ghost btn-outline btn-secondary join-item z-0"
              @click="book.summary = props.metadata.summary"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="currentColor"
                class="w-6 h-6"
              >
                <path
                  fill-rule="evenodd"
                  d="M11.03 3.97a.75.75 0 010 1.06l-6.22 6.22H21a.75.75 0 010 1.5H4.81l6.22 6.22a.75.75 0 11-1.06 1.06l-7.5-7.5a.75.75 0 010-1.06l7.5-7.5a.75.75 0 011.06 0z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <textarea
              :value="props.metadata.summary"
              disabled
              class="jelu-cursor-text textarea textarea-secondary w-full join-item"
            />
          </div>
        </div>
      </div>
      <div
        class="flex space-x-5 mt-4 justify-center"
      >
        <button
          class="btn btn-primary uppercase"
          :disabled="progress"
          @click="importData"
        >
          <span
            v-if="progress"
            class="loading loading-spinner"
          />
          <span class="icon">
            <i class="mdi mdi-check mdi-18px" />
          </span><span>{{ t('labels.import') }}</span>
        </button>
        <button
          class="btn btn-warning uppercase"
          :disabled="progress"
          @click="discard"
        >
          <span
            v-if="progress"
            class="loading loading-spinner"
          />
          <span class="icon">
            <i class="mdi mdi-cancel mdi-18px" />
          </span><span>{{ t('labels.discard') }}</span>
        </button>
      </div>
    </div>
  </section>
</template>

<style>

</style>
