<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { onMounted } from 'vue'
import Avatar from 'vue-avatar-sdh'
import { useStore } from 'vuex'
import { key } from '../store'
import { useI18n } from 'vue-i18n'

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | User page')

const store = useStore(key)

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
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
</style>
