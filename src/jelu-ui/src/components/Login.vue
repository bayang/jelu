<script setup lang="ts">
import { computed, onBeforeMount, onMounted, reactive, ref } from 'vue'
import { useStore } from 'vuex'
import { key } from '../store'

// defineProps<{ msg: string }>()
const store = useStore(key)
const form = reactive({'login' : '', 'password' : ''})
const errorMessage = ref('')
const isInitialSetup = computed(() => {
    return store.state.isInitialSetup
  })
const logUser = async () => {
  try {
    // await dataService.authenticateUser(form.login, form.password)
    await store.dispatch('authenticate', {"user" : form.login, "password" : form.password})
  } catch (error: any) {
    console.log('failed to auth user ' + error)
    console.log(`failed to auth user ${error.message}`)

    errorMessage.value = error.message
  }
}
const createInitialUser = async () => {
  try {
    await store.dispatch('createInitialUser', {"user" : form.login, "password" : form.password})
  } catch (error: any) {
    console.log('failed to create user ' + error)
    console.log(`failed to create user ${error.message}`)

    errorMessage.value = error.message
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
  <section>
    <div class="field">
    <o-field label="Login" class="control">
      <o-input type="text" value="john@" maxlength="50" v-model="form.login"> </o-input>
    </o-field>
    </div>
    <div class="field">
    <o-field label="Password"  class="control">
      <o-input @keyup.enter="submit" value="123" type="password" maxlength="30" v-model="form.password"></o-input>
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

  </section>
</template>

<style scoped>
a {
  color: #42b983;
}

label {
  margin: 0 0.5em;
  font-weight: bold;
}

code {
  background-color: #eee;
  padding: 2px 4px;
  border-radius: 4px;
  color: #304455;
}
</style>
