<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useStore } from 'vuex'
import { key } from '../store'
import { StringUtils } from '../utils/StringUtils'

const store = useStore(key)
const form = reactive({'login' : '', 'password' : ''})
const loginValidation = ref('')
const passwordValidation = ref('')
const errorMessage = ref('')
import { useTitle } from '@vueuse/core'

useTitle('Jelu | Login')

watch(form, (oldVal, newVal) => {
  if (isInitialSetup.value) {
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

const isInitialSetup = computed(() => {
    return store.state.isInitialSetup
  })
const logUser = async () => {
  if (validateInputLight()) {
    try {
      await store.dispatch('authenticate', {"user" : form.login, "password" : form.password})
    } catch (error: any) {
      console.log('failed to auth user ' + error)
      console.log(`failed to auth user ${error.message}`)
  
      errorMessage.value = error.message
    }
  }
}

const validateInputLight = (): boolean => {
  let isValid = true;
  errorMessage.value = ''
  if (!StringUtils.isNotBlank(form.login)) {
    errorMessage.value = errorMessage.value + ' login cannot be empty'
    isValid = false
  }
  if (!StringUtils.isNotBlank(form.password)) {
    errorMessage.value = errorMessage.value + ' password cannot be empty'
    isValid = false
  }
  return isValid
} 

const validateInput = (): boolean => {
  let isValid: boolean = validateInputLight()
  errorMessage.value = ''
  if (form.login !== loginValidation.value) {
    errorMessage.value = 'login fields do not match'
    isValid = false;
  }
  if (form.password !== passwordValidation.value) {
    errorMessage.value = errorMessage.value + ' password fields do not match'
    isValid = false;
  }
  if (StringUtils.isNotBlank(form.login) && form.login.length < 3) {
    errorMessage.value = errorMessage.value + ' login must be 3 chars long minimum'
    isValid = false
  }
  if (StringUtils.isNotBlank(form.password) && form.password.length < 3) {
    errorMessage.value = errorMessage.value + ' password must be 3 chars long minimum'
    isValid = false
  }
  return isValid
} 

const createInitialUser = async () => {
  if (validateInput()) {
    try {
      await store.dispatch('createInitialUser', {"user" : form.login, "password" : form.password})
    } catch (error: any) {
      console.log('failed to create user ' + error)
      console.log(`failed to create user ${error.message}`)
  
      errorMessage.value = error.message
    }
  }
}
onMounted(() => {
            console.log('Component login is mounted in script setup!')
            console.log(`form data ${form}`)
        })

const submit = () => {
  if (isInitialSetup.value) {
    createInitialUser()
  }
  else {
    logUser()
  }
}
</script>

<template>
  <div class="columns  is-centered">
    <div class="column is-centered is-half">
      <div class="field">
        <o-field
          label="Login"
          class="control"
        >
          <o-input
            v-model="form.login"
            type="text"
            value="john"
            maxlength="50"
          />
        </o-field>
      </div>
      <div
        v-if="isInitialSetup"
        class="field"
      >
        <o-field
          label="Confirm Login"
          class="control"
        >
          <o-input
            v-model="loginValidation"
            type="text"
            value="john"
            maxlength="50"
          />
        </o-field>
      </div>
      <div class="field">
        <o-field
          label="Password"
          class="control"
        >
          <o-input
            v-model="form.password"
            value="123"
            type="password"
            maxlength="30"
            password-reveal
            @keyup.enter="submit"
          />
        </o-field>
      </div>
      <div
        v-if="isInitialSetup"
        class="field"
      >
        <o-field
          label="Confirm Password"
          class="control"
        >
          <o-input
            v-model="passwordValidation"
            value="123"
            type="password"
            maxlength="30"
            password-reveal
            @keyup.enter="submit"
          />
        </o-field>
      </div>
      <div class="field">
        <p
          v-if="isInitialSetup"
          class="control"
        >
          <button
            class="button is-warning"
            @click="createInitialUser"
          >
            Create First User
          </button>
        </p>
        <p
          v-else
          class="control"
        >
          <button
            class="button is-primary"
            @click="logUser"
          >
            Login
          </button>
        </p>
        <p
          v-if="errorMessage"
          class="has-text-danger"
        >
          {{ errorMessage }}
        </p>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>


</style>
