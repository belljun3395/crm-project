<script setup lang="ts">
// appTitle을 prop으로 받아서 로고에 사용
const props = defineProps<{ appTitle?: string }>();
</script>

<template>
  <aside class="sidebar">
    <div class="logo">{{ props.appTitle || 'CRM Admin' }}</div>
    <nav>
      <router-link to="/users" active-class="active">Users</router-link>
      <router-link to="/emails" active-class="active">Emails</router-link>
      <router-link to="/email-schedules" active-class="active">Email Schedules</router-link>
      <router-link to="/events" active-class="active">Events</router-link>
      <!-- 앞으로 메뉴 추가 가능 -->
    </nav>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 200px;
  min-width: 160px;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 2rem 0 0 0;
  position: fixed;
  left: 0; top: 0; bottom: 0;
}
.logo {
  font-weight: bold;
  font-size: 1.2rem;
  color: #2563eb;
  padding: 0 2rem 2rem 2rem;
}
nav {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding-left: 2rem;
}
a {
  color: #22223b;
  text-decoration: none;
  font-size: 1rem;
  padding: 0.5rem 0.7rem;
  border-radius: 6px;
  transition: background 0.2s;
}
a.active, a:hover {
  background: #e6f0fa;
  color: #2563eb;
  font-weight: bold;
}
</style> 