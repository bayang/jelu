<script setup lang="ts">
import { computed, onMounted, reactive, Ref, ref, watch } from 'vue'
import { useStore } from 'vuex'
import { key } from '../store'
import { StringUtils } from '../utils/StringUtils'
import { useI18n } from 'vue-i18n'
import { useTitle } from '@vueuse/core'

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
onMounted(() => {
            console.log(`form data ${form}`)
        })

const submit = () => {
  if (displayInitialSetup.value) {
    createInitialUser()
  }
  else {
    logUser()
  }
}
</script>

<template>
  <div class="flex flex-row justify-center">
    <div class="basis-10/12 sm:basis-1/3">
      <div class="field">
        <label class="label">
          <span class="label-text font-semibold capitalize">{{ t('login.login') }}</span>
        </label>

        <o-input
          v-model="form.login"
          type="text"
          maxlength="50"
          class="input focus:input-accent"
        />
      </div>
      <div
        v-if="displayInitialSetup"
        class="field"
      >
        <label class="label">
          <span class="label-text font-semibold capitalize">{{ t('login.confirm_login') }}</span>
        </label>
        <o-input
          v-model="loginValidation"
          type="text"
          maxlength="50"
          class="input focus:input-accent"
        />
      </div>
      <div class="field">
        <label class="label">
          <span class="label-text font-semibold capitalize">{{ t('login.password') }}</span>
        </label>
        <o-input
          v-model="form.password"
          type="password"
          maxlength="150"
          password-reveal
          class="input focus:input-accent"
          @keyup.enter="submit"
        />
      </div>
      <div
        v-if="displayInitialSetup"
        class="field"
      >
        <label class="label">
          <span class="label-text font-semibold capitalize">{{ t('login.confirm_password') }}</span>
        </label>
        <o-input
          v-model="passwordValidation"
          type="password"
          maxlength="150"
          password-reveal
          class="input focus:input-accent"
          @keyup.enter="submit"
        />
      </div>
      <div class="field">
        <p
          v-if="displayInitialSetup"
          class="control"
        >
          <button
            class="btn btn-warning mt-2 uppercase"
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
            class="btn btn-success mt-2 uppercase"
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
      </div>
      <progress
        v-if="progress"
        class="animate-pulse progress progress-success mt-5"
      />
    </div>
  </div>
</template>

<style lang="scss" scoped>

</style>
