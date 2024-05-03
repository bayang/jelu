<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next"
import { useTitle } from '@vueuse/core'
import { computed, ref, Ref } from 'vue'
import Avatar from 'vue-avatar-sdh'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import { key } from '../store'
import UserModalVue from './UserModal.vue'
import { Provider } from "../model/User"
import { Shelf } from "../model/Shelf"
import dataService from "../services/DataService";
import { Tag } from "../model/Tag"

const oruga = useOruga()

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | User page')

const store = useStore(key)

const showModal: Ref<boolean> = ref(false)

const user = computed(() => {
  return store.getters.getUser
})

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

function toggleUserModal() {
  showModal.value = !showModal.value
  oruga.modal.open({
    component: UserModalVue,
    trapFocus: true,
    active: true,
    canCancel: ['x', 'button', 'outside'],
    scroll: 'keep',
    props: {
      "currentUser": user.value
    },
    onClose: modalClosed
  });
}

function deleteShelf(shelf: Shelf) {
  dataService.deleteShelf(shelf.id)
  .then(res => {
    console.log("deleted shelf " + shelf.id)
    store.dispatch('getUserShelves')
    })
  .catch(err => console.log("failed to delete shelf " + shelf.id))
}

function modalClosed() {
  console.log("modal closed")
  store.dispatch('getUser')
}

const shelves = computed(() => {
  return store.getters.getShelves
})

</script>

<template>
  <div class="w-fit flex flex-wrap justify-items-center justify-self-center">
    <div>
      <h1 class="typewriter text-2xl mb-3 capitalize">
        {{ t('settings.profile') }} :
      </h1>
      <div class="card card-side bg-base-200 shadow-2xl">
        <Avatar
          :username="store != null && store.getters.getUsername"
          class="ml-4 mt-8"
        />
        <div class="card-body">
          <p>
            <span class="capitalize">{{ t('settings.username') }}</span> :
            <strong>{{ store.getters.getUsername }}</strong>
            <br>
            <span class="capitalize">{{ t('settings.role', 2) }}</span> :
            <span class="badge badge-info tag">USER</span> &nbsp;
            <span
              v-if="store.getters.isAdmin"
              class="badge badge-warning tag"
            >ADMIN</span>
          </p>
          <div class="card-actions justify-end">
            <button
              v-if="user.provider !== Provider.LDAP && user.provider !== Provider.PROXY"
              v-tooltip="t('profile.edit_user')"
              class="btn btn-circle btn-ghost"
              @click="toggleUserModal"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="24"
                height="24"
                fill="text-base-content"
              >
                <path
                  d="M19.045 7.401c.378-.378.586-.88.586-1.414s-.208-1.036-.586-1.414l-1.586-1.586c-.378-.378-.88-.586-1.414-.586s-1.036.208-1.413.585L4 13.585V18h4.413L19.045 7.401zm-3-3 1.587 1.585-1.59 1.584-1.586-1.585 1.589-1.584zM6 16v-1.585l7.04-7.018 1.586 1.586L7.587 16H6zm-2 4h16v2H4z"
                />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>

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
