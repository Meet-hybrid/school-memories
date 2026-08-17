import type {
  AchievementDto,
  AdminStats,
  Announcement,
  AuthResponse,
  ChallengeTimeline,
  ClassSet,
  CommentDto,
  DayDetail,
  LeaderboardEntry,
  MemoryDto,
  NotificationDto,
  PageResponse,
  School,
  UserDto,
} from './types';

const TOKEN_KEY = 'keepsake.token';

export function getToken(): string | null {
  if (typeof window === 'undefined') return null;
  return window.localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null) {
  if (typeof window === 'undefined') return;
  if (token) window.localStorage.setItem(TOKEN_KEY, token);
  else window.localStorage.removeItem(TOKEN_KEY);
}

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  const token = getToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (options.body && typeof options.body === 'string' && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const res = await fetch(path, { ...options, headers, cache: 'no-store' });

  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      if (body?.message) message = body.message;
      else if (body?.fieldErrors?.length) message = body.fieldErrors.join(', ');
    } catch {
      /* non-JSON error body */
    }
    throw new ApiError(res.status, message);
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export const api = {
  // auth
  register: (body: Record<string, unknown>) =>
    request<AuthResponse>('/api/auth/register', { method: 'POST', body: JSON.stringify(body) }),
  login: (body: { email: string; password: string }) =>
    request<AuthResponse>('/api/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  googleSignIn: (idToken: string) =>
    request<AuthResponse>('/api/auth/google', { method: 'POST', body: JSON.stringify({ idToken }) }),
  oauthConfig: () => request<OAuthConfig>('/api/auth/oauth-config'),
  me: () => request<AuthResponse['user']>('/api/auth/me'),
  verifyEmail: (token: string) => request<void>(`/api/auth/verify-email?token=${encodeURIComponent(token)}`),
  forgotPassword: (email: string) =>
    request<void>('/api/auth/forgot-password', { method: 'POST', body: JSON.stringify({ email }) }),
  resetPassword: (token: string, newPassword: string) =>
    request<void>('/api/auth/reset-password', { method: 'POST', body: JSON.stringify({ token, newPassword }) }),

  // schools
  schools: () => request<School[]>('/api/schools'),
  sets: (schoolId: number) => request<ClassSet[]>(`/api/schools/${schoolId}/sets`),

  // challenge
  challenge: () => request<ChallengeTimeline>('/api/challenge'),
  challengeDay: (day: number) => request<DayDetail>(`/api/challenge/day/${day}`),

  // memories
  feed: (params: Record<string, string | number | undefined>) => {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') qs.set(k, String(v));
    });
    return request<PageResponse<MemoryDto>>(`/api/memories?${qs.toString()}`);
  },
  memory: (id: number) => request<MemoryDto>(`/api/memories/${id}`),
  createMemory: (form: FormData) =>
    request<MemoryDto>('/api/memories', { method: 'POST', body: form }),
  updateMemory: (id: number, body: { answer?: string; mood?: string; mediaUrl?: string; mediaType?: string }) =>
    request<MemoryDto>(`/api/memories/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  deleteMemory: (id: number) => request<void>(`/api/memories/${id}`, { method: 'DELETE' }),
  react: (id: number, type = 'LIKE') =>
    request<{ liked: boolean; type: string }>(`/api/memories/${id}/reactions`, {
      method: 'POST',
      body: JSON.stringify({ type }),
    }),
  comments: (id: number) => request<PageResponse<CommentDto>>(`/api/memories/${id}/comments?size=100`),
  addComment: (id: number, body: string) =>
    request<CommentDto>(`/api/memories/${id}/comments`, { method: 'POST', body: JSON.stringify({ body }) }),
  deleteComment: (memoryId: number, commentId: number) =>
    request<void>(`/api/memories/${memoryId}/comments/${commentId}`, { method: 'DELETE' }),

  // users
  userProfile: (handle: string) => request<UserDto>(`/api/users/${handle}`),
  myProfile: () => request<UserDto>('/api/users/me'),
  updateProfile: (body: Record<string, unknown>) =>
    request<UserDto>('/api/users/me', { method: 'PATCH', body: JSON.stringify(body) }),
  uploadAvatar: (form: FormData) => request<UserDto>('/api/users/me/avatar', { method: 'POST', body: form }),
  userMemories: (id: number) => request<MemoryDto[]>(`/api/users/${id}/memories`),
  follow: (id: number) => request<void>(`/api/users/${id}/follow`, { method: 'POST' }),
  unfollow: (id: number) => request<void>(`/api/users/${id}/unfollow`, { method: 'POST' }),
  search: (params: Record<string, string | number | undefined>) => {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') qs.set(k, String(v));
    });
    return request<PageResponse<UserDto>>(`/api/users/search?${qs.toString()}`);
  },
  suggested: () => request<UserDto[]>('/api/users/suggested'),

  // notifications
  notifications: () => request<PageResponse<NotificationDto>>('/api/notifications?size=50'),
  unreadCount: () => request<{ count: number }>('/api/notifications/unread-count'),
  markNotificationRead: (id: number) => request<void>(`/api/notifications/${id}/read`, { method: 'PATCH' }),
  markAllRead: () => request<void>('/api/notifications/read-all', { method: 'PATCH' }),

  // announcements
  announcements: () => request<Announcement[]>('/api/announcements'),

  // achievements & leaderboards
  myAchievements: () => request<AchievementDto[]>('/api/achievements/me'),
  leaderboard: (type: string) => request<LeaderboardEntry[]>(`/api/leaderboards?type=${type}&limit=10`),

  // admin
  adminStats: () => request<AdminStats>('/api/admin/stats'),
  adminUsers: (q: string) => request<PageResponse<AdminUserRow>>(`/api/admin/users?q=${encodeURIComponent(q)}&size=100`),
  adminSetUserActive: (id: number, active: boolean) =>
    request<void>(`/api/admin/users/${id}/active`, { method: 'PATCH', body: JSON.stringify({ active }) }),
  adminSetUserRole: (id: number, role: string) =>
    request<void>(`/api/admin/users/${id}/role`, { method: 'PATCH', body: JSON.stringify({ role }) }),
  adminQuestions: () => request<AdminQuestionRow[]>('/api/admin/questions'),
  adminCreateQuestion: (body: { dayNumber: number; question: string; hint?: string }) =>
    request<AdminQuestionRow>('/api/admin/questions', { method: 'POST', body: JSON.stringify(body) }),
  adminUpdateQuestion: (id: number, body: Partial<AdminQuestionRow>) =>
    request<AdminQuestionRow>(`/api/admin/questions/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  adminMemories: () => request<PageResponse<AdminMemoryRow>>('/api/admin/memories?size=100'),
  adminModerateMemory: (id: number, deleted: boolean) =>
    request<void>(`/api/admin/memories/${id}/moderate`, { method: 'PATCH', body: JSON.stringify({ deleted }) }),
  adminComments: () => request<PageResponse<AdminCommentRow>>('/api/admin/comments?size=100'),
  adminDeleteComment: (id: number) => request<void>(`/api/admin/comments/${id}`, { method: 'DELETE' }),
  adminAnnouncements: () => request<Announcement[]>(`/api/admin/announcements`),
  adminCreateAnnouncement: (title: string, body: string) =>
    request<Announcement>('/api/admin/announcements', { method: 'POST', body: JSON.stringify({ title, body }) }),
  adminDeleteAnnouncement: (id: number) =>
    request<void>(`/api/admin/announcements/${id}`, { method: 'DELETE' }),
  adminCreateSchool: (name: string, description?: string) =>
    request<School>('/api/admin/schools', { method: 'POST', body: JSON.stringify({ name, description }) }),
  adminCreateSet: (schoolId: number, name: string, graduationYear?: number) =>
    request<ClassSet>(`/api/admin/schools/${schoolId}/sets`, {
      method: 'POST',
      body: JSON.stringify({ name, graduationYear }),
    }),
};

export interface OAuthConfig {
  enabled: boolean;
  clientId: string | null;
}

export interface AdminUserRow {
  id: number;
  email: string;
  fullName: string;
  nickname?: string | null;
  username?: string | null;
  role: string;
  active: boolean;
  verified: boolean;
  schoolName?: string | null;
  memories: number;
  createdAt: string;
}

export interface AdminQuestionRow {
  id: number;
  dayNumber: number;
  question: string;
  hint?: string | null;
  active: boolean;
}

export interface AdminMemoryRow {
  id: number;
  authorId: number;
  authorName: string;
  authorEmail: string;
  dayNumber: number;
  question?: string | null;
  answer: string;
  mediaUrl?: string | null;
  deleted: boolean;
  createdAt: string;
}

export interface AdminCommentRow {
  id: number;
  memoryId: number;
  authorId: number;
  authorName: string;
  body: string;
  deleted: boolean;
  createdAt: string;
}
