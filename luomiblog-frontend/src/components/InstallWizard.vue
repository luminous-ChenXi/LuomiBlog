<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { api } from '../utils/api';
import type { FaviconConfigRequest } from '../types/api';

const currentStep = ref(0);
const loading = ref(false);
const installStatus = ref<{ installed: boolean; locked: boolean; hasData: boolean; message: string } | null>(null);
const dbTestPassed = ref(false);
const sqlExecuted = ref(false); // 标记SQL脚本是否已执行

// 表单数据
const envForm = reactive({
  javaVersion: '',
  portAvailable: false,
  mysqlDriver: false
});

// 环境检测日志
const envLogs = ref<string[]>([]);
const showEnvLogs = ref(false);

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

// 监听步骤变化，防止跳过SQL初始化步骤
watch(currentStep, (newStep, oldStep) => {
  // 如果用户试图从第3步（初始化数据）跳到第7步（创建管理员）或更后，但SQL未执行
  if (newStep >= 6 && oldStep < 6 && !sqlExecuted.value) {
    ElMessage.warning('请先完成数据库初始化步骤');
    currentStep.value = 2; // 强制回到第3步（初始化数据）
  }
});

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

// SMTP配置表单
const smtpForm = reactive({
  enabled: false,
  host: '',
  port: 587,
  username: '',
  password: '',
  fromName: 'LuomiBlog',
  fromEmail: '',
  useSsl: true
});

// 跳过SMTP配置
const skipSmtp = () => {
  currentStep.value++;
};

// 跳过网站图标配置
const skipFavicon = () => {
  currentStep.value++;
};

// 图标加载错误处理
const onFaviconError = () => {
  ElMessage.warning('图标加载失败，请检查URL是否有效');
};

// 网站图标配置表单
const defaultFavicon = `<svg t="1773292844613" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="21701" width="200" height="200"><path d="M810.667 490.667L787.2 552.533h-61.867l51.2 38.4-23.466 70.4 57.6-42.666 57.6 42.666-23.467-70.4 51.2-38.4h-61.867l-23.466-61.866zM539.733 448l57.6-42.667 57.6 42.667-23.466-70.4 51.2-38.4H620.8l-23.467-61.867-23.466 61.867H512l51.2 38.4-23.467 70.4z m441.6-320H896l-32-85.333L832 128h-85.333l70.4 53.333-32 96 81.066-59.733 81.067 59.733-32-96L981.333 128zM362.667 339.2c0-113.067 40.533-215.467 106.666-296.533C230.4 64 42.667 266.667 42.667 512c0 260.267 209.066 469.333 469.333 469.333 147.2 0 298.667-68.266 384-172.8-14.933 2.134-49.067 2.134-64 2.134-260.267 0-469.333-211.2-469.333-471.467zM512 938.667c-234.667 0-426.667-192-426.667-426.667 0-187.733 119.467-349.867 292.267-405.333-36.267 72.533-57.6 153.6-57.6 234.666 0 268.8 204.8 488.534 467.2 512-76.8 53.334-177.067 85.334-275.2 85.334z" p-id="21702"></path></svg>`;

const faviconForm = reactive({
  type: 'svg', // 'svg' | 'url'
  svgCode: defaultFavicon,
  url: ''
});

// 判断是否为有效的SVG代码
const isValidSvg = (code: string): boolean => {
  return code.trim().startsWith('<svg') && code.trim().endsWith('</svg>');
};

// 获取预览图标内容
const getFaviconPreview = computed(() => {
  if (faviconForm.type === 'svg') {
    return isValidSvg(faviconForm.svgCode) ? faviconForm.svgCode : defaultFavicon;
  }
  return faviconForm.url || '';
});

