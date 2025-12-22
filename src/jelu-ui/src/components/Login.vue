<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { computed, reactive, Ref, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import { OAuth2ClientDto } from '../model/oauth-client-dto'
import dataService from "../services/DataService"
import { key } from '../store'
import urls from '../urls'
import { StringUtils } from '../utils/StringUtils'

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const store = useStore(key)
const form = reactive({'login' : '', 'password' : ''})
const loginValidation = ref('')
const passwordValidation = ref('')
const errorMessage = ref('')
const progress: Ref<boolean> = ref(false)
const providers: Ref<Array<OAuth2ClientDto>> = ref([])

const getOauthproviders = () => {
  dataService.oauth2Providers().then(res => {
    providers.value = res
  })
}

useTitle('Jelu | Login')

watch(form, (oldVal, newVal) => {
  if (displayInitialSetup.value) {
    validateInput()
  }
  else {
    validateInputLight()
  }
})

watch(passwordValidation, (oldVal, newVal) => {
  validateInput()
})

watch(loginValidation, (oldVal, newVal) => {
  validateInput()
})

const displayInitialSetup = computed(() => {
  return store.getters.getDisplayInitialSetup
})

const logUser = async () => {
  if (validateInputLight()) {
    progress.value = true
    try {
      await store.dispatch('authenticate', {"user" : form.login, "password" : form.password})
      progress.value = false
    } catch (error: any) {
      progress.value = false
      console.log('failed to auth user ' + error)
      errorMessage.value = error.message
    }
  }
}

const validateInputLight = (): boolean => {
  let isValid = true;
  errorMessage.value = ''
  if (!StringUtils.isNotBlank(form.login)) {
    errorMessage.value = errorMessage.value + " " + t('login.login_not_empty')
    isValid = false
  }
  if (!StringUtils.isNotBlank(form.password)) {
    errorMessage.value = errorMessage.value + " " + t('login.password_not_empty')
    isValid = false
  }
  return isValid
} 

const validateInput = (): boolean => {
  let isValid: boolean = validateInputLight()
  errorMessage.value = ''
  if (form.login !== loginValidation.value) {
    errorMessage.value = t('login.login_not_match')
    isValid = false;
  }
  if (form.password !== passwordValidation.value) {
    errorMessage.value = errorMessage.value + " " + t('login.password_not_match')
    isValid = false;
  }
  if (StringUtils.isNotBlank(form.login) && form.login.length < 3) {
    errorMessage.value = errorMessage.value + " " + t('login.login_length')
    isValid = false
  }
  if (StringUtils.isNotBlank(form.password) && form.password.length < 3) {
    errorMessage.value = errorMessage.value + " " + t('login.password_length')
    isValid = false
  }
  return isValid
} 

const createInitialUser = async () => {
  if (validateInput()) {
    progress.value = true
    try {
      await store.dispatch('createInitialUser', {"user" : form.login, "password" : form.password})
      progress.value = false
    } catch (error: any) {
      progress.value = false
      console.log('failed to create user ' + error)
      console.log(`failed to create user ${error.message}`)
  
      errorMessage.value = error.message
    }
  }
}

const oauth2Login = (provider: OAuth2ClientDto) => {
const url = `${urls.BASE_URL}/oauth2/authorization/${provider.registrationId}`
      const height = 600
      const width = 600
      const y = window.top!.outerHeight / 2 + window.top!.screenY - (height / 2)
      const x = window.top!.outerWidth / 2 + window.top!.screenX - (width / 2)
      window.open(url, 'oauth2Login',
        `toolbar=no,
        location=off,
        status=no,
        menubar=no,
        scrollbars=yes,
        resizable=yes,
        top=${y},
        left=${x},
        width=${height},
        height=${width}`,
      )
}
  
const submit = () => {
  if (displayInitialSetup.value) {
    createInitialUser()
  }
  else {
    logUser()
  }
}

getOauthproviders()
</script>

<template>
  <div class="flex flex-row justify-center items-center content-center">
    <div>
      <fieldset class="fieldset bg-base-200 border-base-300 rounded-box w-xs border p-4">
        <legend class="fieldset-legend capitalize">
          {{ t('login.login') }}
        </legend>
        <label class="label">{{ t('login.username') }}</label>
        <input
          v-model="form.login"
          type="text"
          maxlength="50"
          class="input focus:input-accent"
          :placeholder="t('login.username')"
        >
        <label
          v-if="displayInitialSetup"
          class="label"
        >
          {{ t('login.confirm_login') }}
        </label>
        <input
          v-if="displayInitialSetup"

          v-model="loginValidation"
          type="text"
          :placeholder="t('login.confirm_login')"
          maxlength="50"
          class="input focus:input-accent"
        >
        <label class="label">{{ t('login.password') }}</label>
        <input
          v-model="form.password"
          type="password"
          maxlength="150"
          class="input focus:input-accent" 
          :placeholder="t('login.password')"
        >
        <label
          v-if="displayInitialSetup"
        
          class="label"
        >
          {{ t('login.confirm_password') }}
        </label>
        <input
          v-if="displayInitialSetup"
          v-model="passwordValidation"
          type="password"
          :placeholder="t('login.confirm_password')"
          maxlength="150"
          class="input focus:input-accent"
          @keyup.enter="submit"
        >
        <p
          v-if="displayInitialSetup"
          class="control"
        >
          <button
            class="btn btn-warning mt-4 uppercase"
            :disabled="progress"
            @click="createInitialUser"
          >
            <span
              v-if="progress"
              class="loading loading-spinner"
            />
            {{ t('login.create_first_user') }}
          </button>
        </p>
        <p
          v-else
          class="control"
        >
          <button
            class="btn btn-success mt-4 capitalize"
            :disabled="progress"
            @click="logUser"
          >
            <span
              v-if="progress"
              class="loading loading-spinner"
            />
            {{ t('login.login') }}
          </button>
        </p>
        <p
          v-if="errorMessage"
          class="text-error"
        >
          {{ errorMessage }}
        </p>
        
        <div
          v-if="!displayInitialSetup"
          class="mt-3"
        >
          <button
            v-for="provider in providers"
            :key="provider.name"
            class="btn btn-info m-2 capitalize"
            :disabled="displayInitialSetup"
            @click="oauth2Login(provider)"
          >
            <span class="icon">
              <i :class="'mdi mdi-18px mdi-' + provider.registrationId" />
            </span>
            {{ t("labels.social_login", {provider: provider.registrationId}) }}
          </button>
        </div>
      </fieldset>
      <progress
        v-if="progress"
        class="animate-pulse progress progress-success mt-5"
      />
    </div>
  </div>
</template>

<style lang="scss" scoped>

</style>
