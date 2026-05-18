import { useEffect, type ReactNode } from "react";
import { useWorkbenchStore } from "@/features/workbench/workbenchStore";

function resolveTheme(mode: "light" | "dark" | "system"): "light" | "dark" {
  if (mode !== "system") {
    return mode;
  }
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const themeMode = useWorkbenchStore((state) => state.themeMode);

  useEffect(() => {
    const theme = resolveTheme(themeMode);
    document.documentElement.dataset.theme = theme;
  }, [themeMode]);

  useEffect(() => {
    const listener = () => {
      if (useWorkbenchStore.getState().themeMode === "system") {
        document.documentElement.dataset.theme = resolveTheme("system");
      }
    };
    const media = window.matchMedia("(prefers-color-scheme: dark)");
    media.addEventListener("change", listener);
    return () => media.removeEventListener("change", listener);
  }, []);

  return children;
}
