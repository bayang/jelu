import { useLocalStorage } from '@vueuse/core';
import { computed } from 'vue';

export default function useTypography() {

  const fontPreference = useLocalStorage("JL_FONT", "typewriter")

  const typographyClasses = computed(() => {
    if (fontPreference.value === 'cormorant') {
      return "cormorant font-bold"
    } else if (fontPreference.value === 'typewriter') {
      return "typewriter"
    }
      return ""
    })

    return {
      typographyClasses
    }
}
