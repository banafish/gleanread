import React from "react";
import ReactDOM from "react-dom/client";
import { App } from "@/app/App";
import { AuthProvider } from "@/app/providers/AuthProvider";
import { QueryProvider } from "@/app/providers/QueryProvider";
import { ThemeProvider } from "@/app/providers/ThemeProvider";
import "reactflow/dist/style.css";
import "@/styles/globals.css";

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <QueryProvider>
      <AuthProvider>
        <ThemeProvider>
          <App />
        </ThemeProvider>
      </AuthProvider>
    </QueryProvider>
  </React.StrictMode>
);
