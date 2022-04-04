<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { onMounted } from 'vue'
import Avatar from 'vue-avatar-sdh'
import { useStore } from 'vuex'
import { key } from '../store'
import { themeChange } from 'theme-change'

useTitle('Jelu | User settings')

const store = useStore(key)

const themes = [
        "light",
        "dark",
        "jelu",
        "cupcake",
        "bumblebee",
        "emerald",
        "corporate",
        "synthwave",
        "retro",
        "cyberpunk",
        "valentine",
        "halloween",
        "garden",
        "forest",
        "aqua",
        "lofi",
        "pastel",
        "fantasy",
        "wireframe",
        "black",
        "luxury",
        "dracula",
        "cmyk",
        "autumn",
        "business",
        "acid",
        "lemonade",
        "night",
        "coffee",
        "winter",
      ];

onMounted(() => {
  console.log("Component is mounted!");
  themeChange(false);
});


</script>

<template>
  <div class="w-fit form-control">
    <label class="label">
      <span class="label-text">Choose a theme : </span>
    </label>
    <select
      class="select select-bordered select-primary"
      data-choose-theme
    >
      <option value="">
        Default
      </option>
      <option
        v-for="theme in themes"
        :key="theme"
        :value="theme"
      >
        {{ theme }}
      </option>
    </select>
    <label class="label">
      <span class="label-text-alt font-bold">Beta. Default theme is 'dark'</span>
    </label>
  </div>
</template>

<style lang="scss" scoped>

</style>