// 保存网站图标配置
const saveFaviconConfig = async () => {
  if (faviconForm.type === 'svg') {
    if (!faviconForm.svgCode.trim()) {
      ElMessage.warning('请输入SVG代码');
      return;
    }
    if (!isValidSvg(faviconForm.svgCode)) {
      ElMessage.warning('请输入有效的SVG代码（必须以<svg开头，以</svg>结尾）');
      return;
    }
  } else {
    if (!faviconForm.url.trim()) {
      ElMessage.warning('请输入图标URL');
      return;
    }
    // 简单的URL验证
    try {
      new URL(faviconForm.url);
    } catch {
      ElMessage.warning('请输入有效的URL地址');
      return;
    }
  }

  loading.value = true;
  try {
    const config: FaviconConfigRequest = faviconForm.type === 'svg'
      ? { type: 'svg', content: faviconForm.svgCode }
      : { type: 'url', content: faviconForm.url };
    const response = await api.install.saveFaviconConfig(config);
    if (response.success) {
      ElMessage.success('网站图标配置保存成功');
      currentStep.value++;
    } else {
      ElMessage.error(response.message);
    }
  } catch (error: any) {
    ElMessage.error(error.message || '网站图标配置保存失败');
  } finally {
    loading.value = false;
  }
};

// 检查安装状态
const checkInstallStatus = async () => {
  try {
    const response = await api.install.getStatus();
    installStatus.value = response;
    
    // 如果已安装且已锁定，直接跳转到首页
    if (response.locked) {
      ElMessageBox.alert(
        '<div style="text-align: left;">' +
        '<p style="margin-bottom: 12px; font-weight: 500;">系统已安装完成</p>' +
        '<p style="margin-bottom: 8px; color: #666; font-size: 13px;">如需重新安装，请按以下步骤操作：</p>' +
        '<ol style="margin: 0; padding-left: 16px; color: #666; font-size: 13px; line-height: 1.8;">' +
        '<li>删除后端目录下的 <code style="background: #f5f5f5; padding: 2px 6px; border-radius: 3px;">install.lock</code> 文件</li>' +
        '<li>重新访问安装页面</li>' +
        '<li>输入管理员/博主密码进行验证</li>' +
        '</ol>' +
        '</div>',
        '提示',
        {
          confirmButtonText: '前往首页',
          showClose: false,
          closeOnClickModal: false,
          closeOnPressEscape: false,
          dangerouslyUseHTMLString: true,
          callback: () => {
            window.location.href = '/';
          }
        }
      );
      return;
    }
    
    // 如果有数据但未锁定，需要二次验证
    if (response.hasData) {
      showReinstallVerify.value = true;
    }
  } catch (error) {
    console.error('检查安装状态失败', error);
  }
};

// 重新安装验证
const showReinstallVerify = ref(false);
const verifyPassword = ref('');
const verifying = ref(false);

// 重新安装选项
const showReinstallOptions = ref(false);
const reinstallOptions = ref<Array<{ code: string; name: string; description: string }>>([]);
const selectedReinstallOption = ref('');
const executingReinstall = ref(false);

const verifyReinstall = async () => {
  if (!verifyPassword.value) {
    ElMessage.warning('请输入管理员密码');
    return;
  }

  verifying.value = true;
  try {
    const response = await api.install.verifyReinstall(verifyPassword.value);
    if (response.success) {
      ElMessage.success('验证通过');
      showReinstallVerify.value = false;

      // 如果需要选择安装选项，显示选项对话框
      if (response.needsOptions) {
        await loadReinstallOptions();
      } else {
        // 不需要选项，直接继续安装
        installStatus.value = {
          ...installStatus.value!,
          hasData: false,
          installed: false,
          locked: false,
          message: '验证通过，可以重新安装'
        };
      }
    } else {
      ElMessage.error(response.message || '验证失败');
    }
  } catch (error: any) {
    ElMessage.error(error.message || '验证失败，请检查密码是否正确');
  } finally {
    verifying.value = false;
  }
};

// 加载重新安装选项
const loadReinstallOptions = async () => {
  try {
    const response = await api.install.getReinstallOptions();
    reinstallOptions.value = response.options;
    showReinstallOptions.value = true;
  } catch (error: any) {
    ElMessage.error(error.message || '加载安装选项失败');
  }
};

