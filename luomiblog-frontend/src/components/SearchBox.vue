<script setup lang="ts">
import { ref } from 'vue';

const searchQuery = ref('');
const isFocused = ref(false);

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    // 示例：可以在这里实现搜索逻辑
    console.log('搜索:', searchQuery.value);
  }
};

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter') {
    handleSearch();
  }
};
</script>

<template>
  <div class="search-box" :class="{ focused: isFocused }">
    <div class="search-input-wrapper">
      <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="11" cy="11" r="8"/>
        <path d="m21 21-4.35-4.35"/>
      </svg>
      <input
        v-model="searchQuery"
        type="text"
        placeholder="搜索文章..."
        class="search-input"
        @focus="isFocused = true"
        @blur="isFocused = false"
        @keydown="handleKeydown"
      />
      <button v-if="searchQuery" class="clear-btn" @click="searchQuery = ''">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M18 6 6 18"/>
          <path d="m6 6 12 12"/>
        </svg>
      </button>
    </div>
    <button class="search-btn" @click="handleSearch">
      搜索
    </button>
  </div>
</template>

<style scoped>
.search-box {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 12px;
  background: var(--color-card, #ffffff);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.search-box.focused {
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.15);
  transform: translateY(-1px);
}

.search-input-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-bg-secondary, #f5f5f5);
  border-radius: 8px;
  padding: 0 12px;
  transition: all 0.3s ease;
}

.search-box.focused .search-input-wrapper {
  background: var(--color-bg, #ffffff);
  box-shadow: inset 0 0 0 2px var(--color-primary, #ff6b9d);
}

.search-icon {
  width: 18px;
  height: 18px;
  color: var(--color-text-secondary, #999);
  flex-shrink: 0;
}

.search-box.focused .search-icon {
  color: var(--color-primary, #ff6b9d);
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 10px 0;
  font-size: 14px;
  color: var(--color-text, #333);
  outline: none;
}

.search-input::placeholder {
  color: var(--color-text-secondary, #999);
}

.clear-btn {
  width: 20px;
  height: 20px;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 2px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.clear-btn:hover {
  background: var(--color-bg-secondary, #f0f0f0);
}

.clear-btn svg {
  width: 14px;
  height: 14px;
  color: var(--color-text-secondary, #999);
}

.search-btn {
  padding: 10px 20px;
  background: var(--color-primary, #ff6b9d);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.search-btn:hover {
  background: var(--color-primary-dark, #e55a8a);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 107, 157, 0.3);
}

/* 暗色主题适配 */
[data-theme="dark"] .search-box {
  background: var(--color-card, #2a2a3a);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

[data-theme="dark"] .search-input-wrapper {
  background: rgba(255, 255, 255, 0.05);
}

[data-theme="dark"] .search-box.focused .search-input-wrapper {
  background: rgba(255, 255, 255, 0.08);
}

[data-theme="dark"] .search-input {
  color: var(--color-text, #e0e0e0);
}

[data-theme="dark"] .clear-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

@media (max-width: 768px) {
  .search-box {
    flex-wrap: wrap;
  }
  
  .search-input-wrapper {
    width: 100%;
  }
  
  .search-btn {
    width: 100%;
  }
}
</style>
