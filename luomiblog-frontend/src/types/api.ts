// API 类型定义

// 通用响应类型
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

// 分页响应类型
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// 用户类型
export interface User {
  id: number;
  username: string;
  email: string;
  nickname: string | null;
  avatar: string | null;
  bio: string | null;
  website: string | null;
  github: string | null;
  role: 'visitor' | 'member' | 'blogger' | 'admin';
  status: string;
  createdAt: string;
  updatedAt: string;
}

// 文章类型
export interface Article {
  id: number;
  title: string;
  slug: string;
  summary: string;
  aiSummary: string | null;
  content: string;
  contentHtml: string;
  categoryId: number | null;
  categoryName: string | null;
  authorId: number;
  authorName: string;
  authorAvatar: string | null;
  language: string;
  status: 'draft' | 'published' | 'archived';
  version: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  wordCount: number;
  readingTime: number;
  top: boolean;
  sortOrder: number;
  allowComments: boolean;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
  tags: Tag[];
}

// 标签类型
export interface Tag {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  color: string | null;
  usageCount: number;
}

// 分类类型
export interface Category {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  parentId: number | null;
  sortOrder: number;
  articleCount: number;
}

// 评论类型
export interface Comment {
  id: number;
  articleId: number;
  parentId: number | null;
  userId: number | null;
  visitorName: string | null;
  visitorEmail: string | null;
  visitorWebsite: string | null;
  content: string;
  contentHtml: string;
  status: 'pending' | 'approved' | 'rejected';
  isTop: boolean;
  likeCount: number;
  replyCount: number;
  createdAt: string;
  updatedAt: string;
  replies?: Comment[];
  user?: User;
}

// 登录请求
export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

// 注册请求
export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  nickname?: string;
}

// 认证响应
export interface AuthResponse {
  token: string;
  type: string;
  user: User;
}

// AI 问答请求
export interface AIAskRequest {
  articleId: number;
  question: string;
  language?: string;
}

// AI 问答响应
export interface AIAskResponse {
  answer: string;
  sources: string[];
  answerId: string;
}
