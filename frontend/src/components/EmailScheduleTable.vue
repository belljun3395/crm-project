<script setup lang="ts">
import type { EmailNotificationScheduleDto } from '../types/Email';

// schedules: 이메일 스케줄 목록을 prop으로 받음
defineProps<{ schedules: EmailNotificationScheduleDto[] }>();

// 날짜 포맷 함수
function formatDate(dateStr: string | undefined) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleString();
}

// 사용자 ID 목록을 문자열로 변환
function formatUserIds(userIds: number[] | undefined): string {
  if (!userIds || userIds.length === 0) return '';
  return userIds.join(', ');
}

</script>

<template>
  <table class="user-table">
    <thead>
      <tr>
        <th>Task Name</th>
        <th>Template ID</th>
        <th>Recipient User IDs</th>
        <th>Expired Time</th>
        <th></th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="schedule in schedules" :key="schedule.taskName">
        <td>{{ schedule.taskName }}</td>
        <td>{{ schedule.templateId }}</td>
        <td>{{ formatUserIds(schedule.userIds) }}</td>
        <td>{{ formatDate(schedule.expiredTime) }}</td>
        <td>
          <button class="btn blue" @click="$emit('cancel', schedule.taskName)">취소</button>
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