// 执行重新安装
const executeReinstall = async () => {
  if (!selectedReinstallOption.value) {
    ElMessage.warning('请选择安装方式');
    return;
  }

  const option = reinstallOptions.value.find(o => o.code === selectedReinstallOption.value);
  if (!option) return;

  // 检查数据库配置是否已填写
  if (!dbForm.host || !dbForm.database || !dbForm.username) {
    ElMessage.warning('请先完成数据库配置');
    showReinstallOptions.value = false;
    // 跳转到数据库配置步骤
    currentStep.value = 1;
    return;
  }

  // 全新安装需要确认
  if (selectedReinstallOption.value === 'fresh_install') {
    try {
      await ElMessageBox.confirm(
        '全新安装将删除所有现有数据（包括用户、文章、评论等），此操作不可恢复！',
        '警告：数据将永久丢失',
        {
          confirmButtonText: '确认删除所有数据',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        }
      );
    } catch {
      return; // 用户取消
    }
  }

  executingReinstall.value = true;
  try {
    const response = await api.install.executeReinstall(
      selectedReinstallOption.value,
      dbForm
    );

    if (response.success) {
      ElMessage.success(response.message);
      showReinstallOptions.value = false;

      // 更新安装状态
      installStatus.value = {
        ...installStatus.value!,
        hasData: false,
        installed: false,
        locked: false,
        message: '重新安装准备完成'
      };

      // 如果选择的是保留数据或更新结构，进入初始化数据步骤
      // 如果选择的是全新安装，数据库已清空并初始化完成，直接进入站点配置步骤
      if (selectedReinstallOption.value === 'fresh_install') {
        sqlExecuted.value = true; // 标记SQL已执行
        currentStep.value = 3; // 跳到站点配置步骤
        ElMessage.success('全新安装完成，请配置站点信息');
      } else {
        currentStep.value = 2; // 跳到初始化数据步骤
      }
    } else {
      ElMessage.error(response.message || '重新安装失败');
    }
  } catch (error: any) {
    ElMessage.error(error.message || '重新安装失败');
  } finally {
    executingReinstall.value = false;
  }
};

