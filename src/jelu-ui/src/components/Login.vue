<script setup lang="ts">
import { computed, onBeforeMount, onMounted, reactive, ref, watch } from 'vue'
import { useStore } from 'vuex'
import { key } from '../store'
import { StringUtils } from '../utils/StringUtils'

const store = useStore(key)
const form = reactive({'login' : '', 'password' : ''})
const loginValidation = ref('')
const passwordValidation = ref('')
const errorMessage = ref('')

watch(form, (oldVal, newVal) => {
  validateInput()
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
  if (validateInput()) {
    try {
      await store.dispatch('authenticate', {"user" : form.login, "password" : form.password})
    } catch (error: any) {
      console.log('failed to auth user ' + error)
      console.log(`failed to auth user ${error.message}`)
  
      errorMessage.value = error.message
    }
  }
}

const validateInput = (): boolean => {
  let isValid: boolean = true;
  errorMessage.value = ''
  if (form.login !== loginValidation.value) {
    errorMessage.value = 'login fields do not match'
    isValid = false;
  }
  if (form.password !== passwordValidation.value) {
    errorMessage.value = errorMessage.value + ' password fields do not match'
    isValid = false;
  }
  if (!StringUtils.isNotBlank(form.login)) {
    errorMessage.value = errorMessage.value + ' login cannot be empty'
    isValid = false
  }
  if (!StringUtils.isNotBlank(form.password)) {
    errorMessage.value = errorMessage.value + ' password cannot be empty'
    isValid = false
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
    <o-field label="Login" class="control">
      <o-input type="text" value="john" maxlength="50" v-model="form.login"> </o-input>
    </o-field>
    </div>
    <div class="field">
    <o-field label="Confirm Login" class="control">
      <o-input type="text" value="john" maxlength="50" v-model="loginValidation"> </o-input>
    </o-field>
    </div>
    <div class="field">
    <o-field label="Password"  class="control">
      <o-input @keyup.enter="submit" value="123" type="password" maxlength="30" v-model="form.password" passwordReveal></o-input>
    </o-field>
    </div>
    <div class="field">
    <o-field label="Confirm Password"  class="control">
      <o-input @keyup.enter="submit" value="123" type="password" maxlength="30" v-model="passwordValidation" passwordReveal></o-input>
    </o-field>
    </div>
    <div class="field">
  <p class="control" v-if="isInitialSetup">
    <button @click="createInitialUser" class="button is-warning">
      Create First User
    </button>
  </p>
  <p class="control" v-else>
    <button @click="logUser" class="button is-primary">
      Login
    </button>
  </p>
  <p v-if="errorMessage" class="has-text-danger">{{errorMessage}}</p>
</div>

  <p>login {{form.login}},  pw  {{form.password}}</p>

    </div>
    </div>
</template>

<style lang="scss" scoped>


</style>
