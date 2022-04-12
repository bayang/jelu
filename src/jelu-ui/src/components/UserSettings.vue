<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { onMounted, watch } from 'vue'
import Avatar from 'vue-avatar-sdh'
import { useStore } from 'vuex'
import { key } from '../store'
import { themeChange } from 'theme-change'
import { useI18n } from 'vue-i18n'
import { useLocalStorage } from '@vueuse/core'

useTitle('Jelu | User settings')

const store = useStore(key)
const { t, locale, availableLocales } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

availableLocales.forEach(locale => {
      console.log(`${locale} locale `)
    })

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
  themeChange(false);
});

watch(() => locale.value,(newValue, oldValue) => {
  console.log('locale changed: ' + newValue + " " + oldValue)
  const storedLanguage = useLocalStorage("jelu_language", oldValue)
  storedLanguage.value = newValue
})

</script>

<template>
  <div class="w-fit form-control">
    <label class="label">
      <span class="label-text text-lg">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          class="h-6 w-6"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01"
          />
        </svg>
        {{ t('settings.pick_theme') }} : 
      </span>
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
      <span class="label-text-alt font-bold">{{ t('settings.theme_warning') }}</span>
    </label>
  </div>
  <div class="w-fit form-control">
    <label class="label">
      <span class="label-text text-lg">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          class="h-6 w-6"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth="2"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M3 5h12M9 3v2m1.048 9.5A18.022 18.022 0 016.412 9m6.088 9h7M11 21l5-10 5 10M12.751 5C11.783 10.77 8.07 15.61 3 18.129"
          />
        </svg>
        {{ t('settings.pick_language') }} : </span>
    </label>
    <select
      v-model="locale"
      class="select select-bordered select-accent"
    >
      <option
        disabled
        selected
      >
        {{ t('settings.pick_language') }}
      </option>
      <option
        v-for="loc in availableLocales"
        :key="loc"
        :value="loc"
      >
        {{ loc }}
      </option>
    </select>
  </div>
</template>

<style lang="scss" scoped>

</style>
