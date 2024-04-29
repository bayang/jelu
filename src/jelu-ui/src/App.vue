<script setup lang="ts">
import { useStore } from 'vuex'
import { computed, onMounted, Ref, ref, watch } from 'vue'
import { key } from './store'
import { useRoute, useRouter } from 'vue-router'
import dataService from "./services/DataService";
import Avatar from 'vue-avatar-sdh'
import { themeChange } from 'theme-change'
import { useI18n } from 'vue-i18n'
import { StringUtils } from './utils/StringUtils';
import { useRegisterSW } from 'virtual:pwa-register/vue';

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

const initialLoad : Ref<boolean> = ref(false)

store.dispatch('setupStatus')
initialLoad.value = true
store.dispatch('getUser')
  .then(async () => {
    console.log("then")
    // try {
    // await router.push({ path: store.state.entryPoint })
    if (store.state.route != null) {
      await router.push(store.state.route)
    }
    console.log("ok nav")
    initialLoad.value = false
    store.dispatch('getServerSettings')
    store.dispatch('getUserShelves')
    // } catch(e) {
    // console.log("error nav")
    // console.log(e)
    // }
  })
  .catch(() => {
    if (store.state.route != null && store.state.route.name === "review-detail") {
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
const shelves = computed(() => {
  return store.getters.getShelves
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

</script>

<template>
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
  <section>
    <div class="navbar bg-base-100">
      <div class="navbar-start">
        <div class="dropdown">
          <label
            tabindex="0"
            class="btn btn-ghost lg:hidden"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              class="h-5 w-5"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M4 6h16M4 12h8m-8 6h16"
              />
            </svg>
          </label>
          <ul
            tabindex="0"
            class="menu menu-compact dropdown-content z-[1] mt-3 p-2 shadow bg-base-100 rounded-box w-52"
          >
            <li @click="collapseDropdown()">
              <router-link
                v-if="isLogged"
                class="font-sans text-base capitalize"
                :to="{ name: 'my-books' }"
              >
                {{ t('nav.my_books') }}
              </router-link>
            </li>
            <li @click="collapseDropdown()">
              <router-link
                v-if="isLogged"
                class="font-sans text-base capitalize"
                :to="{ name: 'to-read' }"
              >
                {{ t('nav.to_read') }}
              </router-link>
            </li>
            <li @click="collapseDropdown()">
              <router-link
                v-if="isLogged"
                class="font-sans text-base capitalize"
                :to="{ name: 'random' }"
              >
                {{ t('nav.random') }}
              </router-link>
            </li>
            <li @click="collapseDropdown()">
              <router-link
                v-if="isLogged"
                :to="{ name: 'add-book' }"
                class="font-sans text-base capitalize"
              >
                {{ t('nav.add_book') }}
              </router-link>
            </li>
            <li @click="collapseDropdown()">
              <router-link
                v-if="isLogged"
                :to="{ name: 'history' }"
                class="font-sans text-base capitalize"
              >
                {{ t('nav.history') }}
              </router-link>
            </li>
            <li @click="collapseDropdown()">
              <router-link
                v-if="isLogged"
                :to="{ name: 'search' }"
                class="font-sans text-base capitalize"
              >
                {{ t('nav.search') }}
              </router-link>
            </li>
          </ul>
        </div>
        <router-link
          :to="{ name: 'home' }"
        >
          <img
            src="./assets/jelu_logo.svg"
            alt="home"
            class="w-14"
          >
        </router-link>
        <div
          v-if="isLogged && showSearchInput"
          class="dropdown"
        >
          <label
            tabindex="0"
            class="btn btn-ghost rounded-btn lg:hidden"
          ><svg
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
          /></svg></label>
          <div
            tabindex="0"
            class="dropdown-content mt-2 -left-20"
          >
            <div
              class="form-control w-2"
            >
              <div class="join">
                <input
                  v-model="searchQuery"
                  type="text"
                  placeholder="Search…"
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
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="navbar-center hidden lg:flex">
        <ul class="menu menu-horizontal p-0">
          <li>
            <router-link
              v-if="isLogged"
              class="font-sans text-xl capitalize"
              :to="{ name: 'my-books' }"
            >
              {{ t('nav.my_books') }}
            </router-link>
          </li>
          <li>
            <router-link
              v-if="isLogged"
              class="font-sans text-xl capitalize"
              :to="{ name: 'to-read' }"
            >
              {{ t('nav.to_read') }}
            </router-link>
          </li>
          <li>
            <router-link
              v-if="isLogged"
              class="font-sans text-xl capitalize"
              :to="{ name: 'random' }"
            >
              {{ "Random" }}
            </router-link>
          </li>
          <li>
            <router-link
              v-if="isLogged"
              :to="{ name: 'add-book' }"
              class="font-sans text-xl capitalize"
            >
              {{ t('nav.add_book') }}
            </router-link>
          </li>
          <li>
            <router-link
              v-if="isLogged"
              :to="{ name: 'history' }"
              class="font-sans text-xl capitalize"
            >
              {{ t('nav.history') }}
            </router-link>
          </li>
          <li
            v-if="shelves !== null && shelves.length > 0 && isLogged"
            class="mr-1"
          >
            <div class="dropdown dropdown-bottom">
              <label
                tabindex="0"
              >
                <div class="flex items-center">
                  <a class="font-sans text-xl capitalize">
                    {{ t('nav.shelves') }}
                  </a>
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke-width="1.5"
                    stroke="currentColor"
                    class="w-6 h-6 swap-on"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="m19.5 8.25-7.5 7.5-7.5-7.5"
                    />
                  </svg>
                </div>
              </label>
              <ul
                tabindex="0"
                class="mt-3 p-2 dropdown-content z-[1] bg-base-100 rounded-box w-52"
              >
                <li
                  v-for="shelf in shelves"
                  :key="shelf"
                  @click="collapseDropdown()"
                >
                  <router-link
                    v-if="isLogged"
                    :to="{ name: 'tag-detail', params: { tagId: shelf.targetId }, query: {sort: 'modificationDate,desc'} }"
                  >
                    {{ shelf.name }}
                  </router-link>
                </li>
              </ul>
            </div>
          </li>
        </ul>
        <div
          v-if="isLogged && showSearchInput"
          class="form-control"
        >
          <div class="join">
            <input
              v-model="searchQuery"
              type="text"
              placeholder="Search…"
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
          </div>
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
      <div class="navbar-end">
        <div
          v-if="isLogged && shelves != null && shelves.length > 0"
          class="dropdown lg:hidden"
        >
          <label
            tabindex="0"
            class="btn btn-ghost rounded-btn lg:hidden"
          >
            <span class="h-fit"><i class="mdi mdi-bookshelf mdi-24px" /></span>
          </label>
          <div
            tabindex="0"
            class="dropdown-content z-[1] mt-2 -left-20"
          >
            <ul
              tabindex="0"
              class="menu menu-sm dropdown-content mt-3 p-2 shadow bg-base-100 rounded-box w-52"
            >
              <li
                v-for="shelf in shelves"
                :key="shelf"
                @click="collapseDropdown()"
              >
                <router-link
                  v-if="isLogged"
                  :to="{ name: 'tag-detail', params: { tagId: shelf.targetId }, query: {sort: 'modificationDate,desc'} }"
                >
                  {{ shelf.name }}
                </router-link>
              </li>
            </ul>
          </div>
        </div>
        <div class="dropdown dropdown-end">
          <label
            tabindex="0"
            class="btn btn-ghost btn-circle avatar"
          >
            <Avatar
              :size="40"
              :username="username"
              class="w-10"
            />
          </label>
          <ul
            tabindex="0"
            class="mt-3 p-2 shadow menu menu-sm dropdown-content z-[1] bg-base-100 rounded-box w-52"
          >
            <li
              v-if="isLogged"
              @click="collapseDropdown()"
            >
              <router-link
                class="font-sans text-base capitalize"
                :to="{ name: 'profile-page' }"
              >
                {{ t('nav.dashboard') }}
              </router-link>
            </li>
            <li
              v-if="!isLogged"
              @click="collapseDropdown()"
            >
              <router-link
                class="font-sans text-base"
                :to="{ name: 'login' }"
              >
                {{ t('nav.login') }}
              </router-link>
            </li>
            <li
              v-if="isLogged"
              @click="collapseDropdown()"
            >
              <a
                class="font-sans text-base capitalize"
                @click="logout()"
              >
                {{ t('nav.logout') }}
              </a>
            </li>
            <li @click="collapseDropdown()">
              <label
                for="shortcuts-modal"
                class="font-sans text-base modal-button"
              >{{ t('settings.shortcuts') }}</label>
            </li>
          </ul>
        </div>
      </div>
    </div>
    <div class="divider mt-0" /> 

    <router-view />
    <input
      id="shortcuts-modal"
      type="checkbox"
      class="modal-toggle"
    >
    <label
      for="shortcuts-modal"
      class="modal cursor-pointer"
    >
      <label
        class="modal-box relative"
        for=""
      >
        <h1 class="typewriter text-2xl mb-3 capitalize">
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
      </label>
    </label>
    <o-loading
      v-model:active="initialLoad"
      :full-page="true"
      :can-cancel="false"
    >
      <!-- loader from https://loading.io/css/ -->
      <div class="lds-facebook">
        <div /><div /><div />
      </div>
    </o-loading>
  </section>
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
