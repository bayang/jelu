function withOpacityValue(variable) {
  return ({ opacityValue }) => {
    if (opacityValue === undefined) {
      return `rgb(var(${variable}))`
    }
    return `rgb(var(${variable}) / ${opacityValue})`
  }
}

/** @type {import("@types/tailwindcss/tailwind-config").TailwindConfig } */
module.exports = {
  darkMode: 'class',
  content: [
    "./index.html",
    "./src/App.vue",
    "./src/main.ts",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    // colors: {
    //   mytest: withOpacityValue('--jelu_test'),
    //   // jelu_background: withOpacityValue('--jelu-background'),
    //   // jelu_background_accent: withOpacityValue('--jelu_background_accent'),
    //   // jelu_background_contrast: withOpacityValue('--jelu_background_contrast'),
    //   // jelu_text_primary: withOpacityValue('--jelu_text_primary'),
    //   // jelu_text_secondary: withOpacityValue('--jelu_text_secondary'),
    //   // jelu_text_accent: withOpacityValue('--jelu_text_accent'),
    // },
    extend: {
      colors: {
        jelu_background: withOpacityValue('--jelu_background'),
        jelu_background_accent: withOpacityValue('--jelu_background_accent'),
        jelu_background_contrast: withOpacityValue('--jelu_background_contrast'),
        jelu_text_primary: withOpacityValue('--jelu_text_primary'),
        jelu_text_secondary: withOpacityValue('--jelu_text_secondary'),
        jelu_text_accent: withOpacityValue('--jelu_text_accent'),
      },
    },
  },
  plugins: [require("@tailwindcss/typography"), require("daisyui")],
}
