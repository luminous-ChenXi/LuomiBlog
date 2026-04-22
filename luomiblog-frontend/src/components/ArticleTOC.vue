<template>
  <div class="toc-container" :class="{ 'floating': isFloating }">
    <button
      v-if="isMobile"
      class="toc-toggle"
      @click="isOpen = !isOpen"
      :class="{ 'active': isOpen }"
    >
      <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="8" x2="21" y1="6" y2="6"/><line x1="8" x2="21" y1="12" y2="12"/><line x1="8" x2="21" y1="18" y2="18"/><line x1="3" x2="3.01" y1="6" y2="6"/><line x1="3" x2="3.01" y1="12" y2="12"/><line x1="3" x2="3.01" y1="18" y2="18"/></svg>
    </button>

    <nav
      class="toc-nav"
      :class="{ 'show': !isMobile || isOpen }"
    >
      <div class="toc-header">
        <h4 class="toc-title">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="8" x2="21" y1="6" y2="6"/><line x1="8" x2="21" y1="12" y2="12"/><line x1="8" x2="21" y1="18" y2="18"/><line x1="3" x2="3.01" y1="6" y2="6"/><line x1="3" x2="3.01" y1="12" y2="12"/><line x1="3" x2="3.01" y1="18" y2="18"/></svg>
          目录
        </h4>
        <button v-if="isMobile" class="toc-close" @click="isOpen = false">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" x2="6" y1="6" y2="18"/><line x1="6" x2="18" y1="6" y2="18"/></svg>
        </button>
      </div>

      <ul class="toc-list">
        <li
          v-for="item in tocItems"
          :key="item.id"
          :class="['toc-item', `level-${item.level}`, { 'active': activeId === item.id }]"
        >
          <a :href="`#${item.id}`" @click.prevent="scrollToSection(item.id)">
            {{ item.text }}
          </a>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';

const props = defineProps<{
  contentSelector?: string;
}>();

const tocItems = ref<Array<{ id: string; text: string; level: number }>>([]);
const activeId = ref('');
const isOpen = ref(false);
const isFloating = ref(false);
const isMobile = ref(false);

const generateTOC = () => {
  const content = document.querySelector(props.contentSelector || '.article-content');
  if (!content) return;

  const headings = content.querySelectorAll('h2, h3, h4');
  tocItems.value = Array.from(headings).map((heading, index) => {
    if (!heading.id) {
      heading.id = `heading-${index}`;
    }
    return {
      id: heading.id,
      text: heading.textContent || '',
      level: parseInt(heading.tagName[1])
    };
  });
};

const scrollToSection = (id: string) => {
  const element = document.getElementById(id);
  if (element) {
    const offset = 100;
    const top = element.getBoundingClientRect().top + window.scrollY - offset;
    window.scrollTo({ top, behavior: 'smooth' });
    isOpen.value = false;
  }
};

const handleScroll = () => {
  const scrollPos = window.scrollY + 150;

  for (let i = tocItems.value.length - 1; i >= 0; i--) {
    const item = tocItems.value[i];
    const element = document.getElementById(item.id);
    if (element && element.offsetTop <= scrollPos) {
      activeId.value = item.id;
      break;
    }
  }

  isFloating.value = window.scrollY > 300;
};

onMounted(() => {
  isMobile.value = window.innerWidth < 1024;
  generateTOC();
  window.addEventListener('scroll', handleScroll, { passive: true });
  handleScroll();
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<style scoped>
.toc-container {
  position: relative;
}

@media (min-width: 1024px) {
  .toc-container {
    position: sticky;
    top: 100px;
    max-height: calc(100vh - 150px);
    overflow-y: auto;
  }

  .toc-nav {
    padding: 1.25rem;
    background: var(--color-card);
    border: 1px solid var(--color-border-soft);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
  }
}

.toc-toggle {
  position: fixed;
  right: 1rem;
  bottom: 6rem;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--color-brand-primary);
  color: white;
  border: none;
  box-shadow: 0 4px 12px rgba(255, 107, 157, 0.4);
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease-in-out;
  cursor: pointer;
}

.toc-toggle.active {
  transform: rotate(90deg);
}

@media (max-width: 1023px) {
  .toc-nav {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 50;
    opacity: 0;
    visibility: hidden;
    transition: all 0.3s ease-in-out;
    padding: 0;
    display: flex;
    align-items: flex-end;
    justify-content: flex-end;
  }

  .toc-nav.show {
    opacity: 1;
    visibility: visible;
  }

  .toc-nav::before {
    content: '';
    position: absolute;
    right: 0;
    top: 0;
    width: 280px;
    height: 100%;
    background: var(--color-card);
    padding: 1.25rem;
    border-radius: var(--radius-lg) 0 0 var(--radius-lg);
  }

  .toc-header,
  .toc-list {
    position: relative;
    z-index: 1;
  }
}

.toc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--color-border-soft);
}

.toc-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.toc-title svg {
  color: var(--color-brand-primary);
}

.toc-close {
  background: none;
  border: none;
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toc-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.toc-item {
  margin-bottom: 0.35rem;
}

.toc-item a {
  display: block;
  padding: 0.35rem 0.5rem;
  border-radius: 6px;
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  transition: all 0.2s ease-in-out;
  border-left: 2px solid transparent;
  text-decoration: none;
  line-height: 1.5;
}

.toc-item.level-3 {
  padding-left: 1rem;
}

.toc-item.level-4 {
  padding-left: 2rem;
}

.toc-item:hover a {
  background: rgba(255, 107, 157, 0.06);
  color: var(--color-text);
}

.toc-item.active a {
  border-left-color: var(--color-brand-primary);
  background: rgba(255, 107, 157, 0.1);
  color: var(--color-brand-primary);
  font-weight: 500;
}
</style>
