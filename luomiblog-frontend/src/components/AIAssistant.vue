<script setup lang="ts">
import { ref } from 'vue';

const userInput = ref('');
const isFocused = ref(false);
const isLoading = ref(false);

const handleSend = () => {
  if (userInput.value.trim() && !isLoading.value) {
    isLoading.value = true;
    // 示例：模拟AI回复
    setTimeout(() => {
      isLoading.value = false;
      userInput.value = '';
    }, 1000);
  }
};

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    handleSend();
  }
};

const quickQuestions = [
  '这篇文章讲了什么？',
  '总结一下核心观点',
  '有什么实践建议？'
];
</script>

<template>
  <div class="ai-assistant">
    <!-- 头部区域 -->
    <div class="ai-header">
      <div class="ai-avatar">
        <div class="avatar-glow"></div>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/>
          <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
          <line x1="12" x2="12" y1="19" y2="22"/>
        </svg>
      </div>
      <div class="ai-info">
        <h4 class="ai-title">AI 助手</h4>
        <span class="ai-status">
          <span class="status-dot"></span>
          <span class="status-text">在线</span>
          <span class="status-divider">·</span>
          <span class="model-tag">RAG 增强</span>
        </span>
      </div>
      <div class="ai-actions">
        <button class="action-btn" title="清空对话">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 6h18"/>
            <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/>
            <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- 快速问题 -->
    <div class="quick-questions">
      <button
        v-for="(question, index) in quickQuestions"
        :key="index"
        class="question-chip"
        @click="userInput = question"
      >
        {{ question }}
      </button>
    </div>

    <!-- 输入区域 -->
    <div class="ai-input-area" :class="{ focused: isFocused }">
      <textarea
        v-model="userInput"
        placeholder="有问题问我吧..."
        class="ai-input"
        rows="2"
        @focus="isFocused = true"
        @blur="isFocused = false"
        @keydown="handleKeydown"
      ></textarea>
      <button
        class="send-btn"
        :class="{ active: userInput.trim(), loading: isLoading }"
        @click="handleSend"
        :disabled="!userInput.trim() || isLoading"
      >
        <svg v-if="!isLoading" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="m22 2-7 20-4-9-9-4 20-7z"/>
        </svg>
        <span v-else class="loading-spinner"></span>
      </button>
    </div>

    <!-- 底部提示 -->
    <div class="ai-footer">
      <div class="ai-tips">
        <svg class="tip-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" x2="12" y1="16" y2="12"/>
          <line x1="12" x2="12.01" y1="8" y2="8"/>
        </svg>
        <span class="tip-text">基于博主文章内容的智能问答</span>
      </div>
      <div class="keyboard-hint">
        <kbd>Enter</kbd> 发送
      </div>
    </div>
  </div>
</template>

<style scoped>
.ai-assistant {
  background: var(--color-card, #ffffff);
  border-radius: 20px;
  padding: 20px;
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.05),
    0 1px 3px rgba(0, 0, 0, 0.02);
  border: 1px solid var(--color-border-soft, rgba(0, 0, 0, 0.04));
  transition: all 0.3s ease;
}

.ai-assistant:hover {
  box-shadow:
    0 8px 30px rgba(255, 107, 157, 0.1),
    0 2px 8px rgba(0, 0, 0, 0.04);
  border-color: rgba(255, 107, 157, 0.15);
}

/* 头部区域 */
.ai-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.ai-avatar {
  position: relative;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #ff6b9d 0%, #feca57 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 15px rgba(255, 107, 157, 0.35);
  overflow: hidden;
}

.avatar-glow {
  position: absolute;
  inset: -2px;
  background: linear-gradient(135deg, #ff6b9d 0%, #feca57 100%);
  border-radius: 16px;
  opacity: 0.5;
  filter: blur(8px);
  animation: glow-pulse 3s ease-in-out infinite;
}

@keyframes glow-pulse {
  0%, 100% {
    opacity: 0.4;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(1.05);
  }
}

.ai-avatar svg {
  width: 26px;
  height: 26px;
  position: relative;
  z-index: 1;
}

.ai-info {
  flex: 1;
}

.ai-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text, #333);
  margin: 0 0 5px 0;
  background: linear-gradient(135deg, #ff6b9d 0%, #feca57 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.ai-status {
  font-size: 12px;
  color: var(--color-text-secondary, #999);
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 7px;
  height: 7px;
  background: #10b981;
  border-radius: 50%;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
  }
  50% {
    opacity: 0.8;
    box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.1);
  }
}

