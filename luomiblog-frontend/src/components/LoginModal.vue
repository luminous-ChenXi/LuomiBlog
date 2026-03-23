<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import { api } from '../utils/api';
import { setAuth } from '../stores/user';
import { useBackendStatus } from '../composables/useBackendStatus';

const isVisible = ref(false);
const loginError = ref('');
const backendError = ref('');

const { isUnavailable, backendStatus, checkBackendStatus } = useBackendStatus();

const form = ref({
  username: '',
  password: ''
});

const submitting = ref(false);
const errors = ref({
  username: '',
  password: ''
});

const handleClose = () => {
  isVisible.value = false;
  // 重置表单
  form.value = { username: '', password: '' };
  errors.value = { username: '', password: '' };
  loginError.value = '';
  backendError.value = '';
};

const handleOpen = async () => {
  // 打开前检查后端状态
  backendError.value = '';
  await checkBackendStatus(true);
  
  if (isUnavailable.value) {
    backendError.value = backendStatus.value?.message || '后端服务暂时不可用，无法登录';
  }
  
  isVisible.value = true;
};

const validateForm = () => {
  let isValid = true;
  errors.value = { username: '', password: '' };
  loginError.value = '';

  if (!form.value.username.trim()) {
    errors.value.username = '请输入用户名或邮箱';
    isValid = false;
  }

  if (!form.value.password) {
    errors.value.password = '请输入密码';
    isValid = false;
  } else if (form.value.password.length < 6) {
    errors.value.password = '密码长度至少6位';
    isValid = false;
  }

  return isValid;
};

const handleSubmit = async () => {
  if (!validateForm()) return;

  // 再次检查后端状态
  await checkBackendStatus(true);
  if (isUnavailable.value) {
    loginError.value = '后端服务暂时不可用，请稍后再试';
    return;
  }

  submitting.value = true;
  loginError.value = '';

  try {
      const response = await api.auth.login({
        usernameOrEmail: form.value.username.trim(),
        password: form.value.password
      });

    // 保存认证信息
    setAuth(response);

    // 显示登录成功提示
    ElMessage.success('登录成功！欢迎回来');

    // 关闭弹窗
    handleClose();

    // 跳转到个人中心
    window.location.href = '/user';
  } catch (error: any) {
    const errorMessage = error.message || '';
    
    // 根据错误类型显示人性化提示
    if (errorMessage.includes('密码') || errorMessage.includes('password') || errorMessage.includes('Bad credentials')) {
      loginError.value = '密码错误，请重新输入。如果忘记密码，可以联系管理员重置。';
    } else if (errorMessage.includes('用户') || errorMessage.includes('username') || errorMessage.includes('不存在')) {
      loginError.value = '该用户名或邮箱未注册，请先注册账号。';
    } else if (errorMessage.includes('锁定') || errorMessage.includes('locked')) {
      loginError.value = '账号已被锁定，请联系管理员解锁。';
    } else if (errorMessage.includes('禁用') || errorMessage.includes('disabled')) {
      loginError.value = '账号已被禁用，如有疑问请联系管理员。';
    } else if (errorMessage.includes('ECONNREFUSED') || errorMessage.includes('Failed to fetch')) {
      loginError.value = '无法连接到服务器，请检查网络连接或稍后再试。';
    } else {
      loginError.value = '登录失败，请检查用户名和密码是否正确。';
    }
  } finally {
    submitting.value = false;
  }
};

// ESC 键关闭
const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && isVisible.value) {
    handleClose();
  }
};

// 监听 isVisible 变化
watch(isVisible, (newVal) => {
  if (newVal) {
    document.addEventListener('keydown', handleKeydown);
    document.body.style.overflow = 'hidden';
  } else {
    document.removeEventListener('keydown', handleKeydown);
    document.body.style.overflow = '';
  }
});

