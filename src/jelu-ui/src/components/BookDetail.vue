<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const props = defineProps<{ bookId: string }>()

const route = useRoute()
const book = ref()
const count = ref(0)

watch(() => props.bookId, (newValue, oldValue) => {
  console.log('The new counter value is: ' + props.bookId)
})


console.log('The id value is: ' + props.bookId)

</script>

<template>
  <h1>Book is {{ bookId }}</h1>

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
