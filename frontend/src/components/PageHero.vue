<template>
  <header class="page-hero" :class="['page-hero--' + variant, { 'page-hero-compact': compact }]">
    <div class="page-hero-inner">
      <template v-if="!compact">
        <h1 class="page-hero-title">{{ title }}</h1>
        <p v-if="subtitle" class="page-hero-subtitle">{{ subtitle }}</p>
      </template>
      <slot />
    </div>
  </header>
</template>

<script setup>
defineProps({
  /** 主标题 */
  title: { type: String, default: '' },
  /** 副标题 */
  subtitle: { type: String, default: '' },
  /** 紧凑模式（详情页用，仅显示 slot） */
  compact: { type: Boolean, default: false },
  /** 渐变色方案：blue | green | purple | amber | rose | indigo | teal */
  variant: { type: String, default: 'blue' },
})
</script>

<style scoped>
.page-hero {
  position: relative;
  padding: 3.5rem 1.5rem 4rem;
  overflow: hidden;
  background-size: 28px 28px, 28px 28px, 100% 100%;
  background-position: 0 0, 0 0, 0 0;
}
/* 各 variant 的网格与底色 */
.page-hero--blue {
  background-image:
    linear-gradient(rgba(46, 104, 184, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(46, 104, 184, 0.035) 1px, transparent 1px),
    linear-gradient(165deg, #f0f7ff 0%, #e8f4f8 40%, #f5f9fc 100%);
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
}
.page-hero--blue::before { background: radial-gradient(ellipse at center, rgba(46, 201, 215, 0.12) 0%, transparent 70%); }
.page-hero--blue::after { background: radial-gradient(ellipse at center, rgba(46, 104, 184, 0.08) 0%, transparent 65%); }

.page-hero--green {
  background-image:
    linear-gradient(rgba(5, 150, 105, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(5, 150, 105, 0.04) 1px, transparent 1px),
    linear-gradient(165deg, #f0fdf4 0%, #ecfdf5 40%, #f0fdf9 100%);
  border-bottom: 1px solid rgba(209, 250, 229, 0.6);
}
.page-hero--green::before { background: radial-gradient(ellipse at center, rgba(52, 211, 153, 0.14) 0%, transparent 70%); }
.page-hero--green::after { background: radial-gradient(ellipse at center, rgba(5, 150, 105, 0.09) 0%, transparent 65%); }

.page-hero--purple {
  background-image:
    linear-gradient(rgba(124, 58, 237, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(124, 58, 237, 0.04) 1px, transparent 1px),
    linear-gradient(165deg, #f5f3ff 0%, #ede9fe 40%, #faf5ff 100%);
  border-bottom: 1px solid rgba(233, 213, 255, 0.6);
}
.page-hero--purple::before { background: radial-gradient(ellipse at center, rgba(167, 139, 250, 0.12) 0%, transparent 70%); }
.page-hero--purple::after { background: radial-gradient(ellipse at center, rgba(124, 58, 237, 0.08) 0%, transparent 65%); }

.page-hero--amber {
  background-image:
    linear-gradient(rgba(217, 119, 6, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(217, 119, 6, 0.04) 1px, transparent 1px),
    linear-gradient(165deg, #fffbeb 0%, #fef3c7 40%, #fffbeb 100%);
  border-bottom: 1px solid rgba(254, 243, 199, 0.7);
}
.page-hero--amber::before { background: radial-gradient(ellipse at center, rgba(251, 191, 36, 0.14) 0%, transparent 70%); }
.page-hero--amber::after { background: radial-gradient(ellipse at center, rgba(217, 119, 6, 0.09) 0%, transparent 65%); }

.page-hero--rose {
  background-image:
    linear-gradient(rgba(225, 29, 72, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(225, 29, 72, 0.035) 1px, transparent 1px),
    linear-gradient(165deg, #fff1f2 0%, #ffe4e6 40%, #fdf2f8 100%);
  border-bottom: 1px solid rgba(254, 226, 226, 0.6);
}
.page-hero--rose::before { background: radial-gradient(ellipse at center, rgba(251, 113, 133, 0.12) 0%, transparent 70%); }
.page-hero--rose::after { background: radial-gradient(ellipse at center, rgba(225, 29, 72, 0.08) 0%, transparent 65%); }

.page-hero--indigo {
  background-image:
    linear-gradient(rgba(79, 70, 229, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(79, 70, 229, 0.04) 1px, transparent 1px),
    linear-gradient(165deg, #eef2ff 0%, #e0e7ff 40%, #f5f3ff 100%);
  border-bottom: 1px solid rgba(224, 231, 255, 0.6);
}
.page-hero--indigo::before { background: radial-gradient(ellipse at center, rgba(129, 140, 248, 0.12) 0%, transparent 70%); }
.page-hero--indigo::after { background: radial-gradient(ellipse at center, rgba(79, 70, 229, 0.08) 0%, transparent 65%); }

.page-hero--teal {
  background-image:
    linear-gradient(rgba(13, 148, 136, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(13, 148, 136, 0.04) 1px, transparent 1px),
    linear-gradient(165deg, #f0fdfa 0%, #ccfbf1 40%, #f0fdf9 100%);
  border-bottom: 1px solid rgba(204, 251, 241, 0.6);
}
.page-hero--teal::before { background: radial-gradient(ellipse at center, rgba(45, 212, 191, 0.14) 0%, transparent 70%); }
.page-hero--teal::after { background: radial-gradient(ellipse at center, rgba(13, 148, 136, 0.09) 0%, transparent 65%); }

.page-hero::before,
.page-hero::after {
  content: '';
  position: absolute;
  pointer-events: none;
}
.page-hero::before {
  top: -40%;
  right: -20%;
  width: 70%;
  height: 120%;
}
.page-hero::after {
  bottom: -30%;
  left: -15%;
  width: 50%;
  height: 100%;
}

.page-hero-inner {
  position: relative;
  z-index: 1;
  max-width: 640px;
  margin: 0 auto;
  text-align: center;
}

.page-hero-title {
  font-size: 2.25rem;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.02em;
  margin-bottom: 0.5rem;
}

.page-hero-subtitle {
  font-size: 0.9375rem;
  color: #64748b;
  margin-bottom: 1.5rem;
}

.page-hero-inner > :deep(.global-search) {
  margin-top: 0.5rem;
}

.page-hero-compact {
  padding: 1.25rem 1.5rem;
}
.page-hero-compact .page-hero-inner {
  max-width: 900px;
  margin-left: auto;
  margin-right: auto;
  text-align: left;
}
</style>
