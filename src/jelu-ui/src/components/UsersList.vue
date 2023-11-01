<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { Ref, computed, ref } from 'vue'
import Avatar from 'vue-avatar-sdh'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import { User } from '../model/User'
import dataService from "../services/DataService"
import { key } from '../store'

useTitle('Jelu | Users')

const store = useStore(key)
const { t, locale, availableLocales } = useI18n({
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
          <div class="card-body">
            <p>
              <router-link
                class="link hover:underline hover:decoration-4 hover:decoration-secondary"
                :to="{ name: 'user-detail', params: { userId: user.id } }"
              >
                <strong>{{ user.login }}</strong>&nbsp;
              </router-link>
              <br>
            </p>
          </div>
        </div>
      </div>
    </TransitionGroup>
  </div>
</template>

<style lang="scss" scoped>

</style>