// 环境检测
const checkEnvironment = async () => {
  loading.value = true;
  envLogs.value = [];
  showEnvLogs.value = true;
  try {
    const response = await api.install.checkEnvironment();

    // 保存日志
    envLogs.value = response.logs || [];

    const javaCheck = response.checks.find(c => c.name === 'Java 版本');
    const backendCheck = response.checks.find(c => c.name === '后端服务');
    const driverCheck = response.checks.find(c => c.name === 'MySQL 驱动');

    envForm.javaVersion = javaCheck?.message || '';
    envForm.portAvailable = backendCheck?.passed || false;
    envForm.mysqlDriver = driverCheck?.passed || false;

    if (response.allPassed) {
      ElMessage.success('环境检测通过');
      setTimeout(() => {
        currentStep.value++;
      }, 500);
    } else {
      const failedChecks = response.checks.filter(c => !c.passed);
      const messages = failedChecks.map(c => `${c.name}: ${c.suggestion}`).join('\n');
      ElMessageBox.alert(messages, '环境检测未通过', {
        confirmButtonText: '我知道了'
      });
    }
  } catch (error: any) {
    envLogs.value.push(`[ERROR] 环境检测失败: ${error.message || '未知错误'}`);
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

// 数据库检查日志
const dbCheckLogs = ref<string[]>([]);
const showDbCheckLogs = ref(false);

// 测试数据库连接
const testDatabase = async () => {
  if (!dbForm.host || !dbForm.database || !dbForm.username) {
    ElMessage.warning('请填写完整的数据库配置');
    return;
  }

  loading.value = true;
  dbCheckLogs.value = [];
  showDbCheckLogs.value = true;
  try {
    const response = await api.install.checkDatabase(dbForm);

    // 保存日志
    dbCheckLogs.value = response.logs || [];

    if (response.connected) {
      ElMessage.success(`数据库连接成功，MySQL ${response.mysqlVersion}`);
      dbTestPassed.value = true;

      // 如果检测到已有数据，显示重新安装选项
      if (response.needsReinstallOptions) {
        ElMessage.warning(response.existingDataMessage || '检测到数据库已有数据');
        // 加载重新安装选项
        await loadReinstallOptions();
      }
    } else {
      ElMessage.error(response.message || '数据库连接失败');
      dbTestPassed.value = false;
    }
  } catch (error: any) {
    dbTestPassed.value = false;
    dbCheckLogs.value.push(`[ERROR] 数据库检查失败: ${error.message || '未知错误'}`);
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
      sqlExecuted.value = true; // 标记SQL已执行
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

  // 检查SQL脚本是否已执行
  if (!sqlExecuted.value) {
    ElMessage.error('请先完成数据库初始化步骤');
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

// 测试SMTP配置
const testSmtp = async () => {
  if (!smtpForm.host) {
    ElMessage.warning('请输入SMTP服务器地址');
    return;
  }
  if (!smtpForm.username) {
    ElMessage.warning('请输入发件人邮箱');
    return;
  }
  if (!smtpForm.password) {
    ElMessage.warning('请输入邮箱授权码');
    return;
  }

  loading.value = true;
  try {
    const response = await api.install.testSmtp(smtpForm);
    if (response.success) {
      ElMessage.success('测试邮件发送成功，请查收');
    } else {
      ElMessage.error(response.message || '测试邮件发送失败');
    }
  } catch (error: any) {
    ElMessage.error(error.message || '测试邮件发送失败');
  } finally {
    loading.value = false;
  }
};

// 保存SMTP配置
const saveSmtpConfig = async () => {
  if (!smtpForm.host) {
    ElMessage.warning('请输入SMTP服务器地址');
    return;
  }
  if (!smtpForm.username) {
    ElMessage.warning('请输入发件人邮箱');
    return;
  }
  if (!smtpForm.password) {
    ElMessage.warning('请输入邮箱授权码');
    return;
  }

  loading.value = true;
  try {
    const response = await api.install.saveSmtpConfig(smtpForm);
    if (response.success) {
      ElMessage.success('SMTP配置保存成功');
      currentStep.value++;
    } else {
      ElMessage.error(response.message);
    }
  } catch (error: any) {
    ElMessage.error(error.message || 'SMTP配置保存失败');
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
    <!-- 重新安装验证对话框 -->
    <div v-if="showReinstallVerify" class="reinstall-verify-overlay">
      <div class="reinstall-verify-dialog">
        <div class="verify-header">
          <div class="verify-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              <path d="m9 12 2 2 4-4"/>
            </svg>
          </div>
          <h3 class="verify-title">重新安装验证</h3>
          <p class="verify-desc">检测到系统已有数据，需要验证管理员身份才能重新安装</p>
        </div>
        
        <div class="verify-form">
          <div class="form-group">
            <label class="form-label">请输入任意管理员或博主账号的密码</label>
            <input 
              v-model="verifyPassword" 
              type="password" 
              class="form-input" 
              placeholder="输入管理员或博主密码以验证身份"
              @keyup.enter="verifyReinstall"
            />
          </div>
          
          <div class="verify-actions">
            <a href="/" class="btn-secondary">返回首页</a>
            <button 
              class="btn-primary" 
              :disabled="verifying || !verifyPassword" 
              @click="verifyReinstall"
            >
              <span v-if="verifying" class="btn-loading"></span>
              <span v-else>验证并继续</span>
            </button>
          </div>
        </div>
        
        <div class="verify-warning">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
            <line x1="12" y1="9" x2="12" y2="13"/>
            <line x1="12" y1="17" x2="12.01" y2="17"/>
          </svg>
          <span>重新安装可能需要清除现有数据，请谨慎操作</span>
        </div>
      </div>
    </div>

    <!-- 重新安装选项对话框 -->
    <div v-if="showReinstallOptions" class="reinstall-options-overlay">
      <div class="reinstall-options-dialog">
        <div class="options-header">
          <div class="options-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
            </svg>
          </div>
          <h3 class="options-title">选择安装方式</h3>
          <p class="options-desc">检测到系统已有数据，请选择您需要的安装方式</p>
        </div>

        <div class="options-list">
          <div
            v-for="option in reinstallOptions"
            :key="option.code"
            class="option-card"
            :class="{ active: selectedReinstallOption === option.code, danger: option.code === 'fresh_install' }"
            @click="selectedReinstallOption = option.code"
          >
            <div class="option-radio">
              <div class="radio-circle" :class="{ checked: selectedReinstallOption === option.code }">
                <div v-if="selectedReinstallOption === option.code" class="radio-dot"></div>
              </div>
            </div>
            <div class="option-content">
              <h4 class="option-name">{{ option.name }}</h4>
              <p class="option-description">{{ option.description }}</p>
            </div>
            <div v-if="option.code === 'fresh_install'" class="option-warning-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                <line x1="12" y1="9" x2="12" y2="13"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
            </div>
          </div>
        </div>

        <div class="options-actions">
          <button
            class="btn-secondary"
            @click="showReinstallOptions = false"
          >
            取消
          </button>
          <button
            class="btn-primary"
            :class="{ danger: selectedReinstallOption === 'fresh_install' }"
            :disabled="executingReinstall || !selectedReinstallOption"
            @click="executeReinstall"
          >
            <span v-if="executingReinstall" class="btn-loading"></span>
            <span v-else>确认并继续</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 步骤条 -->
    <div class="steps-container">
      <el-steps :active="currentStep" class="steps" align-center>
        <el-step title="环境检测" />
        <el-step title="数据库配置" />
        <el-step title="初始化数据" />
        <el-step title="站点配置" />
        <el-step title="SMTP配置" />
        <el-step title="网站图标" />
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

        <!-- 日志输出区域 -->
        <div v-if="showEnvLogs && envLogs.length > 0" class="env-logs-section">
          <div class="logs-header">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
              <polyline points="10 9 9 9 8 9"/>
            </svg>
            <span>检测日志</span>
          </div>
          <div class="logs-content">
            <div
              v-for="(log, index) in envLogs"
              :key="index"
              class="log-line"
              :class="{
                'log-info': log.includes('[INFO]'),
                'log-error': log.includes('[ERROR]'),
                'log-warn': log.includes('[WARN]')
              }"
            >
              {{ log }}
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

        <!-- 数据库检查日志 -->
        <div v-if="showDbCheckLogs && dbCheckLogs.length > 0" class="env-logs-section">
          <div class="logs-header">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
              <polyline points="10 9 9 9 8 9"/>
            </svg>
            <span>连接日志</span>
          </div>
          <div class="logs-content">
            <div
              v-for="(log, index) in dbCheckLogs"
              :key="index"
              class="log-line"
              :class="{
                'log-info': log.includes('[INFO]'),
                'log-error': log.includes('[ERROR]'),
                'log-warn': log.includes('[WARN]')
              }"
            >
              {{ log }}
            </div>
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

    <!-- 步骤 5: SMTP配置 -->
    <div v-if="currentStep === 4" class="step-content">
      <div class="step-card">
        <div class="card-header">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
          </div>
          <h3 class="card-title">SMTP邮箱配置</h3>
          <p class="card-desc">配置邮件服务用于发送验证码和通知（可选）</p>
        </div>

        <div class="smtp-notice">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="16" x2="12" y2="12"/>
            <line x1="12" y1="8" x2="12.01" y2="8"/>
          </svg>
          <p>SMTP配置用于发送邮箱验证码、密码重置等邮件。如果暂时不需要邮件功能，可以跳过此步骤。</p>
        </div>

        <div class="install-form">
          <div class="form-group checkbox-group">
            <label class="checkbox-label">
              <input v-model="smtpForm.enabled" type="checkbox" class="checkbox-input" />
              <span class="checkbox-text">启用SMTP邮件服务</span>
            </label>
          </div>

          <template v-if="smtpForm.enabled">
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">SMTP服务器地址</label>
                <input v-model="smtpForm.host" type="text" class="form-input" placeholder="如：smtp.qq.com" />
              </div>
              <div class="form-group">
                <label class="form-label">SMTP端口</label>
                <input v-model.number="smtpForm.port" type="number" class="form-input" placeholder="587" />
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">发件人邮箱</label>
                <input v-model="smtpForm.username" type="email" class="form-input" placeholder="your@email.com" />
              </div>
              <div class="form-group">
                <label class="form-label">邮箱授权码/密码</label>
                <input v-model="smtpForm.password" type="password" class="form-input" placeholder="请输入授权码" />
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">发件人名称</label>
                <input v-model="smtpForm.fromName" type="text" class="form-input" placeholder="LuomiBlog" />
              </div>
              <div class="form-group">
                <label class="form-label">发件人地址</label>
                <input v-model="smtpForm.fromEmail" type="email" class="form-input" placeholder="noreply@yourdomain.com" />
              </div>
            </div>

            <div class="form-group checkbox-group">
              <label class="checkbox-label">
                <input v-model="smtpForm.useSsl" type="checkbox" class="checkbox-input" />
                <span class="checkbox-text">使用SSL/TLS加密连接（推荐）</span>
              </label>
            </div>

            <div class="smtp-test">
              <button class="btn-secondary" :disabled="loading" @click="testSmtp">
                <span v-if="loading" class="btn-loading"></span>
                <span v-else>测试邮件发送</span>
              </button>
              <span class="test-hint">配置完成后建议先测试邮件发送是否正常</span>
            </div>
          </template>
        </div>

        <div class="step-actions">
          <button class="btn-secondary" @click="prevStep">上一步</button>
          <button class="btn-outline" @click="skipSmtp">跳过此步骤</button>
          <button v-if="smtpForm.enabled" class="btn-primary" :disabled="loading" @click="saveSmtpConfig">
            保存并继续
          </button>
        </div>
      </div>
    </div>

    <!-- 步骤 6: 网站图标配置 -->
    <div v-if="currentStep === 5" class="step-content">
      <div class="step-card">
        <div class="card-header">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
          </div>
          <h3 class="card-title">网站图标配置</h3>
          <p class="card-desc">自定义浏览器标签页上显示的网站图标（可选）</p>
        </div>

        <div class="favicon-notice">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="16" x2="12" y2="12"/>
            <line x1="12" y1="8" x2="12.01" y2="8"/>
          </svg>
          <p>您可以粘贴 SVG 代码或图标 URL 来自定义网站图标。如果不设置，将使用默认图标。由于存储策略尚未配置，暂不支持上传图片。</p>
        </div>

        <!-- 图标预览 -->
        <div class="favicon-preview-section">
          <label class="form-label">图标预览</label>
          <div class="favicon-preview-box">
            <div v-if="faviconForm.type === 'svg'" class="favicon-preview-svg" v-html="getFaviconPreview"></div>
            <img v-else-if="faviconForm.url" :src="getFaviconPreview" alt="favicon" class="favicon-preview-img" @error="onFaviconError" />
            <div v-else class="favicon-preview-placeholder">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                <circle cx="8.5" cy="8.5" r="1.5"/>
                <polyline points="21 15 16 10 5 21"/>
              </svg>
              <span>暂无预览</span>
            </div>
          </div>
        </div>

        <div class="install-form">
          <!-- 图标类型选择 -->
          <div class="form-group">
            <label class="form-label">图标来源</label>
            <div class="favicon-type-tabs">
              <button 
                type="button" 
                class="type-tab" 
                :class="{ active: faviconForm.type === 'svg' }" 
                @click="faviconForm.type = 'svg'"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                  <polyline points="10 9 9 9 8 9"/>
                </svg>
                SVG 代码
              </button>
              <button 
                type="button" 
                class="type-tab" 
                :class="{ active: faviconForm.type === 'url' }" 
                @click="faviconForm.type = 'url'"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
                  <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
                </svg>
                图标 URL
              </button>
            </div>
          </div>

          <!-- SVG 代码输入 -->
          <div v-if="faviconForm.type === 'svg'" class="form-group">
            <label class="form-label">
              SVG 代码
              <span class="form-hint-inline">（将自动验证格式）</span>
            </label>
            <textarea 
              v-model="faviconForm.svgCode" 
              class="form-textarea favicon-textarea" 
              rows="6" 
              placeholder="在此粘贴 SVG 代码，例如：<svg>...</svg>"
            ></textarea>
            <p v-if="faviconForm.svgCode && !isValidSvg(faviconForm.svgCode)" class="form-error">
              请输入有效的 SVG 代码（必须以 &lt;svg 开头，以 &lt;/svg&gt; 结尾）
            </p>
          </div>

          <!-- URL 输入 -->
          <div v-else class="form-group">
            <label class="form-label">图标 URL</label>
            <input 
              v-model="faviconForm.url" 
              type="url" 
              class="form-input" 
              placeholder="https://example.com/favicon.ico"
            />
            <p class="form-hint">支持 .ico、.png、.svg 等格式的图标链接</p>
          </div>
        </div>

        <div class="step-actions">
          <button class="btn-secondary" @click="prevStep">上一步</button>
          <button class="btn-outline" @click="skipFavicon">跳过此步骤</button>
          <button class="btn-primary" :disabled="loading || !!(faviconForm.type === 'svg' && faviconForm.svgCode && !isValidSvg(faviconForm.svgCode))" @click="saveFaviconConfig">
            保存并继续
          </button>
        </div>
      </div>
    </div>

    <!-- 步骤 7: 创建管理员 -->
    <div v-if="currentStep === 6" class="step-content">
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

    <!-- 步骤 8: 完成安装 -->
    <div v-if="currentStep === 7" class="step-content">
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

/* SMTP配置样式 */
.smtp-notice {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  background: rgba(255, 107, 157, 0.08);
  border-radius: var(--radius-md);
  border: 1px solid rgba(255, 107, 157, 0.2);
  margin-bottom: 1.5rem;
}

.smtp-notice svg {
  width: 20px;
  height: 20px;
  color: var(--color-brand-primary);
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.smtp-notice p {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.5;
}

.checkbox-group {
  display: flex;
  align-items: center;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-size: 0.875rem;
  color: var(--color-text);
}

.checkbox-input {
  width: 18px;
  height: 18px;
  accent-color: var(--color-brand-primary);
  cursor: pointer;
}

.checkbox-text {
  user-select: none;
}

.smtp-test {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-border);
}

.test-hint {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

.btn-outline {
  padding: 0.75rem 1.5rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-outline:hover {
  border-color: var(--color-brand-primary);
  color: var(--color-brand-primary);
}

/* 重新安装验证对话框 */
.reinstall-verify-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.reinstall-verify-dialog {
  background: var(--color-card);
  border-radius: var(--radius-xl);
  padding: 2.5rem;
  max-width: 420px;
  width: 100%;
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border);
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.verify-header {
  text-align: center;
  margin-bottom: 1.5rem;
}

.verify-icon {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1) 0%, rgba(78, 205, 196, 0.1) 100%);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
  color: var(--color-brand-primary);
}

.verify-title {
  font-size: 1.375rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 0.5rem;
}

.verify-desc {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.5;
}

.verify-form {
  margin-bottom: 1.5rem;
}

.verify-actions {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin-top: 1.5rem;
}

.verify-warning {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.875rem 1rem;
  background: rgba(239, 68, 68, 0.08);
  border-radius: var(--radius-md);
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.verify-warning svg {
  width: 16px;
  height: 16px;
  color: #ef4444;
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.verify-warning span {
  font-size: 0.8125rem;
  color: #ef4444;
  line-height: 1.5;
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
  .btn-secondary,
  .btn-outline {
    width: 100%;
  }

  .card-title {
    font-size: 1.25rem;
  }

  .complete-title {
    font-size: 1.5rem;
  }

  .smtp-test {
    flex-direction: column;
    align-items: flex-start;
  }

  .reinstall-verify-dialog {
    padding: 1.5rem;
    margin: 1rem;
  }

  .verify-actions {
    flex-direction: column;
  }
}

/* 网站图标配置样式 */
.favicon-notice {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  background: rgba(255, 107, 157, 0.08);
  border-radius: var(--radius-md);
  border: 1px solid rgba(255, 107, 157, 0.2);
  margin-bottom: 1.5rem;
}

.favicon-notice svg {
  width: 20px;
  height: 20px;
  color: var(--color-brand-primary);
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.favicon-notice p {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.5;
}

.favicon-preview-section {
  margin-bottom: 1.5rem;
}

.favicon-preview-box {
  width: 100px;
  height: 100px;
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-secondary);
  overflow: hidden;
}

.favicon-preview-svg {
  width: 64px;
  height: 64px;
}

.favicon-preview-svg svg {
  width: 100%;
  height: 100%;
}

.favicon-preview-img {
  width: 64px;
  height: 64px;
  object-fit: contain;
}

.favicon-preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  color: var(--color-text-muted);
}

.favicon-preview-placeholder svg {
  width: 32px;
  height: 32px;
}

.favicon-preview-placeholder span {
  font-size: 0.75rem;
}

.favicon-type-tabs {
  display: flex;
  gap: 0.75rem;
}

.type-tab {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.type-tab:hover {
  border-color: var(--color-brand-primary);
  color: var(--color-brand-primary);
}

.type-tab.active {
  background: linear-gradient(135deg, #ff6b9d 0%, #e87a9f 100%);
  border-color: transparent;
  color: white;
}

.type-tab svg {
  width: 18px;
  height: 18px;
}

.favicon-textarea {
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 0.8125rem;
  resize: vertical;
}

.form-hint-inline {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  font-weight: normal;
  margin-left: 0.5rem;
}

.form-error {
  font-size: 0.875rem;
  color: #dc2626;
  margin: 0.5rem 0 0 0;
}

@media (max-width: 640px) {
  .favicon-type-tabs {
    flex-direction: column;
  }

  .type-tab {
    width: 100%;
    justify-content: center;
  }

  .favicon-preview-box {
    width: 80px;
    height: 80px;
  }
}

/* 重新安装选项对话框样式 */
.reinstall-options-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.reinstall-options-dialog {
  background: var(--color-card);
  border-radius: var(--radius-xl);
  padding: 2.5rem;
  width: 100%;
  max-width: 560px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.options-header {
  text-align: center;
  margin-bottom: 2rem;
}

.options-icon {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, var(--color-brand-primary), var(--color-brand-secondary));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.25rem;
  color: white;
}

.options-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 0.5rem;
}

.options-desc {
  font-size: 0.9375rem;
  color: var(--color-text-secondary);
  margin: 0;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 2rem;
}

.option-card {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1.25rem;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all 0.2s ease;
  background: var(--color-bg-secondary);
}

.option-card:hover {
  border-color: var(--color-brand-primary);
  background: var(--color-card);
}

.option-card.active {
  border-color: var(--color-brand-primary);
  background: rgba(255, 107, 157, 0.05);
}

.option-card.danger {
  border-color: #ef4444;
}

.option-card.danger:hover {
  border-color: #dc2626;
  background: rgba(239, 68, 68, 0.05);
}

.option-card.danger.active {
  border-color: #dc2626;
  background: rgba(239, 68, 68, 0.08);
}

.option-radio {
  flex-shrink: 0;
  padding-top: 0.125rem;
}

.radio-circle {
  width: 20px;
  height: 20px;
  border: 2px solid var(--color-border);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.radio-circle.checked {
  border-color: var(--color-brand-primary);
}

.option-card.danger .radio-circle.checked {
  border-color: #dc2626;
}

.radio-dot {
  width: 10px;
  height: 10px;
  background: var(--color-brand-primary);
  border-radius: 50%;
}

.option-card.danger .radio-dot {
  background: #dc2626;
}

.option-content {
  flex: 1;
}

.option-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 0.375rem;
}

.option-description {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.5;
}

.option-warning-icon {
  color: #ef4444;
  flex-shrink: 0;
}

.options-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.options-actions .btn-primary.danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

.options-actions .btn-primary.danger:hover:not(:disabled) {
  background: linear-gradient(135deg, #dc2626, #b91c1c);
}

/* 环境检测日志样式 */
.env-logs-section {
  margin-top: 1.5rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-bg-secondary);
  overflow: hidden;
}

.logs-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: var(--color-card);
  border-bottom: 1px solid var(--color-border);
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.logs-header svg {
  color: var(--color-brand-primary);
}

.logs-content {
  padding: 1rem;
  max-height: 200px;
  overflow-y: auto;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.8125rem;
  line-height: 1.6;
}

.log-line {
  padding: 0.25rem 0;
  word-break: break-all;
}

.log-info {
  color: var(--color-text-secondary);
}

.log-error {
  color: #ef4444;
}

.log-warn {
  color: #f59e0b;
}

@media (max-width: 640px) {
  .reinstall-options-dialog {
    padding: 1.5rem;
    margin: 1rem;
  }

  .options-actions {
    flex-direction: column;
  }

  .option-card {
    padding: 1rem;
  }

  .logs-content {
    max-height: 150px;
  }
}
</style>
