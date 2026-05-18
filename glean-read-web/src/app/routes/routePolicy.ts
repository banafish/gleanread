export type RouteDecision =
  | { kind: "loading" }
  | { kind: "render" }
  | { kind: "redirect"; to: string; replace: true };

export function resolveProtectedRoute(hasSession: boolean, isLoading: boolean): RouteDecision {
  if (isLoading) {
    return { kind: "loading" };
  }
  if (!hasSession) {
    return { kind: "redirect", to: "/login", replace: true };
  }
  return { kind: "render" };
}

export function resolveLoginRoute(hasSession: boolean, isLoading: boolean): RouteDecision {
  if (!isLoading && hasSession) {
    return { kind: "redirect", to: "/app", replace: true };
  }
  return { kind: "render" };
}
