<script setup lang="ts">
// 이벤트 테이블 컴포넌트
// 이벤트 목록을 테이블로 보여줍니다.
import type { EventDto, SearchEventPropertyDto } from '../types/EventModel';

// events: 이벤트 목록을 prop으로 받음
defineProps<{ events: EventDto[] }>();

// 날짜 포맷 함수 (UserTable, EmailTemplateTable과 동일)
function formatDate(dateStr: string | undefined) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleString();
}

// properties를 key: value 형태로 변환
function formatProperties(properties: SearchEventPropertyDto[] | undefined): string {
  if (!properties || properties.length === 0) return '';
  return properties.map(p => `${p.key}: ${p.value}`).join(', ');
}

</script>

<template>
  <table class="user-table">
    <thead>
      <tr>
        <th>ID</th>
        <th>Event Name</th>
        <th>External ID</th>
        <th>Properties</th>
        <th>Created At</th>
        <!-- <th></th>  액션 컬럼 (필요시 추가) -->
      </tr>
    </thead>
    <tbody>
      <tr v-for="event in events" :key="event.id">
        <td>{{ event.id }}</td>
        <td>{{ event.name }}</td>
        <td>{{ event.externalId }}</td>
        <td>{{ formatProperties(event.properties) }}</td>
        <td>{{ formatDate(event.createdAt) }}</td>
        <!-- <td> 액션 버튼 </td> -->
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