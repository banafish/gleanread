import type { Session } from "@supabase/supabase-js";
import type { AuthSession } from "@/shared/models";
import { createId } from "@/shared/utils";
import { ensureSessionSeed, getCurrentSession, setCurrentSession } from "@/db/repositories/workspaceRepository";
import { hasSupabaseConfig, supabase } from "@/supabase/client";

export type OAuthProvider = "google" | "github";

export interface LoginPayload {
  email: string;
  password: string;
}

export interface SignupPayload extends LoginPayload {
  confirmPassword: string;
}

function toAuthSession(session: Session, fallbackEmail = "user@example.com"): AuthSession {
  return {
    userId: session.user.id,
    email: session.user.email ?? fallbackEmail,
    accessToken: session.access_token,
    refreshToken: session.refresh_token,
    provider: "supabase",
  };
}

async function persistSupabaseSession(session: Session, fallbackEmail?: string): Promise<AuthSession> {
  const authSession = toAuthSession(session, fallbackEmail);
  await ensureSessionSeed(authSession);
  return authSession;
}

export async function loadSession(): Promise<AuthSession | null> {
  if (hasSupabaseConfig && supabase) {
    const { data } = await supabase.auth.getSession();
    if (data.session?.user) {
      return persistSupabaseSession(data.session);
    }

    const storedSession = await getCurrentSession();
    if (storedSession?.provider === "supabase") {
      await setCurrentSession(null);
      return null;
    }
    return storedSession;
  }
  return getCurrentSession();
}

export async function signIn(payload: LoginPayload): Promise<AuthSession> {
  if (hasSupabaseConfig && supabase) {
    const { data, error } = await supabase.auth.signInWithPassword({
      email: payload.email,
      password: payload.password,
    });
    if (error) {
      throw new Error(error.message);
    }
    if (!data.session?.user) {
      throw new Error("未能获取 Supabase 会话。");
    }
    return persistSupabaseSession(data.session, payload.email);
  }

  const session: AuthSession = {
    userId: `local-${payload.email.toLowerCase().replace(/[^a-z0-9]+/g, "-") || createId("user")}`,
    email: payload.email,
    provider: "local",
  };
  await ensureSessionSeed(session);
  return session;
}

export async function signUp(payload: SignupPayload): Promise<AuthSession> {
  if (payload.password !== payload.confirmPassword) {
    throw new Error("两次输入的密码不一致。");
  }
  if (hasSupabaseConfig && supabase) {
    const { data, error } = await supabase.auth.signUp({
      email: payload.email,
      password: payload.password,
    });
    if (error) {
      throw new Error(error.message);
    }
    if (data.session?.user) {
      return persistSupabaseSession(data.session, payload.email);
    }
  }

  const session: AuthSession = {
    userId: `local-${payload.email.toLowerCase().replace(/[^a-z0-9]+/g, "-") || createId("user")}`,
    email: payload.email,
    provider: "local",
  };
  await ensureSessionSeed(session);
  return session;
}

export async function sendMagicLink(email: string): Promise<void> {
  if (hasSupabaseConfig && supabase) {
    const redirectTo = `${window.location.origin}/auth/callback`;
    const { error } = await supabase.auth.signInWithOtp({
      email,
      options: { emailRedirectTo: redirectTo },
    });
    if (error) {
      throw new Error(error.message);
    }
    return;
  }
  const session: AuthSession = {
    userId: `local-${email.toLowerCase().replace(/[^a-z0-9]+/g, "-") || createId("user")}`,
    email,
    provider: "local",
  };
  await ensureSessionSeed(session);
}

export async function signInWithOAuth(provider: OAuthProvider): Promise<void> {
  if (hasSupabaseConfig && supabase) {
    const redirectTo = `${window.location.origin}/auth/callback`;
    const { data, error } = await supabase.auth.signInWithOAuth({
      provider,
      options: {
        redirectTo,
      },
    });
    if (error) {
      throw new Error(error.message);
    }
    if (data.url) {
      window.location.assign(data.url);
      return;
    }
    throw new Error("未能获得 OAuth 跳转地址。");
  }

  const session: AuthSession = {
    userId: `local-${provider}-${createId("user")}`,
    email: `${provider}@gleanread.local`,
    provider: "local",
  };
  await ensureSessionSeed(session);
}

export async function completeCallback(): Promise<AuthSession | null> {
  if (hasSupabaseConfig && supabase) {
    const { data, error } = await supabase.auth.getSession();
    if (error) {
      throw new Error(error.message);
    }
    if (data.session?.user) {
      return persistSupabaseSession(data.session);
    }
  }
  return getCurrentSession();
}

export async function signOut(): Promise<void> {
  if (hasSupabaseConfig && supabase) {
    await supabase.auth.signOut();
  }
  await setCurrentSession(null);
}
