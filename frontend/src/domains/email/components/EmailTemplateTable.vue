<script setup lang="ts">
// 이메일 템플릿 테이블 컴포넌트
// 이메일 템플릿 목록을 테이블로 보여주고, 수정/삭제/테스트 이벤트를 발생시킵니다.
import type { EmailTemplate } from '../types/Email';

const props = defineProps<{ emailTemplates: EmailTemplate[], loading: boolean }>();
const emit = defineEmits(['edit-template', 'delete-template', 'send-test-email']);

// HTML 태그 제거 함수 (미리보기용)
function stripHtml(html: string): string {
  const div = document.createElement('div');
  div.innerHTML = html;
  return div.textContent || div.innerText || '';
}
</script>

<template>
  <table class="user-table">
    <thead>
      <tr>
        <th>ID</th>
        <th>Subject</th>
        <th>Body (Preview)</th>
        <th>Created At</th>
        <th>Updated At</th>
        <th>Actions</th>
      </tr>
    </thead>
    <tbody>
      <tr v-if="loading">
        <td colspan="6" style="text-align: center;">Loading email templates...</td>
      </tr>
      <tr v-else-if="emailTemplates.length === 0">
        <td colspan="6" style="text-align: center;">No email templates found.</td>
      </tr>
      <tr v-else v-for="template in emailTemplates" :key="template.id">
        <td>{{ template.id }}</td>
        <td>{{ template.subject }}</td>
        <td>{{ stripHtml(template.body).substring(0, 100) + '...' }}</td>
        <td>{{ new Date(template.createdAt).toLocaleString() }}</td>
        <td>{{ new Date(template.updatedAt).toLocaleString() }}</td>
        <td>
          <button class="btn blue" @click="emit('edit-template', template)">수정</button>
          <button class="btn blue" @click="emit('delete-template', template.id)">삭제</button>
          <button class="btn blue" @click="emit('send-test-email', template.id)">테스트</button>
        </td>
      </tr>
    </tbody>
  </table>
</template>

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
.btn {
  background: #60a5fa;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 0.5em 1.2em;
  cursor: pointer;
  font-weight: 500;
  font-size: 1rem;
  transition: background 0.2s ease;
}
.btn:hover {
  background: #3b82f6;
}
.btn.blue {
  background: #2563eb;
}
.btn.blue:hover {
  background: #1746a2;
}
td button {
  margin-right: 0.5em;
}
</style> 