import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import type { AuthSession } from "@/shared/models";
import { completeCallback, loadSession, sendMagicLink, signIn, signInWithOAuth, signOut, signUp, type OAuthProvider } from "@/supabase/auth";
import { getWorkspaceSnapshot } from "@/db/repositories/workspaceRepository";
import { useWorkbenchStore } from "@/features/workbench/workbenchStore";

export interface AuthContextValue {
  session: AuthSession | null;
  isLoading: boolean;
  signInWithPassword: (email: string, password: string) => Promise<void>;
  signUpWithPassword: (email: string, password: string, confirmPassword: string) => Promise<void>;
  sendMagicLink: (email: string) => Promise<void>;
  signInWithOAuth: (provider: OAuthProvider) => Promise<void>;
  completeAuthCallback: () => Promise<void>;
  signOut: () => Promise<void>;
  refreshWorkspace: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

async function hydrateWorkspace(session: AuthSession | null): Promise<void> {
  const workbench = useWorkbenchStore.getState();
  if (!session) {
    workbench.resetUiForUser();
    return;
  }
  await workbench.loadPreferences(session.userId);
  const snapshot = await getWorkspaceSnapshot(session.userId, { seedIfEmpty: session.provider === "local" });
  workbench.setUserId(session.userId);
  workbench.hydrateWorkspace(session.userId, snapshot);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const refreshWorkspace = useCallback(async () => {
    await hydrateWorkspace(session);
  }, [session]);

  useEffect(() => {
    let alive = true;
    void (async () => {
      try {
        const nextSession = await loadSession();
        if (!alive) {
          return;
        }
        setSession(nextSession);
        await hydrateWorkspace(nextSession);
      } finally {
        if (alive) {
          setIsLoading(false);
        }
      }
    })();
    return () => {
      alive = false;
    };
  }, []);

  useEffect(() => {
    void hydrateWorkspace(session);
  }, [session]);

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      isLoading,
      signInWithPassword: async (email, password) => {
        const nextSession = await signIn({ email, password });
        setSession(nextSession);
        await hydrateWorkspace(nextSession);
      },
      signUpWithPassword: async (email, password, confirmPassword) => {
        const nextSession = await signUp({ email, password, confirmPassword });
        setSession(nextSession);
        await hydrateWorkspace(nextSession);
      },
      sendMagicLink: async (email) => {
        await sendMagicLink(email);
        const nextSession = await loadSession();
        if (nextSession) {
          setSession(nextSession);
          await hydrateWorkspace(nextSession);
        }
      },
      signInWithOAuth: async (provider) => {
        await signInWithOAuth(provider);
        const nextSession = await loadSession();
        if (nextSession) {
          setSession(nextSession);
          await hydrateWorkspace(nextSession);
        }
      },
      completeAuthCallback: async () => {
        const nextSession = await completeCallback();
        setSession(nextSession);
        await hydrateWorkspace(nextSession);
      },
      signOut: async () => {
        await signOut();
        setSession(null);
        await hydrateWorkspace(null);
      },
      refreshWorkspace: async () => {
        await hydrateWorkspace(session);
      },
    }),
    [isLoading, session]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}
