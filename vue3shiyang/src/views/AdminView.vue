<template>
  <div class="admin-page">
    <div class="page-header">
      <h1>管理后台</h1>
      <p class="subtitle">系统管理和数据统计</p>
    </div>
    <div class="placeholder-content">
      <div class="placeholder-box">
        <div class="placeholder-icon">⚙️</div>
        <h2>管理后台功能开发中</h2>
        <p>这里将包含用户管理、文章管理、系统配置等功能</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.admin-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.page-header {
  text-align: center;
  margin-bottom: 3rem;
}

.page-header h1 {
  font-size: 2rem;
  color: #333;
  margin-bottom: 0.5rem;
}

.subtitle {
  color: #666;
  font-size: 1rem;
}

.placeholder-content {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.placeholder-box {
  text-align: center;
  padding: 3rem;
  background: #f9f9f9;
  border-radius: 12px;
  max-width: 500px;
}

.placeholder-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.placeholder-box h2 {
  font-size: 1.5rem;
  color: #333;
  margin-bottom: 0.5rem;
}

.placeholder-box p {
  color: #666;
  font-size: 1rem;
}
</style>
