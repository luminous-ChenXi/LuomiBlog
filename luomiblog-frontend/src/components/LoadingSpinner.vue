<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';

const isLoading = ref(true);

// 监听页面加载完成
const handleLoad = () => {
  // 添加小延迟确保平滑过渡
  setTimeout(() => {
    isLoading.value = false;
  }, 300);
};

// 监听资源加载
const handleResourcesLoaded = () => {
  const images = document.querySelectorAll('img');
  const promises = Array.from(images).map(img => {
    if (img.complete) return Promise.resolve();
    return new Promise((resolve) => {
      img.addEventListener('load', resolve);
      img.addEventListener('error', resolve);
    });
  });

  Promise.all(promises).then(() => {
    handleLoad();
  });
};

onMounted(() => {
  // 如果页面已经加载完成
  if (document.readyState === 'complete') {
    handleLoad();
  } else {
    // 等待页面加载
    window.addEventListener('load', handleResourcesLoaded);
    // 设置超时，确保不会一直显示
    setTimeout(() => {
      isLoading.value = false;
    }, 3000);
  }
});

onUnmounted(() => {
  window.removeEventListener('load', handleResourcesLoaded);
});
</script>

<template>
  <Transition name="fade">
    <div v-if="isLoading" class="loading-overlay">
      <div class="spinner">
        <span></span>
        <span></span>
        <span></span>
        <span></span>
        <span></span>
        <span></span>
        <span></span>
        <span></span>
      </div>
      <p class="loading-text">Loading...</p>
    </div>
  </Transition>
</template>

<style scoped>
.loading-overlay {
  position: fixed;
  inset: 0;
  background: var(--color-bg, #ffffff);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.spinner {
  position: relative;
  width: 90px;
  height: 90px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 50%;
  margin-left: -110px;
  transform: scale(1.5);
}

.spinner span {
  position: absolute;
  top: 50%;
  left: var(--left);
  width: 52px;
  height: 10px;
  background: #ffff;
  animation: dominos 1s ease infinite;
  box-shadow: 2px 2px 3px 0px black;
  border-radius: 2px;
}

.spinner span:nth-child(1) {
  --left: 120px;
  animation-delay: 0.125s;
}

.spinner span:nth-child(2) {
  --left: 105px;
  animation-delay: 0.3s;
}

.spinner span:nth-child(3) {
  left: 90px;
  animation-delay: 0.425s;
}

.spinner span:nth-child(4) {
  animation-delay: 0.54s;
  left: 75px;
}

.spinner span:nth-child(5) {
  animation-delay: 0.665s;
  left: 60px;
}

.spinner span:nth-child(6) {
  animation-delay: 0.79s;
  left: 45px;
}

.spinner span:nth-child(7) {
  animation-delay: 0.915s;
  left: 30px;
}

.spinner span:nth-child(8) {
  left: 15px;
}

@keyframes dominos {
  50% {
    opacity: 0.7;
  }

  75% {
    -webkit-transform: rotate(90deg);
    transform: rotate(90deg);
  }

  80% {
    opacity: 1;
  }
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease, transform 0.5s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

/* 暗色主题适配 - 背景变化 */
[data-theme="dark"] .loading-overlay {
  background: var(--color-bg, #0f0f1a);
}
</style>
