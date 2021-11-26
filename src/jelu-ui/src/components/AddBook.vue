<script setup lang="ts">
import { computed, onMounted, reactive, Ref, ref } from 'vue'
import { useStore } from 'vuex'
import dataService from '../services/DataService'
import { key } from '../store'

// defineProps<{ msg: string }>()
const store = useStore(key)
const form = reactive({
  'title' : '', 
  'summary' : '', 
  'isbn10': '', 
  'isbn13': '',
  'publisher': '',
  'pageCount' : '',
  'publishedDate' : ''
  })
const errorMessage = ref('')
// load existing authors from db here
const data: Ref<Array<string>>  = ref(["jacques"])
let filteredAuthors: Ref<Array<string>> = ref(data)
let authors: Ref<Array<string>> = ref([])
const importBook = async () => {
  console.log('import book')
}

onMounted(() => {
            console.log(`form data `)
            console.log(form)
        })

function getFilteredAuthors(text: string) {
    filteredAuthors.value = data.value.filter(option => {
            return (
              option
                .toString()
                .toLowerCase()
                .indexOf(text.toLowerCase()) >= 0
            )
          })
        }


</script>

<template>
  <h1 class="title">Add book</h1>
  <section>
    <div class="field">
    <o-field label="Title">
      <o-input v-model="form.title"></o-input>
    </o-field>
    </div>

<div class="field">
<o-field label="Authors">
      <o-inputitems
        v-model="authors"
        :data="filteredAuthors"
        autocomplete
        :allow-new="true"
        :open-on-focus="true"
        iconPack="mdi"
        icon="account-plus"
        placeholder="Add an author"
        @typing="getFilteredAuthors"
      >
      </o-inputitems>
    </o-field>
    <p class="content"><b>Items:</b> {{ authors }}</p>
</div>
    <div class="field">
    <o-field label="Summary">
      <o-input maxlength="200" type="textarea" v-model="form.summary"></o-input>
    </o-field>
    </div>
    
    
    <div class="field">
  <p class="control">
    <button @click="importBook" class="button is-success">
      Import book
    </button>
  </p>
  <p v-if="errorMessage" class="has-text-danger">{{errorMessage}}</p>
</div>

  <p>login {{form.title}},  pw  {{form.summary}}</p>

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