// 暴露方法给全局
onMounted(() => {
  // 将打开方法挂载到 window 对象
  (window as any).openLoginModal = handleOpen;
  (window as any).closeLoginModal = handleClose;
  
  // 监听自定义事件
  window.addEventListener('open-login-modal', handleOpen as EventListener);
});

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown);
  document.body.style.overflow = '';
  window.removeEventListener('open-login-modal', handleOpen as EventListener);
  delete (window as any).openLoginModal;
  delete (window as any).closeLoginModal;
});
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="isVisible" class="login-modal-overlay" @click.self="handleClose">
        <div class="login-modal-container">
          <!-- 关闭按钮 -->
          <button class="modal-close" @click="handleClose">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6 6 18"/>
              <path d="m6 6 12 12"/>
            </svg>
          </button>

          <div class="modal-content">
            <!-- 左侧：品牌展示 -->
            <div class="modal-brand">
              <div class="brand-logo-large">
                <span class="logo-text">CX</span>
              </div>
              <h2 class="brand-title">辰汐博客</h2>
              <p class="brand-slogan">记录生活，分享技术</p>

              <!-- 装饰区域 -->
              <div class="brand-visual">
                <div class="visual-placeholder">
                  <svg class="placeholder-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"/>
                    <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
                    <line x1="12" x2="12" y1="19" y2="22"/>
                  </svg>
                  <span>探索无限可能</span>
                </div>
              </div>
            </div>

            <!-- 右侧：登录表单 -->
            <div class="modal-form">
              <header class="form-header">
                <p class="form-eyebrow">欢迎回来</p>
                <h1 class="form-title">登录账号</h1>
              </header>

              <form class="login-form" @submit.prevent="handleSubmit">
                <!-- 后端服务错误提示 -->
                <div v-if="backendError" class="backend-error">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" x2="12" y1="8" y2="12"/>
                    <line x1="12" x2="12.01" y1="16" y2="16"/>
                  </svg>
                  <span>{{ backendError }}</span>
                </div>
                
                <!-- 登录错误提示 -->
                <div v-else-if="loginError" class="login-error">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" x2="12" y1="8" y2="12"/>
                    <line x1="12" x2="12.01" y1="16" y2="16"/>
                  </svg>
                  <span>{{ loginError }}</span>
                </div>

                <div class="form-item">
                  <label class="form-label">用户名 / 邮箱</label>
                  <div class="input-wrapper">
                    <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/>
                      <circle cx="12" cy="7" r="4"/>
                    </svg>
                    <input
                      v-model="form.username"
                      type="text"
                      placeholder="请输入用户名或邮箱"
                      class="form-input"
                      :class="{ error: errors.username }"
                    />
                  </div>
                  <span v-if="errors.username" class="error-message">{{ errors.username }}</span>
                </div>

                <div class="form-item">
                  <label class="form-label">密码</label>
                  <div class="input-wrapper">
                    <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect width="18" height="11" x="3" y="11" rx="2" ry="2"/>
                      <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                    </svg>
                    <input
                      v-model="form.password"
                      type="password"
                      placeholder="请输入密码"
                      class="form-input"
                      :class="{ error: errors.password }"
                    />
                  </div>
                  <span v-if="errors.password" class="error-message">{{ errors.password }}</span>
                </div>

                <div class="form-options">
                  <a href="#" class="forgot-link" @click.prevent>忘记密码？</a>
                </div>

                <button
                  type="submit"
                  class="btn-login"
                  :disabled="submitting || isUnavailable"
                  :class="{ disabled: isUnavailable }"
                >
                  <span v-if="isUnavailable">服务不可用</span>
                  <span v-else-if="!submitting">登录</span>
                  <span v-else class="loading-spinner"></span>
                </button>

                <div class="form-footer">
                  <span class="footer-text">还没有账号？</span>
                  <a href="#" class="register-link" @click.prevent>立即注册</a>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* 遮罩层 - 亚克力质感 */
.login-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

/* 弹窗容器 */
.login-modal-container {
  position: relative;
  width: 100%;
  max-width: 800px;
  background: white;
  border-radius: 24px;
  box-shadow:
    0 25px 50px -12px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

/* 关闭按钮 */
.modal-close {
  position: absolute;
  top: 1rem;
  right: 1rem;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.05);
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: rgba(0, 0, 0, 0.1);
  color: #333;
}

.modal-close svg {
  width: 18px;
  height: 18px;
}

/* 内容布局 */
.modal-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 500px;
}

/* 左侧品牌区 */
.modal-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 2rem;
  background: linear-gradient(135deg, #FADCE9 0%, #F9A8C8 50%, #E87A9F 100%);
  color: white;
  text-align: center;
}

.brand-logo-large {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  border-radius: 24px;
  background: white;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  margin-bottom: 1.5rem;
}

.logo-text {
  font-size: 2rem;
  font-weight: 800;
  background: linear-gradient(135deg, #F9A8C8 0%, #E87A9F 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-title {
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0 0 0.5rem;
  color: white;
}

.brand-slogan {
  font-size: 1rem;
  opacity: 0.9;
  margin: 0 0 2rem;
}

/* 装饰区域 */
.brand-visual {
  width: 100%;
  max-width: 200px;
}

.visual-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 2rem;
  background: rgba(255, 255, 255, 0.2);
  border: 2px dashed rgba(255, 255, 255, 0.4);
  border-radius: 16px;
  backdrop-filter: blur(4px);
}

.placeholder-icon {
  width: 40px;
  height: 40px;
  opacity: 0.8;
}

