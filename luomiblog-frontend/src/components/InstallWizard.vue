<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '../utils/api';

const currentStep = ref(0);
const loading = ref(false);
const installStatus = ref<{ installed: boolean; locked: boolean; message: string } | null>(null);
const dbTestPassed = ref(false);

// 表单数据
const envForm = reactive({
  javaVersion: '',
  portAvailable: false,
  mysqlDriver: false
});

const dbForm = reactive({
  host: 'localhost',
  port: 3306,
  database: 'luomiblog',
  username: '',
  password: ''
});

// 监听数据库配置变化，重置测试状态
watch(() => [dbForm.host, dbForm.port, dbForm.database, dbForm.username, dbForm.password], () => {
  dbTestPassed.value = false;
}, { deep: true });

const siteForm = reactive({
  siteName: 'LuomiBlog',
  siteDescription: '一个基于 Astro + Vue + SpringBoot 的 AI 知识库博客',
  defaultTheme: 'auto',
  defaultLanguage: 'zh',
  timezone: 'Asia/Shanghai'
});

const adminForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  nickname: ''
});

// 检查安装状态
const checkInstallStatus = async () => {
  try {
    const response = await api.install.getStatus();
    installStatus.value = response;
    
    // 如果已安装且已锁定，直接跳转到首页
    if (response.locked) {
      ElMessageBox.alert('系统已安装完成', '提示', {
        confirmButtonText: '前往首页',
        showClose: false,
        closeOnClickModal: false,
        closeOnPressEscape: false,
        callback: () => {
          window.location.href = '/';
        }
      });
      return;
    }
    
    // 如果已安装但未锁定（有用户数据），显示提示并跳转
    if (response.installed) {
      ElMessageBox.alert('系统已初始化，如需重新安装请清空数据库用户表', '提示', {
        confirmButtonText: '前往首页',
        showClose: false,
        closeOnClickModal: false,
        closeOnPressEscape: false,
        callback: () => {
          window.location.href = '/';
        }
      });
    }
  } catch (error) {
    console.error('检查安装状态失败', error);
  }
};

// 环境检测
const checkEnvironment = async () => {
  loading.value = true;
  try {
    const response = await api.install.checkEnvironment();

    const javaCheck = response.checks.find(c => c.name === 'Java 版本');
    const backendCheck = response.checks.find(c => c.name === '后端服务');
    const driverCheck = response.checks.find(c => c.name === 'MySQL 驱动');

    envForm.javaVersion = javaCheck?.message || '';
    envForm.portAvailable = backendCheck?.passed || false;
    envForm.mysqlDriver = driverCheck?.passed || false;

    if (response.allPassed) {
      ElMessage.success('环境检测通过');
      currentStep.value++;
    } else {
      const failedChecks = response.checks.filter(c => !c.passed);
      const messages = failedChecks.map(c => `${c.name}: ${c.suggestion}`).join('\n');
      ElMessageBox.alert(messages, '环境检测未通过', {
        confirmButtonText: '我知道了'
      });
    }
  } catch (error: any) {
    if (error.status === 403) {
      ElMessageBox.alert('系统已安装，无法重复安装', '提示', {
        confirmButtonText: '前往首页',
        callback: () => {
          window.location.href = '/';
        }
      });
    } else {
      ElMessage.error('环境检测失败: ' + (error.message || '未知错误'));
    }
  } finally {
    loading.value = false;
  }
};

// 测试数据库连接
const testDatabase = async () => {
  if (!dbForm.host || !dbForm.database || !dbForm.username) {
    ElMessage.warning('请填写完整的数据库配置');
    return;
  }

  loading.value = true;
  try {
    const response = await api.install.testDatabase(dbForm);
    if (response.success) {
      ElMessage.success('数据库连接成功，MySQL 版本符合要求');
      dbTestPassed.value = true;
    } else {
      ElMessage.error(response.message || '数据库连接失败或版本过低（需要 MySQL 8.0+）');
      dbTestPassed.value = false;
    }
  } catch (error: any) {
    dbTestPassed.value = false;
    if (error.status === 403) {
      ElMessageBox.alert('系统已安装，无法重复安装', '提示', {
        confirmButtonText: '前往首页',
        callback: () => {
          window.location.href = '/';
        }
      });
    } else {
      ElMessage.error(error.message || '数据库连接失败');
    }
  } finally {
    loading.value = false;
  }
};

