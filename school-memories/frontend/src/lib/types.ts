export interface Identity {
  id: number;
  email: string;
  fullName: string;
  nickname?: string | null;
  username?: string | null;
  avatarUrl?: string | null;
  bio?: string | null;
  schoolId?: number | null;
  schoolName?: string | null;
  graduationYear?: number | null;
  role: 'USER' | 'ADMIN';
  verified: boolean;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: Identity;
  verifyLink?: string | null;
}

export interface School {
  id: number;
  name: string;
  description?: string | null;
}

export interface ClassSet {
  id: number;
  name: string;
  graduationYear?: number | null;
}

export interface UserDto {
  id: number;
  email?: string;
  fullName: string;
  nickname?: string | null;
  username?: string | null;
  bio?: string | null;
  avatarUrl?: string | null;
  school?: { id: number; name: string } | null;
  classSet?: { id: number; name: string; graduationYear?: number | null } | null;
  graduationYear?: number | null;
  role?: string;
  verified?: boolean;
  following: boolean;
  followers: number;
  followingCount: number;
  memories: number;
  likesReceived: number;
  createdAt: string;
}

export interface MemoryDto {
  id: number;
  author: {
    id: number;
    fullName: string;
    nickname?: string | null;
    username?: string | null;
    avatarUrl?: string | null;
    schoolId?: number | null;
    schoolName?: string | null;
    className?: string | null;
    graduationYear?: number | null;
  };
  dayNumber: number;
  question?: string | null;
  answer: string;
  mood?: string | null;
  mediaUrl?: string | null;
  mediaType?: 'PHOTO' | 'VIDEO' | null;
  thumbnailUrl?: string | null;
  likes: number;
  comments: number;
  likedByMe: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CommentDto {
  id: number;
  authorId: number;
  authorName: string;
  authorNickname?: string | null;
  authorUsername?: string | null;
  authorAvatarUrl?: string | null;
  body: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DayDto {
  dayNumber: number;
  question: string;
  hint?: string | null;
  active: boolean;
  answered: boolean;
  memoryId?: number | null;
  answerSnippet?: string | null;
  answeredAt?: string | null;
}

export interface ChallengeTimeline {
  days: DayDto[];
  answeredCount: number;
  total: number;
  streak: number;
}

export interface DayDetail {
  dayNumber: number;
  question: string;
  hint?: string | null;
  active: boolean;
  memory: MemoryDto | null;
}

export interface NotificationDto {
  id: number;
  type: 'LIKE' | 'COMMENT' | 'FOLLOW' | 'ACHIEVEMENT' | 'ANNOUNCEMENT';
  message: string;
  actorId?: number | null;
  actorName?: string | null;
  actorAvatarUrl?: string | null;
  memoryId?: number | null;
  read: boolean;
  createdAt: string;
}

export interface AchievementDto {
  code: string;
  name: string;
  description: string;
  unlockedAt: string;
}

export interface LeaderboardEntry {
  userId: number;
  name: string;
  avatarUrl?: string | null;
  value: number;
}

export interface Announcement {
  id: number;
  title: string;
  body: string;
  active: boolean;
  createdAt: string;
}

export interface AdminStats {
  users: number;
  schools: number;
  memories: number;
  comments: number;
  questions: number;
  announcements: number;
}
