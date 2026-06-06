/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        trigger:     { DEFAULT: '#16a34a', light: '#bbf7d0' },
        action:      { DEFAULT: '#ea580c', light: '#fed7aa' },
        processor:   { DEFAULT: '#2563eb', light: '#bfdbfe' },
        transformer: { DEFAULT: '#7c3aed', light: '#ddd6fe' },
      },
    },
  },
  plugins: [],
}
