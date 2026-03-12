<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { api } from '../utils/api';
import { setAuth } from '../stores/user';

const isVisible = ref(false);
const isLoading = ref(false);
const currentStep = ref(1);
const registerError = ref('');

const formData = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  agreeTerms: false
});

const errors = ref<Record<string, string>>({});

const handleOpen = () => {
  isVisible.value = true;
  document.body.style.overflow = 'hidden';
  resetForm();
};

const handleClose = () => {
  isVisible.value = false;
  document.body.style.overflow = '';
};

const resetForm = () => {
  formData.value = {
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    agreeTerms: false
  };
  errors.value = {};
  registerError.value = '';
  currentStep.value = 1;
};

const validateStep1 = () => {
  errors.value = {};
  
  if (!formData.value.username.trim()) {
    errors.value.username = '请输入用户名';
  } else if (formData.value.username.length < 3) {
    errors.value.username = '用户名至少3个字符';
  }
  
  if (!formData.value.email.trim()) {
    errors.value.email = '请输入邮箱';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.value.email)) {
    errors.value.email = '请输入有效的邮箱地址';
  }
  
  return Object.keys(errors.value).length === 0;
};

const validateStep2 = () => {
  errors.value = {};
  
  if (!formData.value.password) {
    errors.value.password = '请输入密码';
  } else if (formData.value.password.length < 6) {
    errors.value.password = '密码至少6个字符';
  }
  
  if (formData.value.password !== formData.value.confirmPassword) {
    errors.value.confirmPassword = '两次输入的密码不一致';
  }
  
  if (!formData.value.agreeTerms) {
    errors.value.agreeTerms = '请同意服务条款';
  }
  
  return Object.keys(errors.value).length === 0;
};

const nextStep = () => {
  registerError.value = '';
  if (validateStep1()) {
    currentStep.value = 2;
  }
};

const prevStep = () => {
  currentStep.value = 1;
  errors.value = {};
};

const handleSubmit = async () => {
  if (!validateStep2()) return;

  isLoading.value = true;
  registerError.value = '';

  try {
    const response = await api.auth.register({
      username: formData.value.username.trim(),
      email: formData.value.email.trim(),
      password: formData.value.password,
      confirmPassword: formData.value.confirmPassword,
      nickname: formData.value.username.trim()
    });

    // 保存认证信息（自动登录）
    setAuth(response);

    isLoading.value = false;
    handleClose();

    // 触发注册成功事件
    window.dispatchEvent(new CustomEvent('register-success'));

    // 跳转到个人中心
    window.location.href = '/user';
  } catch (error: any) {
    registerError.value = error.message || '注册失败，请检查输入信息';
    isLoading.value = false;
  }
};

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    handleClose();
  }
};

