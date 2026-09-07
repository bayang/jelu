<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next";
import { themeChange } from 'theme-change';
import { useRegisterSW } from 'virtual:pwa-register/vue';
import { computed, onMounted, Ref, ref, useTemplateRef, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import ScanModal from "./components/ScanModal.vue";
import UserShelvesModal from './components/UserShelvesModal.vue';
import dataService from "./services/DataService";
import { key } from './store';
import { StringUtils } from './utils/StringUtils';
import useTypography from "./composables/typography";
import MenuBarItem from "./components/MenuBarItem.vue";
import { BookUser, BookCopy, Dices, BookPlus, ClipboardClock, Users, Search, LibraryBig, Command, LogOut, LogIn,SquareActivity } from '@lucide/vue';

const {
  offlineReady,
  needRefresh,
  updateServiceWorker,
} = useRegisterSW()

const close = async() => {
  offlineReady.value = false
  needRefresh.value = false
}

const store = useStore(key)
const router = useRouter()
const route = useRoute()
const { t, locale } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

console.log("route " + route.fullPath + " " + route.path + " " + route.redirectedFrom)
console.log(route)
console.log(router.currentRoute.value)

const oruga = useOruga();
const initialLoad : Ref<boolean> = ref(false)
const progress: Ref<boolean> = ref(false)

const shortcutsModal = useTemplateRef("shortcuts_modal")

store.dispatch('setupStatus')
initialLoad.value = true
store.dispatch('getUser')
  .then(async () => {
    console.log("then")
    if (store.state.route != null) {
      await router.push(store.state.route)
    }
    console.log("ok nav")
    initialLoad.value = false
    store.dispatch('getServerSettings')
  })
  .catch(() => {
    if (store.state.route != null && (
      store.state.route.name === "review-detail" ||
      store.state.route.name === "list-detail"||
      store.state.route.name === "book-reviews")) {
      router.push(store.state.route)
      initialLoad.value = false
      return
    }
    initialLoad.value = false
    console.log("catch in App")
    router.push({ name: 'login' }).then(() => { console.log("ok nav") }).catch(() => { console.log("error nav") })
  })

const username = computed(() => {
  return store.getters.getUsername
})
const isLogged = computed(() => {
  return store.getters.getLogged
})

onMounted(() => {
  console.log('Component is mounted!')
  themeChange(false);
})

const logout = () => {
  console.log("logout")
  dataService.logout()
    .then(res => {
      store.dispatch('logout')
    })
}

const searchQuery = ref('')

const showAdvanced = ref(false)
const hideAdvanced = () => {
  setTimeout(() => showAdvanced.value = false, 1000)
}

const showSearchInput = ref(true)

const search = () => {
  console.log(searchQuery.value)
  if (StringUtils.isNotBlank(searchQuery.value)) {
    showAdvanced.value = false
    router.push({ path: '/search', query: { q: searchQuery.value } })
  }
}

// hide the search input if current view is the search page
watch(() => route.name, (newVal, oldVal) => {
  if (route.name === 'search') {
    showSearchInput.value = false
  } else {
    showSearchInput.value = true
  }
})

const collapseDropdown = () => {
  if (document.activeElement instanceof HTMLElement) {
    document.activeElement.blur();
  }
}

const { typographyClasses } = useTypography()

let barcodeReader: any = null

function toggleScanModal() {
    oruga.modal.open({
      component: ScanModal,
      trapFocus: true,
      active: true,
      canCancel: ['x', 'button', 'outside'],
      scroll: 'keep',
      props: {
      },
      events: {
        decoded: (barcode: string|null) => {
          console.log("barcode " + barcode)
          if (barcode != null) {
            // form.isbn = barcode
            searchQuery.value = barcode
            search()
          }
      },
      barcodeLoaded: (reader: any) => {
        barcodeReader = reader
      }
    },
      onClose: scanModalClosed
    });
}

function toggleShelvesModal() {
  oruga.modal.open({
      component: UserShelvesModal,
      trapFocus: true,
      active: true,
      canCancel: ['x', 'button', 'outside'],
      scroll: 'keep',
      onClose: scanModalClosed,
    });
}

function scanModalClosed() {
  console.log("scan modal closed")
}

</script>

<template>
  <div>
    <div
      v-if="offlineReady || needRefresh"
      class="pwa-toast"
      role="alert"
    >
      <div class="message">
        <span v-if="offlineReady">
          App ready to work offline
        </span>
        <span v-else>
          New content available, click on reload button to update.
        </span>
      </div>
      <button
        v-if="needRefresh"
        @click="updateServiceWorker()"
      >
        Reload
      </button>
      <button @click="close">
        Close
      </button>
    </div>
    <div class="drawer lg:drawer-open">
      <input
        id="my-drawer-4"
        type="checkbox"
        class="drawer-toggle inline"
        autocomplete="off"
      >
      <div class="drawer-content">
        <nav class="navbar w-full bg-base-200">
          <label
            for="my-drawer-4"
            aria-label="open sidebar"
            class="btn btn-square btn-ghost drawer-button"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              stroke-linejoin="round"
              stroke-linecap="round"
              stroke-width="2"
              fill="none"
              stroke="currentColor"
              class="my-1.5 inline-block size-7"
            ><path d="M4 4m0 2a2 2 0 0 1 2 -2h12a2 2 0 0 1 2 2v12a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2z" /><path d="M9 4v16" /><path d="M14 10l2 2l-2 2" /></svg>
          </label>
          <div class="px-4 flex items-center gap-3 navbar-start">
            <router-link
              :to="{ name: 'home' }"
            >
              <img
                src="./assets/jelu_logo.svg"
                alt="home"
                class="w-14"
              >
            </router-link>
            <span>Jelu</span>
          </div>

          <div
            v-if="isLogged && showSearchInput"
            class="form-control navbar-center"
          >
            <div class="join">
              <input
                v-model="searchQuery"
                type="text"
                :placeholder="t('labels.search_query')"
                class="input input-accent join-item"
                @focus="showAdvanced = true"
                @blur="hideAdvanced"
                @keyup.enter="search"
              >
              <button
                class="btn btn-square btn-outline join-item"
                @click="search"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  class="h-6 w-6"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                ><path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                /></svg>
              </button>
              <button
                class="btn btn-warning p-2 mx-1"
                :class="{'btn-disabled' : progress}"
                @click="toggleScanModal"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke-width="1.5"
                  stroke="currentColor"
                  class="w-6 h-6"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M3.75 4.875c0-.621.504-1.125 1.125-1.125h4.5c.621 0 1.125.504 1.125 1.125v4.5c0 .621-.504 1.125-1.125 1.125h-4.5A1.125 1.125 0 013.75 9.375v-4.5zM3.75 14.625c0-.621.504-1.125 1.125-1.125h4.5c.621 0 1.125.504 1.125 1.125v4.5c0 .621-.504 1.125-1.125 1.125h-4.5a1.125 1.125 0 01-1.125-1.125v-4.5zM13.5 4.875c0-.621.504-1.125 1.125-1.125h4.5c.621 0 1.125.504 1.125 1.125v4.5c0 .621-.504 1.125-1.125 1.125h-4.5A1.125 1.125 0 0113.5 9.375v-4.5z"
                  />
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M6.75 6.75h.75v.75h-.75v-.75zM6.75 16.5h.75v.75h-.75v-.75zM16.5 6.75h.75v.75h-.75v-.75zM13.5 13.5h.75v.75h-.75v-.75zM13.5 19.5h.75v.75h-.75v-.75zM19.5 13.5h.75v.75h-.75v-.75zM19.5 19.5h.75v.75h-.75v-.75zM16.5 16.5h.75v.75h-.75v-.75z"
                  />
                </svg>
              </button>
            </div>

            <Transition>
              <button
                v-if="showAdvanced"
                class="btn btn-circle btn-outline border-0 tooltip tooltip-bottom lowercase"
                :data-tip="t('labels.advanced_search')"
              >
                <router-link
                  class="link-hover font-sans"
                  :to="{ name: 'search' }"
                >
                  <span class="mdi mdi-magnify-plus-outline mdi-24 text-3xl" />
                </router-link>
              </button>
            </Transition>
          </div>
        </nav>
        <div class="container mx-auto">
          <router-view />
        </div>
      </div>

      <div class="drawer-side is-drawer-close:overflow-visible">
        <label
          for="my-drawer-4"
          aria-label="close sidebar"
          class="drawer-overlay"
        />
        <div class="flex min-h-full flex-col items-start bg-base-200 is-drawer-close:w-16 is-drawer-open:w-64 p-0">
          <ul class="menu w-full grow flex justify-between">
            <div>
              <li>
                <menu-bar-item
                  :is-logged="isLogged"
                  :name="t('nav.my_books')"
                  destination="my-books"
                >
                  <template #icon>
                    <BookUser />
                  </template>
                </menu-bar-item>
              </li>
              <li>
                <menu-bar-item
                  :is-logged="isLogged"
                  :name="t('nav.to_read')"
                  destination="to-read"
                >
                  <template #icon>
                    <BookCopy />
                  </template>
                </menu-bar-item>
              </li>
              <li>
                <menu-bar-item
                  :is-logged="isLogged"
                  :name="t('nav.random')"
                  destination="random"
                >
                  <template #icon>
                    <Dices />
                  </template>
                </menu-bar-item>
              </li>
              <li>
                <menu-bar-item
                  :is-logged="isLogged"
                  :name="t('nav.add_book')"
                  destination="add-book"
                >
                  <template #icon>
                    <BookPlus />
                  </template>
                </menu-bar-item>
              </li>
              <li>
                <menu-bar-item
                  :is-logged="isLogged"
                  :name="t('nav.history')"
                  destination="history"
                >
                  <template #icon>
                    <ClipboardClock />
                  </template>
                </menu-bar-item>
              </li>
              <li>
                <menu-bar-item
                  :is-logged="isLogged"
                  :name="t('nav.activity')"
                  destination="reviews"
                >
                  <template #icon>
                    <SquareActivity />
                  </template>
                </menu-bar-item>
              </li>
              <li>
                <menu-bar-item
                  :is-logged="isLogged"
                  :name="t('book.author', 2)"
                  destination="authors"
                >
                  <template #icon>
                    <Users />
                  </template>
                </menu-bar-item>
              </li>
              <li>
                <menu-bar-item
                  :is-logged="isLogged"
                  :name="t('nav.search')"
                  destination="search"
                >
                  <template #icon>
                    <Search />
                  </template>
                </menu-bar-item>
              </li>
              <li v-if="isLogged">
                <button
                  class="tooltip tooltip-right"
                  :data-tip="t('settings.shelves')"
                  @click="toggleShelvesModal"
                >
                  <LibraryBig />
                  <span class="font-sans text-base capitalize is-drawer-close:hidden">{{ t("settings.shelves") }}</span>
                </button>
              </li>
            </div>
            <div>
              <li>
                <menu-bar-item
                  :is-logged="isLogged"
                  :name="t('nav.dashboard')"
                  destination="profile-page"
                >
                  <template #icon>
                    <div class="avatar avatar-placeholder">
                      <div class="bg-neutral text-neutral-content w-10 rounded-full">
                        <span>{{ username.substring(0,2) }}</span>
                      </div>
                    </div>
                  </template>
                </menu-bar-item>
              </li>
              <li>
                <button
                  class="is-drawer-close:tooltip is-drawer-close:tooltip-right"
                  :data-tip="t('settings.shortcuts')"
                  @click="shortcutsModal?.showModal()"
                >
                  <Command />
                  <span class="is-drawer-close:hidden">{{ t('settings.shortcuts') }}</span>
                </button>
              </li>
              <li v-if="!isLogged">
                <button
                  class="is-drawer-close:tooltip is-drawer-close:tooltip-right"
                  :data-tip="t('nav.login')"
                >
                  <router-link
                    class="font-sans text-base capitalize flex items-center gap-1"
                    :to="{ name: 'login' }"
                  >
                    <LogIn />
                    <span class="is-drawer-close:hidden">{{ t('nav.login') }}</span>
                  </router-link>
                </button>
              </li>
              <li
                v-if="isLogged"
              >
                <button
                  class="is-drawer-close:tooltip is-drawer-close:tooltip-right"
                  :data-tip="t('nav.logout')"
                  @click="logout()"
                >
                  <LogOut />
                  <span class="is-drawer-close:hidden">{{ t('nav.logout') }}</span>
                </button>
              </li>
            </div>
          </ul>
        </div>
      </div>
    </div>
  </div>
  <dialog
    id="shortcuts_modal"
    ref="shortcuts_modal"
    class="modal"
  >
    <div class="modal-box">
      <h1
        class="text-2xl mb-3 capitalize"
        :class="typographyClasses"
      >
        {{ t('settings.shortcuts') }} :
      </h1>
      <div class="flex flex-row flex-wrap justify-center basis-10/12 sm:basis-1/3">
        <p class="basis-full mt-2">
          <kbd class="kbd">shift</kbd>
          +
          <kbd class="kbd">f</kbd> : {{ t('shortcuts.toggle_bar') }}
        </p>
        <p class="basis-full mt-2">
          <kbd class="kbd">shift</kbd>
          +
          <kbd class="kbd">◀︎</kbd> : {{ t('shortcuts.page_previous') }}
        </p>
        <p class="basis-full mt-2">
          <kbd class="kbd">shift</kbd>
          +
          <kbd class="kbd">▶︎</kbd> : {{ t('shortcuts.page_next') }}
        </p>
      </div>
      <p class="py-4">
        Press ESC key or click outside to close
      </p>
    </div>
    <form
      method="dialog"
      class="modal-backdrop"
    >
      <button>close</button>
    </form>
  </dialog>
  <o-loading
    v-model:active="initialLoad"
    :full-page="true"
    :cancelable="false"
  >
    <!-- loader from https://loading.io/css/ -->
    <div class="lds-facebook">
      <div /><div /><div />
    </div>
  </o-loading>
</template>

<style lang="css">

#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
}

.v-enter-active,
.v-leave-active {
  transition: opacity 0.7s ease;
}

.v-enter-from,
.v-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}
.pwa-toast {
  position: fixed;
  right: 0;
  bottom: 0;
  margin: 16px;
  padding: 12px;
  border: 1px solid #8885;
  border-radius: 4px;
  z-index: 1;
  text-align: left;
  box-shadow: 3px 4px 5px 0 #8885;
  background-color: white;
}
.pwa-toast .message {
  margin-bottom: 8px;
}
.pwa-toast button {
  border: 1px solid #8885;
  outline: none;
  margin-right: 5px;
  border-radius: 2px;
  padding: 3px 10px;
}
</style>
