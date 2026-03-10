<script setup lang="ts">
import { ref, onMounted } from 'vue';

const typedText = ref('');
const fullText = '全栈开发者';
const isTyping = ref(true);

onMounted(() => {
  let index = 0;
  const typeInterval = setInterval(() => {
    if (index <= fullText.length) {
      typedText.value = fullText.slice(0, index);
      index++;
    } else {
      clearInterval(typeInterval);
      setTimeout(() => {
        index = 0;
        typedText.value = '';
      }, 3000);
    }
  }, 150);
});
</script>

<template>
  <div class="profile-card">
    <!-- 卡片头部 -->
    <div class="profile-header">
      <div class="avatar-frame">
        <div class="avatar-placeholder">
          <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
          </svg>
        </div>
        <div class="status-dot"></div>
      </div>
      <h5 class="profile-name">辰汐</h5>
      <div class="profile-label">
        <span class="label-prefix">I'm</span>
        <span class="typed-text">{{ typedText }}</span>
        <span class="cursor" :class="{ 'cursor-blink': isTyping }">|</span>
      </div>
    </div>

    <!-- 分隔线 -->
    <div class="divider"></div>

    <!-- 社交链接 -->
    <div class="social-links">
      <a href="#" class="social-link" aria-label="GitHub">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.48-1-3.5.28-1.15.28-2.35 0-3.5 0 0-1 0-3 1.5-2.64-.5-5.36-.5-8 0C6 2 5 2 5 2c-.3 1.15-.3 2.35 0 3.5A5.403 5.403 0 0 0 4 9c0 3.5 3 5.5 6 5.5-.39.49-.68 1.05-.85 1.65-.17.6-.22 1.23-.15 1.85v4"/>
          <path d="M9 18c-4.51 2-5-2-7-2"/>
        </svg>
      </a>
      <a href="#" class="social-link" aria-label="Twitter">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 4s-.7 2.1-2 3.4c1.6 10-9.4 17.3-18 11.6 2.2.1 4.4-.6 6-2C3 15.5.5 9.6 3 5c2.2 2.6 5.6 4.1 9 4-.9-4.2 4-6.6 7-3.8 1.1 0 3-1.2 3-1.2z"/>
        </svg>
      </a>
      <a href="#" class="social-link" aria-label="Email">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect width="20" height="16" x="2" y="4" rx="2"/>
          <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/>
        </svg>
      </a>
      <a href="#" class="social-link" aria-label="RSS">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M4 11a9 9 0 0 1 9 9"/>
          <path d="M4 4a16 16 0 0 1 16 16"/>
          <circle cx="5" cy="19" r="1"/>
        </svg>
      </a>
    </div>

    <!-- 分隔线 -->
    <div class="divider"></div>

    <!-- 个人信息 -->
    <ul class="info-list">
      <li>
        <span class="info-label">居住地:</span>
        <span class="info-value">中国</span>
      </li>
      <li>
        <span class="info-label">城市:</span>
        <span class="info-value">上海</span>
      </li>
      <li>
        <span class="info-label">年龄:</span>
        <span class="info-value">25</span>
      </li>
    </ul>

    <!-- 分隔线 -->
    <div class="divider"></div>

    <!-- 联系按钮 -->
    <div class="action-area">
      <a href="mailto:contact@example.com" class="contact-btn">
        联系我
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect width="20" height="16" x="2" y="4" rx="2"/>
          <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/>
        </svg>
      </a>
    </div>
  </div>
</template>

<style scoped>
.profile-card {
  display: flex;
  flex-direction: column;
  padding: 40px;
  background: var(--color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border);
  height: calc(100vh - 140px);
  max-height: 600px;
  min-height: 500px;
  transition: box-shadow var(--transition-base);
  position: relative;
}

.profile-card::before {
  content: '';
  position: absolute;
  left: 30px;
  top: -8px;
  height: 8px;
  width: calc(100% - 60px);
  background: var(--color-card);
  border-radius: 5px 5px 0 0;
  opacity: 0.5;
}

.profile-card:hover {
  box-shadow: var(--shadow-brand);
}

/* 头部区域 */
.profile-header {
  text-align: center;
  flex-shrink: 0;
}

.avatar-frame {
  position: relative;
  display: inline-block;
  margin-bottom: 16px;
}

.avatar-placeholder {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.15) 0%, rgba(78, 205, 196, 0.15) 100%);
  border: 3px solid var(--color-card);
  box-shadow: var(--shadow-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-brand-primary);
}

.status-dot {
  position: absolute;
  bottom: 8px;
  right: 8px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--color-brand-accent);
  border: 3px solid var(--color-card);
  box-shadow: var(--shadow-sm);
}

.profile-name {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 8px;
}

.profile-label {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.label-prefix {
  font-weight: 500;
}

.typed-text {
  color: var(--color-brand-primary);
  font-weight: 600;
}

.cursor {
  color: var(--color-brand-primary);
  font-weight: 300;
}

.cursor-blink {
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 分隔线 */
.divider {
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, var(--color-border) 50%, transparent 100%);
  margin: 24px 0;
  flex-shrink: 0;
}

/* 社交链接 */
.social-links {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-shrink: 0;
}

.social-link {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  transition: all var(--transition-base);
}

.social-link:hover {
  background: var(--gradient-primary);
  border-color: transparent;
  color: white;
  transform: translateY(-2px);
  box-shadow: var(--shadow-brand);
}

/* 信息列表 */
.info-list {
  list-style: none;
  padding: 0;
  margin: 0;
  flex-shrink: 0;
}

.info-list li {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.info-list li:last-child {
  margin-bottom: 0;
}

.info-label {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.info-value {
  font-size: 0.875rem;
  color: var(--color-text);
  font-weight: 600;
}

/* 联系按钮 */
.action-area {
  text-align: center;
  margin-top: auto;
  flex-shrink: 0;
}

.contact-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 32px;
  background: var(--gradient-primary);
  color: white;
  font-size: 0.9375rem;
  font-weight: 600;
  text-decoration: none;
  border-radius: var(--radius-full);
  transition: all var(--transition-base);
  box-shadow: var(--shadow-brand);
  width: 100%;
}

.contact-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(255, 107, 157, 0.35);
}

/* 响应式 */
@media (max-width: 968px) {
  .profile-card {
    height: auto;
    max-height: none;
    min-height: auto;
    margin-bottom: 40px;
  }

  .profile-card::before {
    display: none;
  }
}
</style>
