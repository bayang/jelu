<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { Ref, computed, ref } from 'vue'
import Avatar from 'vue-avatar-sdh'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import { User } from '../model/User'
import dataService from "../services/DataService"
import { key } from '../store'
import { ObjectUtils } from '../utils/ObjectUtils'

useTitle('Jelu | Users')

const store = useStore(key)
const isAdmin = computed(() => {
  return store !== undefined && store.getters.isAdmin
})
const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const users: Ref<Array<User>> = ref([]);

const getUsers = async () => {
  try {
    users.value = await dataService.getUsers()
  } catch (error) {
    console.log("failed get users : " + error);
  }
};

const filtered = computed(() => {
  return users.value.filter(u => u.id != store.getters.getUser.id).sort((a, b) => a.login.toLowerCase() < b.login.toLowerCase() ? -1 : 1)
})

const deleteUser = async (user: User) => {
    let abort = false
    await ObjectUtils.swalMixin.fire({
      html: `<p>${t('admin_user.delete_user')} ?</p>`,
      showCancelButton: true,
      showConfirmButton: false,
      showDenyButton: true,
      confirmButtonText: t('labels.delete'),
      cancelButtonText: t('labels.dont_delete'),
      denyButtonText: t('labels.delete'),
    }).then((result) => {
      if (result.isDismissed) {
        abort = true
        return;
      }
    })
    if (abort) {
      return
    }
    if (user.id != null) {
        dataService.deleteUser(user.id)
        .then(_ => {
            getUsers()
    })
    .catch(err => {
      console.log(err)
    })
    }
}

getUsers()

</script>

<template>
  <h2 class="text-3xl typewriter capitalize mb-4">
    {{ t('settings.users') }}
  </h2>
  <div class="flex flex-row flex-wrap gap-3">
    <TransitionGroup name="list">
      <div
        v-for="user in filtered"
        :key="user.id"
      >
        <div class="card card-side bg-base-200 shadow-2xl">
          <Avatar
            :username="user.login"
            class="ml-4 mt-5"
          />
          <div class="card-body flex-row gap-3">
            <p class="basis-2/3">
              <router-link
                class="link hover:underline hover:decoration-4 hover:decoration-secondary"
                :to="{ name: 'user-detail', params: { userId: user.id } }"
              >
                <strong>{{ user.login }}</strong>&nbsp;
              </router-link>
              <br>
            </p>
            <button
              v-if="isAdmin"
              class="btn btn-sm basis-1/3"
              @click="deleteUser(user)"
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
      </div>
    </TransitionGroup>
  </div>
</template>

<style lang="scss" scoped>

</style>