// 下一步（数据库配置步骤）
const nextStepFromDb = async () => {
  if (!dbTestPassed.value) {
    ElMessage.warning('请先测试数据库连接');
    return;
  }
  currentStep.value++;
};

// 执行 SQL 脚本
const executeSql = async () => {
  loading.value = true;
  try {
    const response = await api.install.executeSql(dbForm);
    if (response.success) {
      ElMessage.success('数据库初始化成功');
      currentStep.value++;
    } else {
      ElMessage.error(response.message);
    }
  } catch (error: any) {
    ElMessage.error(error.message || 'SQL 执行失败');
  } finally {
    loading.value = false;
  }
};

// 创建管理员账号
const createAdmin = async () => {
  if (!adminForm.username || !adminForm.password || !adminForm.email) {
    ElMessage.warning('请填写完整的管理员信息');
    return;
  }
  
  if (adminForm.password !== adminForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致');
    return;
  }
  
  if (adminForm.password.length < 8) {
    ElMessage.error('密码长度至少8位');
    return;
  }
  
  loading.value = true;
  try {
    const response = await api.install.createAdmin(adminForm);
    if (response.success) {
      ElMessage.success('管理员账号创建成功');
      currentStep.value++;
    } else {
      ElMessage.error(response.message);
    }
  } catch (error: any) {
    ElMessage.error(error.message || '管理员账号创建失败');
  } finally {
    loading.value = false;
  }
};

// 保存站点配置
const saveSiteConfig = async () => {
  if (!siteForm.siteName) {
    ElMessage.warning('请输入网站名称');
    return;
  }
  
  loading.value = true;
  try {
    const response = await api.install.saveSiteConfig(siteForm);
    if (response.success) {
      ElMessage.success('站点配置保存成功');
      currentStep.value++;
    } else {
      ElMessage.error(response.message);
    }
  } catch (error: any) {
    ElMessage.error(error.message || '站点配置保存失败');
  } finally {
    loading.value = false;
  }
};

// 完成安装
const completeInstall = async () => {
  loading.value = true;
  try {
    const response = await api.install.complete();
    if (response.success) {
      ElMessage.success('安装完成！');
      setTimeout(() => {
        window.location.href = '/';
      }, 1500);
    } else {
      ElMessage.error(response.message);
    }
  } catch (error: any) {
    ElMessage.error(error.message || '安装完成操作失败');
  } finally {
    loading.value = false;
  }
};

// 上一步
const prevStep = () => {
  currentStep.value--;
};

onMounted(() => {
  checkInstallStatus();
});
</script>

