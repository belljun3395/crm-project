<template>
  <div class="layout">
    <Sidebar :appTitle="APP_TITLE" />
    <div class="main-area">
      <header class="topbar">
        <h1>{{ APP_TITLE }}</h1>
      </header>
      <main>
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import Sidebar from './components/Sidebar.vue';
// 앱 타이틀을 상수로 분리하여 쉽게 교체 가능하게 함
const APP_TITLE = 'CRM Admin';
</script>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
  background: #f7f9fb;
}
.main-area {
  flex: 1;
  min-width: 0;
  padding-left: 200px;
  display: flex;
  flex-direction: column;
}
.topbar {
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  padding: 1.2rem 2rem;
  font-size: 1.3rem;
  font-weight: bold;
  color: #22223b;
  letter-spacing: 1px;
}
main {
  width: 100%;
  max-width: 1100px;
  margin: 2rem auto 0 auto;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 12px #e5e7eb40;
  padding: 2rem 2.5vw;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  transition: max-width 0.2s, padding 0.2s;
}
@media (max-width: 1200px) {
  main {
    max-width: 98vw;
    padding: 2rem 1vw;
  }
}
@media (max-width: 900px) {
  .main-area {
    padding-left: 0;
  }
  main {
    margin: 2rem auto 0 auto;
    max-width: 100vw;
    padding: 1.2rem 0.5rem;
  }
}
@media (max-width: 700px) {
  .sidebar {
    display: none;
  }
  .main-area {
    padding-left: 0;
  }
  main {
    margin: 1rem 0 0 0;
    max-width: 100vw;
    padding: 0.5rem 0.2rem;
  }
}
</style> 