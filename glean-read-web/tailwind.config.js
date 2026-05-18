/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        app: {
          bg: "rgb(var(--app-bg) / <alpha-value>)",
          surface: "rgb(var(--app-surface) / <alpha-value>)",
          surface2: "rgb(var(--app-surface-2) / <alpha-value>)",
          border: "rgb(var(--app-border) / <alpha-value>)",
          text: "rgb(var(--app-text) / <alpha-value>)",
          muted: "rgb(var(--app-muted) / <alpha-value>)",
          accent: "rgb(var(--app-accent) / <alpha-value>)",
          accent2: "rgb(var(--app-accent-2) / <alpha-value>)",
          danger: "rgb(var(--app-danger) / <alpha-value>)",
          success: "rgb(var(--app-success) / <alpha-value>)",
        },
      },
      boxShadow: {
        panel: "0 12px 30px rgba(15, 23, 42, 0.08)",
      },
      borderRadius: {
        panel: "14px",
      },
    },
  },
  plugins: [],
};