<template>
  <div class="install-wizard">
    <!-- 步骤条 -->
    <div class="steps-container">
      <el-steps :active="currentStep" class="steps" align-center>
        <el-step title="环境检测" />
        <el-step title="数据库配置" />
        <el-step title="初始化数据" />
        <el-step title="站点配置" />
        <el-step title="创建管理员" />
        <el-step title="完成安装" />
      </el-steps>
    </div>

    <!-- 步骤 1: 环境检测 -->
    <div v-if="currentStep === 0" class="step-content">
      <div class="step-card">
        <div class="card-header">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
          </div>
          <h3 class="card-title">环境检测</h3>
          <p class="card-desc">检查系统环境是否满足安装要求</p>
        </div>
        
        <div class="env-checks">
          <div class="check-item" :class="{ passed: envForm.javaVersion }">
            <div class="check-icon">
              <svg v-if="envForm.javaVersion" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/></svg>
            </div>
            <div class="check-info">
              <div class="check-name">Java 版本</div>
              <div class="check-status">{{ envForm.javaVersion || '待检测' }}</div>
            </div>
          </div>
          
          <div class="check-item" :class="{ passed: envForm.portAvailable }">
            <div class="check-icon">
              <svg v-if="envForm.portAvailable" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/></svg>
            </div>
            <div class="check-info">
              <div class="check-name">后端服务</div>
              <div class="check-status">{{ envForm.portAvailable ? '运行正常' : '未启动或待检测' }}</div>
            </div>
          </div>
          
          <div class="check-item" :class="{ passed: envForm.mysqlDriver }">
            <div class="check-icon">
              <svg v-if="envForm.mysqlDriver" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/></svg>
            </div>
            <div class="check-info">
              <div class="check-name">MySQL 驱动</div>
              <div class="check-status">{{ envForm.mysqlDriver ? '已加载' : '待检测' }}</div>
            </div>
          </div>
        </div>

        <div class="step-actions">
          <button class="btn-primary" :disabled="loading" @click="checkEnvironment">
            <span v-if="loading" class="btn-loading"></span>
            <span v-else>开始检测</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 步骤 2: 数据库配置 -->
    <div v-if="currentStep === 1" class="step-content">
      <div class="step-card">
        <div class="card-header">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M3 5V19A9 3 0 0 0 21 19V5"/><path d="M3 12A9 3 0 0 0 21 12"/></svg>
          </div>
          <h3 class="card-title">数据库配置</h3>
          <p class="card-desc">配置数据库连接信息</p>
        </div>
        
        <div class="install-form">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">数据库主机</label>
              <input v-model="dbForm.host" type="text" class="form-input" placeholder="localhost" />
            </div>
            <div class="form-group">
              <label class="form-label">数据库端口</label>
              <input v-model.number="dbForm.port" type="number" class="form-input" placeholder="3306" />
            </div>
          </div>
          
          <div class="form-group">
            <label class="form-label">数据库名称</label>
            <input v-model="dbForm.database" type="text" class="form-input" placeholder="luomiblog" />
          </div>
          
          <div class="form-group">
            <label class="form-label">数据库用户名</label>
            <input v-model="dbForm.username" type="text" class="form-input" placeholder="root" />
          </div>
          
          <div class="form-group">
            <label class="form-label">数据库密码</label>
            <input v-model="dbForm.password" type="password" class="form-input" placeholder="请输入数据库密码" />
          </div>
        </div>

        <div class="step-actions">
          <button class="btn-secondary" @click="prevStep">上一步</button>
          <button class="btn-secondary" :disabled="loading" @click="testDatabase">
            测试连接
          </button>
          <button class="btn-primary" :disabled="loading || !dbTestPassed" @click="nextStepFromDb">
            下一步
          </button>
        </div>
      </div>
    </div>

    <!-- 步骤 3: 初始化数据 -->
    <div v-if="currentStep === 2" class="step-content">
      <div class="step-card">
        <div class="card-header">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          </div>
          <h3 class="card-title">初始化数据</h3>
          <p class="card-desc">创建数据库表结构和初始数据</p>
        </div>
        
        <div class="init-info">
          <div class="info-box">
            <div class="info-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
            </div>
            <div class="info-content">
              <p class="info-title">即将执行数据库初始化</p>
              <p class="info-desc">此步骤将执行 schema.sql 和 data.sql，创建必要的表结构和初始数据</p>
            </div>
          </div>
          
          <div class="init-details">
            <p class="details-title">将执行的操作：</p>
            <ul class="details-list">
              <li>创建用户表、文章表、评论表等核心表结构</li>
              <li>初始化角色数据（访客、会员、博主、管理员）</li>
              <li>初始化权限数据</li>
              <li>创建系统配置表</li>
            </ul>
          </div>
        </div>

        <div class="step-actions">
          <button class="btn-secondary" @click="prevStep">上一步</button>
          <button class="btn-primary" :disabled="loading" @click="executeSql">
            <span v-if="loading" class="btn-loading"></span>
            <span v-else>开始初始化</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 步骤 4: 站点配置 -->
    <div v-if="currentStep === 3" class="step-content">
      <div class="step-card">
        <div class="card-header">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
          </div>
          <h3 class="card-title">站点配置</h3>
          <p class="card-desc">配置网站基本信息</p>
        </div>
        
        <div class="install-form">
          <div class="form-group">
            <label class="form-label">网站名称</label>
            <input v-model="siteForm.siteName" type="text" class="form-input" placeholder="LuomiBlog" />
          </div>
          
          <div class="form-group">
            <label class="form-label">网站描述</label>
            <textarea v-model="siteForm.siteDescription" class="form-textarea" rows="3" placeholder="请输入网站描述"></textarea>
          </div>
          
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">默认主题</label>
              <select v-model="siteForm.defaultTheme" class="form-select">
                <option value="auto">自动（跟随系统）</option>
                <option value="light">亮色模式</option>
                <option value="dark">暗色模式</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">默认语言</label>
              <select v-model="siteForm.defaultLanguage" class="form-select">
                <option value="zh">简体中文</option>
                <option value="en">English</option>
                <option value="ja">日本語</option>
              </select>
            </div>
          </div>
          
          <div class="form-group">
            <label class="form-label">时区</label>
            <select v-model="siteForm.timezone" class="form-select">
              <option value="Asia/Shanghai">Asia/Shanghai (中国标准时间)</option>
              <option value="Asia/Tokyo">Asia/Tokyo (日本标准时间)</option>
              <option value="UTC">UTC (协调世界时)</option>
            </select>
          </div>
        </div>

        <div class="step-actions">
          <button class="btn-secondary" @click="prevStep">上一步</button>
          <button class="btn-primary" :disabled="loading" @click="saveSiteConfig">
            下一步
          </button>
        </div>
      </div>
    </div>

    <!-- 步骤 5: 创建管理员 -->
    <div v-if="currentStep === 4" class="step-content">
      <div class="step-card">
        <div class="card-header">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          </div>
          <h3 class="card-title">创建管理员账号</h3>
          <p class="card-desc">设置管理员账号信息</p>
        </div>
        
        <div class="install-form">
          <div class="form-group">
            <label class="form-label">管理员用户名</label>
            <input v-model="adminForm.username" type="text" class="form-input" placeholder="请输入用户名（3-50位字母数字下划线）" />
          </div>
          
          <div class="form-group">
            <label class="form-label">管理员邮箱</label>
            <input v-model="adminForm.email" type="email" class="form-input" placeholder="请输入邮箱地址" />
          </div>
          
          <div class="form-group">
            <label class="form-label">显示昵称</label>
            <input v-model="adminForm.nickname" type="text" class="form-input" placeholder="可选，默认为用户名" />
          </div>
          
          <div class="form-group">
            <label class="form-label">登录密码</label>
            <input v-model="adminForm.password" type="password" class="form-input" placeholder="请输入密码（至少8位）" />
          </div>
          
          <div class="form-group">
            <label class="form-label">确认密码</label>
            <input v-model="adminForm.confirmPassword" type="password" class="form-input" placeholder="请再次输入密码" />
          </div>
        </div>

        <div class="step-actions">
          <button class="btn-secondary" @click="prevStep">上一步</button>
          <button class="btn-primary" :disabled="loading" @click="createAdmin">
            创建管理员
          </button>
        </div>
      </div>
    </div>

    <!-- 步骤 6: 完成安装 -->
    <div v-if="currentStep === 5" class="step-content">
      <div class="step-card complete-card">
        <div class="complete-icon">
          <div class="success-circle">
            <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
          </div>
        </div>
        
        <h3 class="complete-title">安装成功</h3>
        <p class="complete-subtitle">恭喜！LuomiBlog 已成功安装完成</p>
        
        <div class="complete-info">
          <div class="info-item">
            <span class="info-label">网站名称</span>
            <span class="info-value">{{ siteForm.siteName }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">管理员账号</span>
            <span class="info-value">{{ adminForm.username }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">登录地址</span>
            <span class="info-value">/login</span>
          </div>
        </div>

        <div class="step-actions">
          <button class="btn-primary btn-large" :disabled="loading" @click="completeInstall">
            <span v-if="loading" class="btn-loading"></span>
            <span v-else>完成安装并进入网站</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.install-wizard {
  background: var(--color-card);
  border-radius: var(--radius-lg);
  padding: 2rem;
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border);
}

.steps-container {
  margin-bottom: 2.5rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--color-border);
}