.status-divider {
  opacity: 0.4;
}

.model-tag {
  font-size: 10px;
  padding: 2px 6px;
  background: rgba(255, 107, 157, 0.1);
  color: var(--color-brand-primary, #ff6b9d);
  border-radius: 4px;
  font-weight: 500;
}

.ai-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: var(--color-bg-secondary, #f5f5f5);
  color: var(--color-text-secondary, #999);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: rgba(255, 107, 157, 0.1);
  color: var(--color-brand-primary, #ff6b9d);
}

.action-btn svg {
  width: 16px;
  height: 16px;
}

/* 快速问题 */
.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.question-chip {
  padding: 6px 12px;
  background: var(--color-bg-secondary, #f5f5f5);
  border: 1px solid transparent;
  border-radius: 20px;
  font-size: 12px;
  color: var(--color-text-secondary, #666);
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.question-chip:hover {
  background: rgba(255, 107, 157, 0.08);
  border-color: rgba(255, 107, 157, 0.2);
  color: var(--color-brand-primary, #ff6b9d);
  transform: translateY(-1px);
}

/* 输入区域 */
.ai-input-area {
  position: relative;
  background: var(--color-bg-secondary, #f8f8f8);
  border-radius: 16px;
  padding: 14px;
  padding-right: 50px;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.ai-input-area.focused {
  background: var(--color-bg, #ffffff);
  border-color: rgba(255, 107, 157, 0.3);
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.08);
}

.ai-input {
  width: 100%;
  border: none;
  background: transparent;
  font-size: 14px;
  color: var(--color-text, #333);
  resize: none;
  outline: none;
  line-height: 1.6;
  min-height: 44px;
}

.ai-input::placeholder {
  color: var(--color-text-muted, #aaa);
}

.send-btn {
  position: absolute;
  bottom: 10px;
  right: 10px;
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 12px;
  background: var(--color-bg-tertiary, #e8e8e8);
  color: var(--color-text-muted, #bbb);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.send-btn.active {
  background: linear-gradient(135deg, #ff6b9d 0%, #feca57 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(255, 107, 157, 0.35);
}

.send-btn.active:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 6px 18px rgba(255, 107, 157, 0.45);
}

.send-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.send-btn svg {
  width: 18px;
  height: 18px;
  margin-left: 2px;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 底部区域 */
.ai-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--color-border-soft, rgba(0, 0, 0, 0.04));
}

.ai-tips {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tip-icon {
  width: 14px;
  height: 14px;
  color: var(--color-brand-primary, #ff6b9d);
  opacity: 0.7;
}

.tip-text {
  font-size: 11px;
  color: var(--color-text-muted, #999);
}

.keyboard-hint {
  font-size: 11px;
  color: var(--color-text-muted, #aaa);
  display: flex;
  align-items: center;
  gap: 4px;
}

.keyboard-hint kbd {
  padding: 2px 6px;
  background: var(--color-bg-secondary, #f0f0f0);
  border-radius: 4px;
  font-family: inherit;
  font-size: 10px;
  border: 1px solid var(--color-border, #ddd);
}

/* 暗色主题适配 */
[data-theme="dark"] .ai-assistant {
  background: var(--color-card, #2a2a3a);
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 4px 20px rgba(0, 0, 0, 0.2),
    0 1px 3px rgba(0, 0, 0, 0.1);
}

[data-theme="dark"] .ai-assistant:hover {
  border-color: rgba(255, 107, 157, 0.2);
  box-shadow:
    0 8px 30px rgba(255, 107, 157, 0.15),
    0 2px 8px rgba(0, 0, 0, 0.2);
}

[data-theme="dark"] .ai-input-area {
  background: rgba(255, 255, 255, 0.04);
}

[data-theme="dark"] .ai-input-area.focused {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 107, 157, 0.4);
}

[data-theme="dark"] .ai-input {
  color: var(--color-text, #e0e0e0);
}

[data-theme="dark"] .send-btn {
  background: rgba(255, 255, 255, 0.08);
}

[data-theme="dark"] .question-chip {
  background: rgba(255, 255, 255, 0.06);
  color: var(--color-text-secondary, #aaa);
}

[data-theme="dark"] .question-chip:hover {
  background: rgba(255, 107, 157, 0.12);
}

[data-theme="dark"] .action-btn {
  background: rgba(255, 255, 255, 0.06);
}

[data-theme="dark"] .ai-footer {
  border-top-color: rgba(255, 255, 255, 0.06);
}

[data-theme="dark"] .keyboard-hint kbd {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.1);
}
</style>
