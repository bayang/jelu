<script setup lang="ts">
import { computed, onBeforeMount, onMounted, reactive, ref } from 'vue'
import { useStore } from 'vuex'
import dataService from '../services/DataService'
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
console.log('Component is created!')
onMounted(() => {
            console.log('Component login is mounted in script setup!')
            console.log(`form data ${form}`)
        })
onBeforeMount(() => {
  console.log("onbeforemount script setup login")
})
</script>

<template>
  <h1 class="title">Login</h1>
  <section>
    <div class="field">
    <o-field label="Email" class="control">
      <o-input type="email" value="john@" maxlength="30" v-model="form.login"> </o-input>
    </o-field>
    </div>
    <div class="field">
    <o-field label="Password"  class="control">
      <o-input value="123" type="password" maxlength="30" v-model="form.password"></o-input>
    </o-field>
    </div>
    <div class="field">
  <p class="control" v-if="isInitialSetup">
    <button @click="createInitialUser" class="button is-warning">
      Create First User
    </button>
  </p>
  <p class="control" v-else>
    <button @click="logUser" class="button is-success">
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