.visual-placeholder span {
  font-size: 0.9rem;
  opacity: 0.9;
}

/* 右侧表单区 */
.modal-form {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 3rem 2.5rem;
  background: white;
}

.form-header {
  text-align: center;
  margin-bottom: 2rem;
}

.form-eyebrow {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: #F9A8C8;
  margin: 0 0 0.5rem;
}

.form-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

/* 表单样式 */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* 后端错误提示 */
.backend-error {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  padding: 1rem 1rem;
  background: linear-gradient(135deg, #fff7ed 0%, #fffaf5 100%);
  border: 1px solid #fed7aa;
  border-radius: 12px;
  color: #9a3412;
  font-size: 0.875rem;
  line-height: 1.5;
  margin-bottom: 1rem;
  box-shadow: 0 2px 8px rgba(234, 88, 12, 0.08);
}

.backend-error svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  margin-top: 1px;
  color: #ea580c;
}

/* 登录错误提示 */
.login-error {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  padding: 1rem 1rem;
  background: linear-gradient(135deg, #fef2f2 0%, #fff5f5 100%);
  border: 1px solid #fecaca;
  border-radius: 12px;
  color: #991b1b;
  font-size: 0.875rem;
  line-height: 1.5;
  margin-bottom: 1rem;
  animation: shake 0.5s ease-in-out;
  box-shadow: 0 2px 8px rgba(220, 38, 38, 0.08);
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}

.login-error svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  margin-top: 1px;
  color: #dc2626;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #4a4a5a;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 12px;
  width: 20px;
  height: 20px;
  color: #9ca3af;
  pointer-events: none;
}

.form-input {
  width: 100%;
  padding: 12px 12px 12px 44px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  font-size: 0.9375rem;
  color: #1a1a2e;
  background: white;
  transition: all 0.2s ease;
}

.form-input::placeholder {
  color: #9ca3af;
}

.form-input:hover {
  border-color: #F9A8C8;
}

.form-input:focus {
  outline: none;
  border-color: #F9A8C8;
  box-shadow: 0 0 0 3px rgba(249, 168, 200, 0.15);
}

.form-input.error {
  border-color: #ef4444;
}

.form-input.error:focus {
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.15);
}

.error-message {
  font-size: 0.75rem;
  color: #ef4444;
}

.form-options {
  display: flex;
  justify-content: flex-end;
}

.forgot-link {
  font-size: 0.875rem;
  color: #F9A8C8;
  text-decoration: none;
  transition: color 0.2s ease;
}

.forgot-link:hover {
  color: #E87A9F;
}

/* 登录按钮 */
.btn-login {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #F9A8C8 0%, #E87A9F 100%);
  border: none;
  font-size: 1rem;
  font-weight: 600;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-login:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(249, 168, 200, 0.4);
}

.btn-login:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-login.disabled {
  background: linear-gradient(135deg, #9ca3af 0%, #6b7280 100%);
}

.loading-spinner {
  width: 20px;
  height: 20px;
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

/* 底部链接 */
.form-footer {
  text-align: center;
  margin-top: 0.5rem;
}

.footer-text {
  font-size: 0.875rem;
  color: #6b7280;
}

.register-link {
  font-size: 0.875rem;
  color: #F9A8C8;
  font-weight: 600;
  text-decoration: none;
  margin-left: 0.25rem;
  transition: color 0.2s ease;
}

.register-link:hover {
  color: #E87A9F;
}

/* 过渡动画 */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .login-modal-container,
.modal-leave-to .login-modal-container {
  transform: scale(0.95);
  opacity: 0;
}

/* 暗色主题适配 */
[data-theme="dark"] .login-modal-container {
  background: #1a1a2e;
}

[data-theme="dark"] .modal-form {
  background: #1a1a2e;
}

[data-theme="dark"] .form-title {
  color: #e0e0e0;
}

[data-theme="dark"] .form-label {
  color: #a0a0b0;
}

[data-theme="dark"] .form-input {
  background: #252538;
  border-color: #3a3a4a;
  color: #e0e0e0;
}

[data-theme="dark"] .form-input::placeholder {
  color: #6b7280;
}

[data-theme="dark"] .footer-text {
  color: #9ca3af;
}

[data-theme="dark"] .modal-close {
  background: rgba(255, 255, 255, 0.1);
  color: #a0a0b0;
}

[data-theme="dark"] .modal-close:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #e0e0e0;
}

/* 响应式 */
@media (max-width: 768px) {
  .modal-content {
    grid-template-columns: 1fr;
  }

  .modal-brand {
    display: none;
  }

  .modal-form {
    padding: 2rem 1.5rem;
  }
}
</style>