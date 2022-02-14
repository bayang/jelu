<script setup lang="ts">
import { computed, onMounted, Ref, ref } from 'vue'
import { Carousel, Navigation, Pagination, Slide } from 'vue3-carousel'
import { useStore } from 'vuex'
import { Quote } from '../model/Quote'
import dataService from "../services/DataService"
import { key } from '../store'

const store = useStore(key)

const isLogged = computed(() => {
    return store.state.isLogged
  })

const quotes: Ref<Array<Quote>> = ref([]);

const getQuotes = async () => {
  try {
    let res = await dataService.quotes()
    if (res.length > 0) {
      quotes.value = res
    }
    else {
      quotes.value = await dataService.randomQuotes()
    }
  } catch (error) {
    console.log("failed get books : " + error);
  }
};

onMounted(() => {
  console.log("Component is mounted!");
    
});

if (isLogged.value) {
    try {
      getQuotes()
    }
      catch (err) {
        console.log("failed get quotes : " + err);
    }
}

</script>

<template>
  <div v-if="isLogged">
    <div class="divider typewriter">
      Quotes
    </div>
    <carousel
      :autoplay="5000"
      :wrap-around="true"
      :pause-autoplay-on-hover="true"
      :transition="500"
    >
      <slide
        v-for="quote in quotes"
        :key="quote.content"
      >
        <div>
          <p class="is-size-7">
            {{ quote.content }}
          </p>
          <br>
          <p>
            {{ quote.author }}
            <a
              v-if="quote.link"
              :href="quote.link"
              target="_blank"
            >{{ quote.origin }}
            </a>
          </p>
        </div>
      </slide>

      <template #addons="{ slidesCount }">
        <navigation v-if="slidesCount > 1" />
        <pagination v-if="slidesCount > 1" />
      </template>
    </carousel>
  </div>
</template>

<style lang="scss" scoped>

</style>
