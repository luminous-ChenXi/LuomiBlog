// 辰汐通行证（Chenxi Passport）登录工具
// 标准 OAuth 2.1 / OIDC 授权码 + PKCE（S256）：本站是公共客户端，没有任何密钥，
// 授权跳转前在浏览器侧生成 code_verifier 并保留，回调后随授权码一起交给后端换取会话。

import { api } from './api';

// sessionStorage 键名与有效期（PKCE 中间态只在一次授权跳转期间存在）
const STORAGE_KEY = 'chenxi_oauth';
const STATE_TTL_MS = 10 * 60 * 1000;

// 授权跳转前存入 sessionStorage 的中间态
interface ChenxiOAuthState {
  verifier: string;
  state: string;
  nonce: string;
  createdAt: number;
}

// base64url 编码（RFC 4648 §5，无填充）
function base64UrlEncode(bytes: Uint8Array): string {
  let binary = '';
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

// 生成密码学安全的随机串（state / nonce / code_verifier 共用）
function randomToken(byteLength: number): string {
  const bytes = new Uint8Array(byteLength);
  crypto.getRandomValues(bytes);
  return base64UrlEncode(bytes);
}

// code_challenge = BASE64URL(SHA-256(code_verifier))，通行证只接受 S256
async function createCodeChallenge(verifier: string): Promise<string> {
  if (!crypto?.subtle) {
    // Web Crypto 仅在安全上下文（HTTPS 或 localhost）下可用
    throw new Error('当前环境不支持安全加密（需通过 HTTPS 或 localhost 访问），无法使用辰汐通行证登录');
  }
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier));
  return base64UrlEncode(new Uint8Array(digest));
}

// 保存授权中间态
function saveOAuthState(state: ChenxiOAuthState): void {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

/**
 * 校验回调带回的 state 并取回中间态（一次性：无论是否有效，读取后立即清除）
 * 中间态缺失 / state 不匹配 / 超过 10 分钟有效期都视为无效
 */
export function consumeOAuthState(returnedState: string | null): ChenxiOAuthState | null {
  const raw = sessionStorage.getItem(STORAGE_KEY);
  sessionStorage.removeItem(STORAGE_KEY);
  if (!returnedState || !raw) {
    return null;
  }
  try {
    const saved = JSON.parse(raw) as ChenxiOAuthState;
    if (!saved?.verifier || saved.state !== returnedState) {
      return null;
    }
    if (Date.now() - saved.createdAt > STATE_TTL_MS) {
      return null;
    }
    return saved;
  } catch {
    return null;
  }
}

/**
 * 发起辰汐通行证登录：
 * 1. 读取后端公开的门牌配置（未启用则报错）；
 * 2. 生成 code_verifier / state / nonce 并存入 sessionStorage（10 分钟有效）；
 * 3. 跳转通行证授权页。
 * 失败时抛出带中文提示的 Error，由调用方（登录弹窗）展示。
 */
export async function startChenxiLogin(): Promise<void> {
  const config = await api.chenxi.config();
  if (!config.enabled) {
    throw new Error('辰汐通行证登录未启用');
  }
  if (!config.clientId || !config.redirectUri) {
    throw new Error('辰汐通行证登录配置不完整，请联系站点管理员');
  }

  const verifier = randomToken(64);
  const codeChallenge = await createCodeChallenge(verifier);
  const oauthState: ChenxiOAuthState = {
    verifier,
    state: randomToken(32),
    nonce: randomToken(32),
    createdAt: Date.now()
  };
  saveOAuthState(oauthState);

  // 手工拼接查询串：encodeURIComponent 会把 scope 的空格编码为 %20，与通行证契约一致
  const query = [
    'response_type=code',
    `client_id=${encodeURIComponent(config.clientId)}`,
    `redirect_uri=${encodeURIComponent(config.redirectUri)}`,
    `scope=${encodeURIComponent(config.scopes)}`,
    `state=${encodeURIComponent(oauthState.state)}`,
    `nonce=${encodeURIComponent(oauthState.nonce)}`,
    `code_challenge=${encodeURIComponent(codeChallenge)}`,
    'code_challenge_method=S256'
  ].join('&');

  window.location.href = `${config.issuer}/oauth2/authorize?${query}`;
}
