function withOpacityValue(variable) {
  return ({ opacityValue }) => {
    if (opacityValue === undefined) {
      return `rgb(var(${variable}))`
    }
    return `rgb(var(${variable}) / ${opacityValue})`
  }
}

/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: 'class',
  content: [
    "./index.html",
    "./src/App.vue",
    "./src/main.ts",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        jelu_background: withOpacityValue('--jelu_background'),
        jelu_background_accent: withOpacityValue('--jelu_background_accent'),
        jelu_background_contrast: withOpacityValue('--jelu_background_contrast'),
        jelu_text_primary: withOpacityValue('--jelu_text_primary'),
        jelu_text_secondary: withOpacityValue('--jelu_text_secondary'),
        jelu_text_accent: withOpacityValue('--jelu_text_accent'),
        jelu_overlay: 'rgba(255, 255,255, 0.3)',
      },
    },
  },
  // daisyui: {
    // themes: [
      // "light",
      // "dark",
      // {
        // jelu: {
          // primary: "#f7f5d1",
          // secondary: "#aaaaaa",
          // accent: "#8D795B",
          // neutral: "#404040",
          // "base-100": "#262429",
          // "info": "#6191c2",
          // "success": "#CEB035",
          // "warning": "#ffad48",
          // "error": "#F87272",
          // "--rounded-box": "0rem", // border radius rounded-box utility class, used in card and other large boxes
          // "--rounded-btn": "0rem", // border radius rounded-btn utility class, used in buttons and similar element
          // "--rounded-badge": "0rem", // border radius rounded-badge utility class, used in badges and similar
          // "--animation-btn": "0.25s", // duration of animation when you click on button
          // "--animation-input": "0.2s", // duration of animation for inputs like checkbox, toggle, radio, etc
          // "--btn-text-case": "uppercase", // set default text transform for buttons
          // "--btn-focus-scale": "0.95", // scale transform of button when you focus on it
          // "--border-btn": "1px", // border width of buttons
          // "--tab-border": "1px", // border width of tabs
          // "--tab-radius": "0.5rem", // border radius of tabs
        // },
      // },
      // "cupcake", "bumblebee", "emerald", "corporate", "synthwave", "retro", "cyberpunk", "valentine", "halloween", "garden", "forest", "aqua", "lofi", "pastel", "fantasy", "wireframe", "black", "luxury", "dracula", "cmyk", "autumn", "business", "acid", "lemonade", "night", "coffee", "winter"
    // ],
  // },
  plugins: [
    require("@tailwindcss/typography"), 
    // require("daisyui"),
  ],
}
