<script setup lang="ts">
import { ref, onMounted } from 'vue';

const isDark = ref(false);

onMounted(() => {
  // 检查本地存储或系统偏好
  const savedTheme = localStorage.getItem('theme');
  const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  
  // 优先使用保存的主题，否则使用系统偏好
  const shouldBeDark = savedTheme === 'dark' || (!savedTheme && systemPrefersDark);
  
  isDark.value = shouldBeDark;
  
  // 初始化时立即应用主题
  applyTheme(shouldBeDark);
});

const applyTheme = (dark: boolean) => {
  const theme = dark ? 'dark' : 'light';
  document.documentElement.setAttribute('data-theme', theme);
  localStorage.setItem('theme', theme);
};

const toggleTheme = () => {
  isDark.value = !isDark.value;
  applyTheme(isDark.value);
};
</script>

<template>
  <button
    @click="toggleTheme"
    class="theme-switcher"
    :class="{ 'is-dark': isDark }"
    :aria-label="isDark ? '切换到亮色模式' : '切换到暗色模式'"
  >
    <div class="switcher-track">
      <div class="switcher-thumb">
        <!-- 太阳图标 -->
        <svg v-if="!isDark" class="icon-sun" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2v2" />
          <path d="M12 20v2" />
          <path d="m4.93 4.93 1.41 1.41" />
          <path d="m17.66 17.66 1.41 1.41" />
          <path d="M2 12h2" />
          <path d="M20 12h2" />
          <path d="m6.34 17.66-1.41 1.41" />
          <path d="m19.07 4.93-1.41 1.41" />
        </svg>
        <!-- 月亮图标 -->
        <svg v-else class="icon-moon" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z" />
        </svg>
      </div>
    </div>
  </button>
</template>

<style scoped>
.theme-switcher {
  width: 48px;
  height: 26px;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
  position: relative;
}

.switcher-track {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #ff6b9d 0%, #ff8fab 100%);
  border-radius: 50px;
  position: relative;
  transition: all 0.3s ease;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

.is-dark .switcher-track {
  background: linear-gradient(135deg, #1a1a2e 0%, #2d2d44 100%);
}

.switcher-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 22px;
  height: 22px;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.is-dark .switcher-thumb {
  transform: translateX(22px);
  background: #ff8fab;
}

.icon-sun {
  color: #ff6b9d;
}

.icon-moon {
  color: #1a1a2e;
}
</style>
