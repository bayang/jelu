<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { ref, Ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { CustomList } from '../model/custom-list'
import { Tag } from "../model/Tag"
import dataService from "../services/DataService"
import ClosableBadge from "./ClosableBadge.vue"

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | User page')

let filteredTags: Ref<Array<string>> = ref([]);
let customLists: Ref<Array<CustomList>> = ref([])
const isFetching = ref(false)

let currentListTags: Ref<Array<string>> = ref([])

watch(currentListTags, (oldVal, newVal) => {
  customList.value.tags = currentListTags.value.join()
})

const newList = (): CustomList => {
  return {
    name:"", tags: "", actionable: false, public: false
  }
}

let customList: Ref<CustomList> = ref(newList())

let editMode = ref(false)

function getFilteredTags(text: string) {
  isFetching.value = true
  dataService.findTagsByCriteria(text).then((data) => {
    filteredTags.value.splice(0, filteredTags.value.length)
    data.content.forEach(t => filteredTags.value.push(t.name))
  })
  isFetching.value = false
}

function getCustomLists() {
  isFetching.value = true
  dataService.findCustomLists(undefined, 0, 30, null).then(data => customLists.value = data.content)
  isFetching.value = false
}

function deleteList(list: CustomList) {
  dataService.deleteCustomList(list.id as string)
  .then(res => {
    console.log("deleted list " + list.id)
    getCustomLists()
    })
  .catch(err => console.log("failed to delete list " + list.id))
}

function createList() {
  dataService.saveCustomList(customList.value)
    .then(res => {
      resetList()
      getCustomLists()
    })
    .catch(err => {
      console.log("failed to create list")
      resetList()
    })
}

function beforeAddTag(item: Tag | string) {
  let shouldAdd = true
  if (item instanceof Object) {
    currentListTags.value.forEach(tag => {
      console.log(`tag ${tag}`)
      if (tag === item.name) {
        console.log(`tag ${tag} item ${item.name}`)
        shouldAdd = false;
      }
    });
  }
  else {
    currentListTags.value.forEach(tag => {
      console.log(`tag ${tag}`)
      if (tag === item) {
        console.log(`tag ${tag} item ${item}`)
        shouldAdd = false;
      }
    });
  }
  return shouldAdd
}

const resetList = () => {
    editMode.value = false
    currentListTags.value.splice(0)
    customList.value = newList()
}

const editList = (list: CustomList) => {
  if (editMode.value) {
  resetList()
  } else {
    editMode.value = true
    customList.value = list
    currentListTags.value = customList.value.tags.split(",")
  }
}

getCustomLists()
</script>

<template>
  <div class="flex flex-wrap justify-self-center sm:w-8/12 w-fit">
    <div class="sm:mx-1 w-full sm:w-1/2">
      <h1 class="typewriter text-2xl mb-3 capitalize">
        {{ t('settings.custom_lists') }} :
      </h1>
      <div>
        <ul>
          <li
            v-for="list in customLists"
            :key="list.id"
            class="my-2"
          >
            <div class="alert shadow-lg w-full jl-card">
              <i class="mdi mdi-bookshelf mdi-24px" />
              <div>
                <h3 class="font-bold">
                  <router-link
                    class="link hover:underline hover:decoration-4 hover:decoration-secondary text-sm italic"
                    :to="{ name: 'list-detail', params: { listId: list.id } }"
                  >
                    {{ list.name }}&nbsp;
                  </router-link>
                </h3>
                <p>{{ list.tags }}</p>
              </div>
              <div class="flex flex-wrap gap-2 justify-end">
                <span
                  v-if="list.actionable"
                  class="badge badge-primary"
                >{{ t('lists.actionable') }}</span>
                <span
                  v-if="list.public"
                  class="badge badge-secondary"
                >{{ t('lists.public') }}</span>
                <button
                  class="btn btn-sm"
                  @click="editList(list)"
                >
                  <svg
                    v-if="editMode"
                    xmlns="http://www.w3.org/2000/svg"
                    viewBox="0 0 24 24"
                    fill="currentColor"
                    class="size-6 h-5 w-5"
                  >
                    <path
                      fill-rule="evenodd"
                      d="M12 2.25c-5.385 0-9.75 4.365-9.75 9.75s4.365 9.75 9.75 9.75 9.75-4.365 9.75-9.75S17.385 2.25 12 2.25Zm-1.72 6.97a.75.75 0 1 0-1.06 1.06L10.94 12l-1.72 1.72a.75.75 0 1 0 1.06 1.06L12 13.06l1.72 1.72a.75.75 0 1 0 1.06-1.06L13.06 12l1.72-1.72a.75.75 0 1 0-1.06-1.06L12 10.94l-1.72-1.72Z"
                      clip-rule="evenodd"
                    />
                  </svg>
                  <svg
                    v-else
                    xmlns="http://www.w3.org/2000/svg"
                    viewBox="0 0 24 24"
                    fill="currentColor"
                    class="size-6 h-5 w-5"
                  >
                    <path d="M21.731 2.269a2.625 2.625 0 0 0-3.712 0l-1.157 1.157 3.712 3.712 1.157-1.157a2.625 2.625 0 0 0 0-3.712ZM19.513 8.199l-3.712-3.712-12.15 12.15a5.25 5.25 0 0 0-1.32 2.214l-.8 2.685a.75.75 0 0 0 .933.933l2.685-.8a5.25 5.25 0 0 0 2.214-1.32L19.513 8.2Z" />
                  </svg>
                </button>
                <button
                  class="btn btn-sm"
                  @click="deleteList(list)"
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    class="h-5 w-5"
                    viewBox="0 0 20 20"
                    fill="currentColor"
                  >
                    <path
                      fill-rule="evenodd"
                      d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z"
                      clip-rule="evenodd"
                    />
                  </svg>
                </button>
              </div>
            </div>
          </li>
        </ul>
      </div>
    </div>
    <div class="sm:mx-1 w-full sm:w-5/12">
      <div>
        <fieldset class="fieldset bg-base-200 border-base-300 rounded-box w-full border p-4">
          <legend class="fieldset-legend capitalize">
            {{ editMode ? t("lists.edit_list") : t("lists.create_list") }}
          </legend>

          <label class="label capitalize">{{ t("lists.name") }}</label>
          <input
            v-model="customList.name"
            type="text"
            class="input input-accent mb-1 w-full"
            :placeholder="t('lists.name')"
          >

  
          <label class="label capitalize">{{ t("lists.tags") }}
            <svg
              v-tooltip="t('lists.tags_desc')"
              xmlns="http://www.w3.org/2000/svg"
              class="h-5 w-5"
              viewBox="0 0 20 20"
              fill="currentColor"
            >
              <path
                fill-rule="evenodd"
                d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
                clip-rule="evenodd"
              />
            </svg>
          </label>
          <div class="jelu-taginput mb-4">
            <o-taginput
              v-model="currentListTags"
              :options="filteredTags"
              :allow-autocomplete="true"
              autocomplete="off"
              :before-adding="beforeAddTag"
              :allow-new="false"
              :allow-duplicates="false"
              :open-on-focus="true"
              icon-pack="mdi"
              icon="tag-plus"
              field="name"
              :placeholder="t('labels.add_tag')"
              @input="getFilteredTags"
            >
              <template #default="{ value }">
                <div class="jl-taginput-item">
                  {{ value }}
                </div>
              </template>
              <template #selected="{ removeItem, items }">
                <ClosableBadge
                  v-for="(item, index) in items"
                  :key="item"
                  :content="item"
                  class="badge-primary"
                  @closed="removeItem(index, $event)"
                />
              </template>
            </o-taginput>
          </div>

          <label class="label capitalize">{{ t("lists.actionable") }}
            <svg
              v-tooltip="t('lists.actionable_desc')"
              xmlns="http://www.w3.org/2000/svg"
              class="h-5 w-5"
              viewBox="0 0 20 20"
              fill="currentColor"
            >
              <path
                fill-rule="evenodd"
                d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
                clip-rule="evenodd"
              />
            </svg>
          </label>
          <input
            v-model="customList.actionable"
            type="checkbox"
            class="checkbox checkbox-accent mb-1"
          >

          <label class="label capitalize">{{ t("lists.public") }}</label>
          <input
            v-model="customList.public"
            type="checkbox"
            class="checkbox checkbox-accent"
          >

          <button
            class="btn btn-primary mt-4 capitalize"
            :disabled="currentListTags.length < 1 || customList.name.length < 1"
            @click="createList"
          >
            {{ editMode ? t("lists.update_list") : t("lists.create") }}
          </button>
        </fieldset>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
</style>