.steps {
  --el-color-primary: var(--color-brand-primary);
  --el-color-success: var(--color-brand-accent);
}

:deep(.el-step__title) {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

:deep(.el-step__title.is-process) {
  color: var(--color-brand-primary);
  font-weight: 600;
}

:deep(.el-step__title.is-success) {
  color: var(--color-brand-accent);
}

:deep(.el-step__head.is-process) {
  color: var(--color-brand-primary);
  border-color: var(--color-brand-primary);
}

:deep(.el-step__head.is-success) {
  color: var(--color-brand-accent);
  border-color: var(--color-brand-accent);
}

.step-content {
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.step-card {
  min-height: 360px;
}

.card-header {
  text-align: center;
  margin-bottom: 2rem;
}

.card-icon {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1) 0%, rgba(78, 205, 196, 0.1) 100%);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
  color: var(--color-brand-primary);
}

.card-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 0.5rem;
}

.card-desc {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  margin: 0;
}

/* 环境检测 */
.env-checks {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin: 1.5rem 0;
}

.check-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.25rem;
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  transition: all var(--transition-fast);
}

.check-item.passed {
  background: rgba(78, 205, 196, 0.08);
  border-color: var(--color-brand-accent);
}

.check-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-tertiary);
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.check-item.passed .check-icon {
  background: var(--color-brand-accent);
  color: white;
}

