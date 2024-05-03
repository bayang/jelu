<script setup lang="ts">
import { setErrors } from '@formkit/vue'
import { useOruga } from "@oruga-ui/oruga-next"
import { useTitle } from '@vueuse/core'
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import dataService from "../services/DataService"
import { ObjectUtils } from "../utils/ObjectUtils"

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | Users admin')

const oruga = useOruga()

const form = ref({'login' : '', 'password' : '', 'admin': false})

async function createUser(user: any) {
  console.log("create user")
  console.log(user)
  try {
    setErrors('create-user-form', [], undefined)
    await dataService.createUser({"login" : user.login, "password": user.password, "isAdmin" : user.admin})
    ObjectUtils.toast(oruga, "success", t('admin_user.user_saved', {name : user.login}), 4000)
  } catch (err: any) {
    setErrors('create-user-form', [], err.message)
  }
}

</script>

<template>
  <div class="grid grid-cols-1 justify-center justify-items-center justify-self-center">
    <h1 class="typewriter text-2xl mb-3 capitalize">
      {{ t('admin_user.create_user') }} :
    </h1>
    <div class="flex flex-row justify-center basis-10/12 sm:basis-1/3">
      <div class="">
        <FormKit
          id="create-user-form"
          v-slot="{ state: { valid } }"
          v-model="form"
          type="form"
          :actions="false"
          message-class="text-error-content"
          messages-class="alert alert-error mt-2"
          @submit="createUser"
        >
          <FormKit
            type="text"
            name="login"
            :label="t('admin_user.login')"
            placeholder="joe123"
            validation="required|length:3"
          />
          <FormKit
            type="password"
            name="password"
            :label="t('admin_user.password')"
            validation="required|length:3"
            :placeholder="t('admin_user.password')"
          />
          <FormKit
            type="password"
            name="password_confirm"
            :label="t('admin_user.password_confirm')"
            :placeholder="t('admin_user.password_confirm')"
            validation="required|confirm"
          />
          <FormKit
            type="checkbox"
            :help="t('admin_user.admin_help')"
            :label="t('admin_user.admin')"
            name="admin"
          />
          <FormKit
            type="submit"
            :disabled="!valid"
            input-class="btn-accent"
          >
            {{ t('admin_user.create_user') }}
          </FormKit>
        </FormKit>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
</style>
