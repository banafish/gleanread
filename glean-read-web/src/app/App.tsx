import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { HomeRoute } from "@/app/routes/HomeRoute";
import { LoginRoute } from "@/app/routes/LoginRoute";
import { AuthCallbackRoute } from "@/app/routes/AuthCallbackRoute";
import { AppRoute } from "@/app/routes/AppRoute";
import { PreviewRoute } from "@/app/previews/PreviewRoute";
import { WorkbenchDragDropLayer } from "@/features/workbench/components/WorkbenchDragDropLayer";

export function App() {
  return (
    <WorkbenchDragDropLayer>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<HomeRoute />} />
          <Route path="/login" element={<LoginRoute />} />
          <Route path="/auth/callback" element={<AuthCallbackRoute />} />
          <Route path="/preview" element={<PreviewRoute />} />
          <Route path="/app/*" element={<AppRoute />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </WorkbenchDragDropLayer>
  );
}
