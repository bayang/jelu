<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { computed, ref, Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import { Shelf } from "../model/Shelf"
import { Tag } from "../model/Tag"
import dataService from "../services/DataService"
import { key } from '../store'

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | User page')

const store = useStore(key)

let filteredTags: Ref<Array<Tag>> = ref([]);
const isFetching = ref(false)

function getFilteredTags(text: string) {
  isFetching.value = true
  dataService.findTagsByCriteria(text).then((data) => filteredTags.value = data.content)
  isFetching.value = false
}

function createShelfFromTag(tag: Tag, event: UIEvent) {
  console.log(tag)
  console.log(event)
  // we receive from oruga weird events while nothing is selected
  // so try to get rid of those null data we receive
  if (tag != null && event != null && tag.id != null) {
    dataService.saveShelf({name: tag.name, targetId: tag.id})
      .then(res => {
        console.log("saved shef " + res.name)
        store.dispatch('getUserShelves')
      })
      .catch(err => console.log("failed to save shelf " + tag.name + " " + err))
  }
}

function deleteShelf(shelf: Shelf) {
  dataService.deleteShelf(shelf.id)
  .then(res => {
    console.log("deleted shelf " + shelf.id)
    store.dispatch('getUserShelves')
    })
  .catch(err => console.log("failed to delete shelf " + shelf.id))
}


const shelves = computed(() => {
  return store.getters.getShelves
})

</script>

<template>
  <div class="w-fit flex flex-wrap justify-items-center justify-self-center">
    <div class="sm:mx-1 w-full sm:w-fit">
      <h1 class="typewriter text-2xl mb-3 capitalize">
        {{ t('settings.shelves') }} :
      </h1>
      <div>
        <ul>
          <li
            v-for="shelf in shelves"
            :key="shelf"
            class="my-2"
          >
            <div class="alert shadow-lg w-full jl-card">
              <i class="mdi mdi-bookshelf mdi-24px" />
              <h3 class="font-bold">
                {{ shelf.name }}
              </h3>
              <button
                class="btn btn-sm"
                @click="deleteShelf(shelf)"
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
          </li>
        </ul>
      </div>
    </div>
    <div class="sm:mx-1 w-full sm:w-fit">
      <div
        v-if="shelves.length >= 10"
        class="mt-4 text-lg"
      >
        {{ t('settings.shelves_max_number_reached') }}
      </div>
      <div v-else>
        <div class="field">
          <o-field :label="t('settings.shelf_choose_tag')">
            <o-autocomplete
              :input-classes="{rootClass:'border-2 border-accent'}"
              :data="filteredTags"
              :clear-on-select="true"
              field="name"
              :loading="isFetching"
              :debounce="100"
              @input="getFilteredTags"
              @select="createShelfFromTag"
            />
          </o-field>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
</style>