.check-info {
  flex: 1;
}

.check-name {
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 0.25rem;
}

.check-status {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

/* 表单样式 */
.install-form {
  max-width: 560px;
  margin: 0 auto;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-group {
  margin-bottom: 1.25rem;
}

.form-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 0.5rem;
}

.form-input,
.form-textarea,
.form-select {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 0.9375rem;
  transition: all var(--transition-fast);
}

.form-input:focus,
.form-textarea:focus,
.form-select:focus {
  outline: none;
  border-color: var(--color-brand-primary);
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.1);
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.form-select {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%23666' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  padding-right: 2.5rem;
}

/* 初始化信息 */
.init-info {
  margin: 1.5rem 0;
}

.info-box {
  display: flex;
  gap: 1rem;
  padding: 1rem 1.25rem;
  background: linear-gradient(135deg, rgba(135, 206, 235, 0.1) 0%, rgba(78, 205, 196, 0.08) 100%);
  border-radius: var(--radius-md);
  border: 1px solid rgba(135, 206, 235, 0.3);
}

.info-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-brand-sky);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.info-content {
  flex: 1;
}

.info-title {
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 0.25rem;
}

.info-desc {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  margin: 0;
}

.init-details {
  margin-top: 1.5rem;
  padding: 1.25rem;
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.details-title {
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 0.75rem;
}

.details-list {
  margin: 0;
  padding-left: 1.25rem;
  color: var(--color-text-secondary);
}

.details-list li {
  margin: 0.5rem 0;
  line-height: 1.5;
}

/* 完成页面 */
.complete-card {
  text-align: center;
  padding: 2rem 1rem;
}

.complete-icon {
  margin-bottom: 1.5rem;
}

.success-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-brand-accent) 0%, #6ee7d8 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  color: white;
  box-shadow: 0 8px 24px rgba(78, 205, 196, 0.3);
}

.complete-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 0.5rem;
}

.complete-subtitle {
  font-size: 1rem;
  color: var(--color-text-secondary);
  margin: 0 0 2rem;
}

.complete-info {
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  padding: 1.5rem;
  margin-bottom: 2rem;
  text-align: left;
  max-width: 400px;
  margin-left: auto;
  margin-right: auto;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--color-border);
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

.info-value {
  font-weight: 500;
  color: var(--color-text);
}

/* 按钮样式 */
.step-actions {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--color-border);
}

.btn-primary,
.btn-secondary {
  padding: 0.75rem 1.5rem;
  border-radius: var(--radius-full);
  font-size: 0.9375rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  border: none;
  min-width: 120px;
}

.btn-primary {
  background: var(--gradient-primary);
  color: white;
  box-shadow: var(--shadow-brand);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(255, 107, 157, 0.35);
}

.btn-primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-secondary {
  background: var(--color-bg-secondary);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.btn-secondary:hover:not(:disabled) {
  background: var(--color-bg-tertiary);
  border-color: var(--color-brand-primary);
  color: var(--color-brand-primary);
}

.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-large {
  padding: 1rem 2rem;
  font-size: 1rem;
  min-width: 200px;
}

.btn-loading {
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

@media (max-width: 640px) {
  .install-wizard {
    padding: 1.5rem 1rem;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .step-actions {
    flex-direction: column;
  }

  .btn-primary,
  .btn-secondary {
    width: 100%;
  }

  .card-title {
    font-size: 1.25rem;
  }

  .complete-title {
    font-size: 1.5rem;
  }
}
</style>
