<script setup lang="ts">
import { onMounted, Ref, ref } from 'vue'
import { Quote } from '../model/Quote'
import dataService from "../services/DataService"
import { Splide, SplideSlide } from '@splidejs/vue-splide';
import { useI18n } from 'vue-i18n'

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
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

try {
  getQuotes()
}
catch (err) {
  console.log("failed get quotes : " + err);
}

</script>

<template>
  <div class="divider typewriter capitalize">
    {{ t('home.quotes') }}
  </div>
  <Splide :options="{ rewind: true, autoplay: true, interval: 5000 }">
    <SplideSlide
      v-for="quote in quotes"
      :key="quote.content"
    >
      <div class="mb-8">
        <p>
          {{ quote.content }}
        </p>
        <br>
        <p>
          {{ quote.author }}
          <a
            v-if="quote.link"
            :href="quote.link"
            target="_blank"
            class="link"
          >{{ quote.origin }}</a>
        </p>
      </div>
    </SplideSlide>
  </Splide>
</template>

<style lang="scss" scoped>
</style>