onMounted(() => {
  // 将打开方法挂载到 window 对象
  (window as any).openRegisterModal = handleOpen;
  (window as any).closeRegisterModal = handleClose;
  
  // 监听自定义事件
  window.addEventListener('open-register-modal', handleOpen as EventListener);
  document.addEventListener('keydown', handleKeydown);
});

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown);
  document.body.style.overflow = '';
  window.removeEventListener('open-register-modal', handleOpen as EventListener);
  delete (window as any).openRegisterModal;
  delete (window as any).closeRegisterModal;
});
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="isVisible" class="modal-overlay" @click.self="handleClose">
        <div class="modal-container">
          <!-- 关闭按钮 -->
          <button class="close-btn" @click="handleClose">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 6 6 18"/><path d="m6 6 12 12"/>
            </svg>
          </button>

          <!-- 左侧装饰 -->
          <div class="modal-decoration">
            <div class="decoration-content">
              <div class="logo-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 2L2 7l10 5 10-5-10-5z"/>
                  <path d="M2 17l10 5 10-5"/>
                  <path d="M2 12l10 5 10-5"/>
                </svg>
              </div>
              <h3 class="decoration-title">加入辰汐小站</h3>
              <p class="decoration-desc">创建账号，开启您的技术之旅</p>
              
              <!-- 步骤指示器 -->
              <div class="step-indicator">
                <div class="step" :class="{ active: currentStep >= 1, current: currentStep === 1 }">
                  <span class="step-number">1</span>
                  <span class="step-label">基本信息</span>
                </div>
                <div class="step-line"></div>
                <div class="step" :class="{ active: currentStep >= 2, current: currentStep === 2 }">
                  <span class="step-number">2</span>
                  <span class="step-label">设置密码</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧表单 -->
          <div class="modal-form">
            <div class="form-header">
              <h2 class="form-title">注册账号</h2>
              <p class="form-subtitle">已有账号？<a href="#" @click.prevent="handleClose(); (window as any).openLoginModal?.()">立即登录</a></p>
            </div>

            <form @submit.prevent="currentStep === 1 ? nextStep() : handleSubmit()">
              <!-- 步骤 1: 基本信息 -->
              <div v-if="currentStep === 1" class="form-step">
                <div class="form-group">
                  <label class="form-label">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
                    </svg>
                    用户名
                  </label>
                  <input 
                    v-model="formData.username"
                    type="text" 
                    class="form-input" 
                    placeholder="请输入用户名"
                    :class="{ error: errors.username }"
                  />
                  <span v-if="errors.username" class="error-message">{{ errors.username }}</span>
                </div>

                <div class="form-group">
                  <label class="form-label">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <rect width="20" height="16" x="2" y="4" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/>
                    </svg>
                    邮箱
                  </label>
                  <input 
                    v-model="formData.email"
                    type="email" 
                    class="form-input" 
                    placeholder="请输入邮箱地址"
                    :class="{ error: errors.email }"
                  />
                  <span v-if="errors.email" class="error-message">{{ errors.email }}</span>
                </div>

                <button type="submit" class="btn btn-primary btn-block">
                  下一步
                  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M5 12h14"/><path d="m12 5 7 7-7 7"/>
                  </svg>
                </button>
              </div>

              <!-- 步骤 2: 设置密码 -->
              <div v-else class="form-step">
                <!-- 注册错误提示 -->
                <div v-if="registerError" class="register-error">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" x2="12" y1="8" y2="12"/>
                    <line x1="12" x2="12.01" y1="16" y2="16"/>
                  </svg>
                  <span>{{ registerError }}</span>
                </div>

                <div class="form-group">
                  <label class="form-label">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <rect width="18" height="11" x="3" y="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                    </svg>
                    密码
                  </label>
                  <input 
                    v-model="formData.password"
                    type="password" 
                    class="form-input" 
                    placeholder="请设置密码（至少6位）"
                    :class="{ error: errors.password }"
                  />
                  <span v-if="errors.password" class="error-message">{{ errors.password }}</span>
                </div>

                <div class="form-group">
                  <label class="form-label">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <rect width="18" height="11" x="3" y="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                    </svg>
                    确认密码
                  </label>
                  <input 
                    v-model="formData.confirmPassword"
                    type="password" 
                    class="form-input" 
                    placeholder="请再次输入密码"
                    :class="{ error: errors.confirmPassword }"
                  />
                  <span v-if="errors.confirmPassword" class="error-message">{{ errors.confirmPassword }}</span>
                </div>

                <div class="form-group">
                  <label class="checkbox-label" :class="{ error: errors.agreeTerms }">
                    <input 
                      v-model="formData.agreeTerms"
                      type="checkbox" 
                      class="checkbox-input"
                    />
                    <span class="checkbox-text">
                      我已阅读并同意 <a href="#" class="link">服务条款</a> 和 <a href="#" class="link">隐私政策</a>
                    </span>
                  </label>
                  <span v-if="errors.agreeTerms" class="error-message">{{ errors.agreeTerms }}</span>
                </div>

                <div class="form-actions">
                  <button type="button" class="btn btn-secondary" @click="prevStep">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M19 12H5"/><path d="m12 19-7-7 7-7"/>
                    </svg>
                    上一步
                  </button>
                  <button type="submit" class="btn btn-primary" :disabled="isLoading">
                    <span v-if="isLoading" class="loading-spinner"></span>
                    <span v-else>完成注册</span>
                  </button>
                </div>
              </div>
            </form>

            <!-- 社交注册 -->
            <div class="social-login">
              <div class="divider">
                <span>或使用以下方式注册</span>
              </div>
              <div class="social-buttons">
                <button class="btn-social">
                  <svg viewBox="0 0 24 24" width="20" height="20">
                    <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                    <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                    <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                    <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                  </svg>
                </button>
                <button class="btn-social">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                    <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* 注册错误提示 */
.register-error {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 0.875rem;
  margin-bottom: 1rem;
}

.register-error svg {
  flex-shrink: 0;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
}

.modal-container {
  background: var(--color-card-bg, #ffffff);
  border-radius: 24px;
  width: 100%;
  max-width: 900px;
  min-height: 560px;
  display: flex;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  position: relative;
}

.close-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.05);
  color: var(--color-text-secondary, #666);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  z-index: 10;
}

