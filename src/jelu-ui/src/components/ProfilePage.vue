<script setup lang="ts">
import { useProgrammatic } from "@oruga-ui/oruga-next"
import { useTitle } from '@vueuse/core'
import { computed, ref, Ref } from 'vue'
import Avatar from 'vue-avatar-sdh'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import { key } from '../store'
import UserModalVue from './UserModal.vue'

const {oruga} = useProgrammatic()

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

function modalClosed() {
  console.log("modal closed")
  store.dispatch('getUser')
}

</script>

<template>
  <div class="w-fit">
    <h1 class="typewriter text-2xl mb-3 capitalize">
      {{ t('settings.profile') }} :
    </h1>
    <div class="card card-side bg-base-100 shadow-2xl">
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
          <span class="badge badge-info tag is-info">USER</span> &nbsp;
          <span
            v-if="store.getters.isAdmin"
            class="badge badge-warning tag is-warning"
          >ADMIN</span>
        </p>
        <div class="card-actions justify-end">
          <button
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
              <path d="M19.045 7.401c.378-.378.586-.88.586-1.414s-.208-1.036-.586-1.414l-1.586-1.586c-.378-.378-.88-.586-1.414-.586s-1.036.208-1.413.585L4 13.585V18h4.413L19.045 7.401zm-3-3 1.587 1.585-1.59 1.584-1.586-1.585 1.589-1.584zM6 16v-1.585l7.04-7.018 1.586 1.586L7.587 16H6zm-2 4h16v2H4z" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
</style>
