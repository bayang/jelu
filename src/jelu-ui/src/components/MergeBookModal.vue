<script setup lang="ts">
import { Ref, ref } from "vue";
import { useI18n } from 'vue-i18n';
import { Author } from "../model/Author";
import { Book } from "../model/Book";
import { Metadata } from "../model/Metadata";
import { Tag } from "../model/Tag";
import dataService from "../services/DataService";
import { ObjectUtils } from "../utils/ObjectUtils";
import { SeriesOrder } from "../model/Series";
import SeriesInput from "./SeriesInput.vue";

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

let filteredAuthors: Ref<Array<Author>> = ref([]);
let filteredTags: Ref<Array<Tag>> = ref([]);

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

function getFilteredAuthors(text: string) {
  console.log("option " + text)
  dataService.findAuthorByCriteria(text).then((data) => filteredAuthors.value = data.content)
}

function getFilteredTags(text: string) {
  dataService.findTagsByCriteria(text).then((data) => filteredTags.value = data.content)
}

const listAsString = (list: Array<Author|Tag>|undefined) => {
  let res = ''
  if (list != null && list.length > 0) {
    let first = true
    for (let obj of list) {
      if (first) {
        first = false
      } else {
        res += ', '
      }
      res += obj.name
    }
  }
  return res
}

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
        <div class="form-control w-full max-w-xs">
          <label class="label">
            <span class="label-text">{{ t('book.title') }}</span>
          </label>
          <input
            v-model="book.title"
            type="text"
            class="input input-bordered input-primary w-full max-w-xs"
          >
        </div>
        <div class="form-control w-full max-w-xs">
          <div class="join">
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
              class="jelu-cursor-text input input-bordered input-secondary w-full max-w-xs join-item"
            >
          </div>
        </div>
        <div class="form-control w-full max-w-xs">
          <label class="label">
            <span class="label-text">{{ t('book.author', 2) }}</span>
          </label>
          <o-taginput
            v-model="book.authors"
            :data="filteredAuthors"
            :allow-autocomplete="true"
            :autocomplete="true"
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
        </div>
        <div class="form-control w-full max-w-xs">
          <div class="flex">
            <div class="m-2">
              {{ listAsString(book.authors) }}
            </div>
          </div>
        </div>
        <div class="form-control w-full max-w-xs">
          <label class="label">
            <span class="label-text">{{ t('book.tag', 2) }}</span>
          </label>
          <o-taginput
            v-model="book.tags"
            :data="filteredTags"
            :allow-autocomplete="true"
            :autocomplete="true"
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
        </div>
        <div class="form-control w-full max-w-xs">
          <div class="flex">
            <div class="m-2">
              {{ listAsString(book.tags) }}
            </div>
          </div>
        </div>
        <div class="form-control w-full max-w-xs">
          <label class="label">
            <span class="label-text">{{ t('book.isbn10') }}</span>
          </label>
          <input
            v-model="book.isbn10"
            type="text"
            class="input input-bordered input-primary w-full max-w-xs"
          >
        </div>
        <div class="form-control w-full max-w-xs">
          <div class="join">
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
              class="jelu-cursor-text input input-bordered input-secondary w-full max-w-xs join-item"
            >
          </div>
        </div>
        <div class="form-control w-full max-w-xs">
          <label class="label">
            <span class="label-text">{{ t('book.isbn13') }}</span>
          </label>
          <input
            v-model="book.isbn13"
            type="text"
            class="input input-bordered input-primary w-full max-w-xs"
          >
        </div>
        <div class="form-control w-full max-w-xs">
          <div class="join">
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
              class="jelu-cursor-text input input-bordered input-secondary w-full max-w-xs join-item"
            >
          </div>
        </div>
        <div class="form-control w-full max-w-xs">
          <label class="label">
            <span class="label-text">{{ t('book.publisher') }}</span>
          </label>
          <input
            v-model="book.publisher"
            type="text"
            class="input input-bordered input-primary w-full max-w-xs"
          >
        </div>
        <div class="form-control w-full max-w-xs">
          <div class="join">
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
              class="jelu-cursor-text input input-bordered input-secondary w-full max-w-xs join-item"
            >
          </div>
        </div>
        <div class="form-control w-full max-w-xs">
          <label class="label">
            <span class="label-text">{{ t('book.page_count') }}</span>
          </label>
          <input
            v-model="book.pageCount"
            type="number"
            class="input input-bordered input-primary w-full max-w-xs"
          >
        </div>
        <div class="form-control w-full max-w-xs">
          <div class="join">
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
              class="jelu-cursor-text input input-bordered input-secondary w-full max-w-xs join-item"
            >
          </div>
        </div>
        <div class="form-control w-full max-w-xs">
          <label class="label">
            <span class="label-text">{{ t('book.published_date') }}</span>
          </label>
          <input
            v-model="book.publishedDate"
            type="text"
            class="input input-bordered input-primary w-full max-w-xs"
          >
        </div>
        <div class="form-control w-full max-w-xs">
          <div class="join">
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
              class="jelu-cursor-text input input-bordered input-secondary w-full max-w-xs join-item"
            >
          </div>
        </div>
        <div class="form-control w-full max-w-xs">
          <label class="label">
            <span class="label-text">{{ t('book.series') }}</span>
          </label>
          <div
            v-for="seriesItem,idx in seriesCopy"
            :key="seriesItem.seriesId"
            class="flex flex-col sm:flex-row items-center grow w-full pb-2"
          >
            <SeriesInput
              class="jl-formkit"
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
          <div class="field pb-2">
            <button
              class="btn btn-primary btn-circle p-2 btn-sm"
              @click="seriesCopy.push({'name' : ''})"
            >
              <span class="icon">
                <i class="mdi mdi-plus mdi-18px" />
              </span>
            </button>
          </div>
        </div>
        <div class="form-control w-full max-w-xs">
          <div class="flex">
            <div class="m-2">
              {{ props.metadata.series }} <span v-if="props.metadata.numberInSeries">#{{ props.metadata.numberInSeries }}</span>
            </div>
          </div>
        </div>
        <div class="form-control w-full max-w-xs">
          <label class="label">
            <span class="label-text">{{ t('book.language') }}</span>
          </label>
          <input
            v-model="book.language"
            type="text"
            class="input input-bordered input-primary w-full max-w-xs"
          >
        </div>
        <div class="form-control w-full max-w-xs">
          <div class="join">
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
              class="jelu-cursor-text input input-bordered input-secondary w-full max-w-xs join-item"
            >
          </div>
        </div>
        <div class="form-control w-full max-w-xs">
          <label class="label">
            <span class="label-text">{{ t('book.google_id') }}</span>
          </label>
          <input
            v-model="book.googleId"
            type="text"
            class="jelu-cursor-text input input-bordered input-primary w-full max-w-xs"
          >
        </div>
        <div class="form-control w-full max-w-xs">
          <div class="join">
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
              class="jelu-cursor-text input input-bordered input-secondary w-full max-w-xs join-item"
            >
          </div>
        </div>
        <div class="form-control w-full max-w-xs">
          <label class="label">
            <span class="label-text">{{ t('book.goodreads_id') }}</span>
          </label>
          <input
            v-model="book.goodreadsId"
            type="text"
            class="input input-bordered input-primary w-full max-w-xs"
          >
        </div>
        <div class="form-control w-full max-w-xs">
          <div class="join">
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
              class="jelu-cursor-text input input-bordered input-secondary w-full max-w-xs join-item"
            >
          </div>
        </div>
        <div class="form-control w-full max-w-xs">
          <label class="label">
            <span class="label-text">{{ t('book.amazon_id') }}</span>
          </label>
          <input
            v-model="book.amazonId"
            type="text"
            class="input input-bordered input-primary w-full max-w-xs"
          >
        </div>
        <div class="form-control w-full max-w-xs">
          <div class="join">
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
              class="jelu-cursor-text input input-bordered input-secondary w-full max-w-xs join-item"
            >
          </div>
        </div>
        <div class="form-control w-full max-w-xs">
          <label class="label">
            <span class="label-text">{{ t('book.summary') }}</span>
          </label>
          <textarea
            v-model="book.summary"
            class="textarea textarea-bordered textarea-primary w-full max-w-xs"
          />
        </div>
        <div class="form-control w-full max-w-xs">
          <div class="join">
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
              class="jelu-cursor-text textarea textarea-bordered textarea-secondary w-full max-w-xs join-item"
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