.close-btn:hover {
  background: rgba(0, 0, 0, 0.1);
  color: var(--color-text, #333);
}

/* 左侧装饰 */
.modal-decoration {
  flex: 0 0 320px;
  background: linear-gradient(135deg, #ff6b9d 0%, #c44569 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: white;
}

.decoration-content {
  text-align: center;
}

.logo-icon {
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  backdrop-filter: blur(10px);
}

.decoration-title {
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0 0 8px 0;
}

.decoration-desc {
  font-size: 0.9375rem;
  opacity: 0.9;
  margin: 0 0 40px 0;
}

/* 步骤指示器 */
.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  opacity: 0.5;
  transition: all 0.3s ease;
}

.step.active {
  opacity: 1;
}

.step.current .step-number {
  background: white;
  color: #ff6b9d;
}

.step-number {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.875rem;
  transition: all 0.3s ease;
}

.step-label {
  font-size: 0.75rem;
  font-weight: 500;
}

.step-line {
  width: 40px;
  height: 2px;
  background: rgba(255, 255, 255, 0.3);
}

/* 右侧表单 */
.modal-form {
  flex: 1;
  padding: 48px;
  display: flex;
  flex-direction: column;
}

.form-header {
  text-align: center;
  margin-bottom: 32px;
}

.form-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text, #333);
  margin: 0 0 8px 0;
}

.form-subtitle {
  font-size: 0.9375rem;
  color: var(--color-text-secondary, #666);
  margin: 0;
}

.form-subtitle a {
  color: #ff6b9d;
  text-decoration: none;
  font-weight: 500;
}

.form-subtitle a:hover {
  text-decoration: underline;
}

/* 表单样式 */
.form-step {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text, #333);
}

.form-input {
  padding: 12px 16px;
  border: 1px solid var(--color-border, #e0e0e0);
  border-radius: 12px;
  font-size: 0.9375rem;
  background: var(--color-bg, #f5f5f5);
  color: var(--color-text, #333);
  transition: all 0.2s ease;
}

.form-input:focus {
  outline: none;
  border-color: #ff6b9d;
  background: white;
}

.form-input.error {
  border-color: #ef4444;
}

.error-message {
  font-size: 0.8125rem;
  color: #ef4444;
}

/* 复选框 */
.checkbox-label {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  cursor: pointer;
  font-size: 0.875rem;
  color: var(--color-text-secondary, #666);
}

.checkbox-label.error {
  color: #ef4444;
}

.checkbox-input {
  width: 18px;
  height: 18px;
  margin-top: 2px;
  accent-color: #ff6b9d;
  cursor: pointer;
}

.checkbox-text {
  line-height: 1.5;
}

.checkbox-text .link {
  color: #ff6b9d;
  text-decoration: none;
}

.checkbox-text .link:hover {
  text-decoration: underline;
}

/* 按钮 */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 24px;
  border: none;
  border-radius: 12px;
  font-size: 0.9375rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-primary {
  background: linear-gradient(135deg, #ff6b9d 0%, #c44569 100%);
  color: white;
  flex: 1;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(255, 107, 157, 0.3);
}

.btn-primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-secondary {
  background: var(--color-bg-secondary, #f0f0f0);
  color: var(--color-text, #333);
}

.btn-secondary:hover {
  background: var(--color-border, #e0e0e0);
}

.btn-block {
  width: 100%;
  margin-top: 8px;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

/* 加载动画 */
.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 社交登录 */
.social-login {
  margin-top: auto;
  padding-top: 32px;
}

.divider {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  color: var(--color-text-secondary, #999);
  font-size: 0.875rem;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--color-border, #e0e0e0);
}

.social-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.btn-social {
  width: 44px;
  height: 44px;
  border: 1px solid var(--color-border, #e0e0e0);
  border-radius: 12px;
  background: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.btn-social:hover {
  border-color: #ff6b9d;
  transform: translateY(-2px);
}

/* 动画 */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

/* 响应式 */
@media (max-width: 768px) {
  .modal-container {
    flex-direction: column;
    max-width: 400px;
  }

  .modal-decoration {
    flex: none;
    padding: 32px 24px;
  }

  .logo-icon {
    width: 60px;
    height: 60px;
    margin-bottom: 16px;
  }

  .decoration-title {
    font-size: 1.25rem;
  }

  .decoration-desc {
    margin-bottom: 24px;
  }

  .modal-form {
    padding: 32px 24px;
  }
}

/* 暗色主题适配 */
[data-theme="dark"] .modal-container {
  background: #1a1a2e;
}

[data-theme="dark"] .form-input {
  background: #252542;
  border-color: #3a3a5c;
  color: #fff;
}

[data-theme="dark"] .form-input:focus {
  background: #2d2d4a;
}

[data-theme="dark"] .btn-secondary {
  background: #3a3a5c;
  color: #fff;
}

[data-theme="dark"] .btn-secondary:hover {
  background: #4a4a6c;
}

[data-theme="dark"] .btn-social {
  background: #252542;
  border-color: #3a3a5c;
}
</style>
