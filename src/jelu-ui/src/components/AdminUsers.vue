<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next"
import { useTitle } from '@vueuse/core'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import dataService from "../services/DataService"
import { ObjectUtils } from "../utils/ObjectUtils"

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | Users admin')

const oruga = useOruga()

const form = ref({'login' : '', 'password' : '', 'password_confirm': '', 'admin': false})

async function createUser() {
  console.log("create user")
  console.log(form)
  try {
    await dataService.createUser({"login" : form.value.login, "password": form.value.password, "isAdmin" : form.value.admin})
    ObjectUtils.toast(oruga, "success", t('admin_user.user_saved', {name : form.value.login}), 4000)
    form.value.password = ''
    form.value.password_confirm = ''
    form.value.login = ''
    form.value.admin = false
  } catch (err: any) {
    console.log('error creating user')
    console.log(err)
  }
}

const isValid = computed(() => {
  return form.value.login.length >=3 && form.value.password.length >=3 && form.value.password_confirm.length >=3 && form.value.password === form.value.password_confirm
})

</script>

<template>
  <div class="grid grid-cols-1 justify-center justify-items-center justify-self-center">
    <h1 class="typewriter text-2xl mb-3 capitalize">
      {{ t('admin_user.create_user') }} :
    </h1>
    <div class="flex flex-row justify-center basis-10/12 sm:basis-1/3">
      <div class="">
        <fieldset class="fieldset">
          <label class="label">{{ t('login.username') }}</label>
          <input
            v-model="form.login"
            type="text"
            class="input validator"
            placeholder="joe123"
            required
            minlength="3"
          >
          <p class="validator-hint hidden">
            {{ t('login.login_length') }}
          </p>
        </fieldset>

        <label class="fieldset">
          <span class="label capitalize">{{ t('admin_user.password') }}</span>
          <input
            v-model="form.password"
            type="password"
            class="input validator"
            :placeholder="t('admin_user.password')"
            required
            minlength="3"
          >
          <span class="validator-hint hidden">{{ t('login.password_length') }}</span>
        </label>
        <label class="fieldset">
          <span class="label capitalize">{{ t('admin_user.password_confirm') }}</span>
          <input
            v-model="form.password_confirm"
            type="password"
            class="input validator"
            :placeholder="t('admin_user.password_confirm')"
            required
            minlength="3"
          >
          <span class="validator-hint hidden">{{ t('login.password_length') }}</span>
        </label>
        <label class="fieldset"> 
          <span class="label capitalize">{{ t('admin_user.admin_help') }}</span>
          <input
            v-model="form.admin"
            type="checkbox"
            class="checkbox checkbox-success"
          >
        </label>

        <button
          class="btn btn-accent mt-4"
          type="submit"
          :disabled="! isValid"
          @click="createUser"
        >
          {{ t('admin_user.create_user') }}
        </button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
</style>
