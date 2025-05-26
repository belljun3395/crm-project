<template>
  <table class="user-table">
    <thead>
      <tr>
        <th>Id</th>
        <th>External ID</th>
        <th>User Attributes</th>
        <th>Created At</th>
        <th>Updated At</th>
        <th></th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="user in users" :key="user.id">
        <td>{{ user.id }}</td>
        <td>{{ user.externalId }}</td>
        <td>
          <div v-if="isJson(user.userAttributes)">
            <div v-for="(value, key) in parseJson(user.userAttributes)" :key="key">
              <strong>{{ key }}</strong>: {{ value }}<br />
            </div>
          </div>
          <div v-else>
            {{ user.userAttributes }}
          </div>
        </td>
        <td>{{ formatDate(user.createdAt) }}</td>
        <td>{{ formatDate(user.updatedAt) }}</td>
        <td>
          <button class="btn edit" @click="$emit('edit', user)">수정</button>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<script setup lang="ts">
// 사용자 테이블 컴포넌트
// 사용자 목록을 테이블 형태로 보여주고, 수정 이벤트를 발생시킵니다.
import type { User } from '../types/UserModel';

// users: 사용자 목록을 prop으로 받음
defineProps<{ users: User[] }>();

// 날짜 포맷 함수
function formatDate(dateStr: string) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleString();
}

// userAttributes가 JSON 문자열인지 확인
function isJson(str: string): boolean {
  try {
    JSON.parse(str);
    return true;
  } catch {
    return false;
  }
}

// userAttributes를 객체로 파싱
function parseJson(str: string): Record<string, any> {
  try {
    return JSON.parse(str);
  } catch {
    return {};
  }
}
</script>

<style scoped>
.user-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 1rem;
  background: #fff;
}
th, td {
  padding: 0.7em 1em;
  text-align: left;
  border-bottom: 1px solid #f3f4f6;
}
th {
  color: #6b7280;
  font-weight: 600;
  background: #f9fafb;
}
.btn.edit {
  background: #f3f4f6;
  color: #2563eb;
  border: none;
  border-radius: 6px;
  padding: 0.3em 1em;
  cursor: pointer;
  font-weight: 500;
  font-size: 0.95rem;
  transition: background 0.2s;
}
.btn.edit:hover {
  background: #e6f0fa;
}
</style> 