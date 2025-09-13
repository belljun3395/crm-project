/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
    "./src/**/*.stories.{js,jsx,ts,tsx}",
    "./.storybook/**/*.{js,jsx,ts,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {},
  },
  safelist: [
    // 동적 클래스들을 위한 safelist
    'bg-red-500',
    'bg-green-500',
    'bg-blue-500',
    'text-red-500',
    'text-green-500',
    'text-blue-500',
    'border-red-500',
    'border-green-500',
    'border-blue-500',
  ],
  plugins: [],
}