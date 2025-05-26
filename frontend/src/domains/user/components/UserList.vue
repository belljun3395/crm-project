<script setup lang="ts">
// 사용자 목록 테이블 (리스트)
// 사용자 목록을 테이블로 보여주고, 수정/삭제 이벤트를 발생시킵니다.
import type { User } from '../types/UserModel';

const props = defineProps<{ users: User[], loading: boolean }>();
const emit = defineEmits(['edit-user', 'delete-user']);

</script>

<template>
  <table>
    <thead>
      <tr>
        <th>ID</th>
        <th>External ID</th>
        <th>Attributes</th>
        <th>Created At</th>
        <th>Actions</th>
      </tr>
    </thead>
    <tbody>
      <tr v-if="loading">
        <td colspan="5" style="text-align: center;">Loading users...</td>
      </tr>
      <tr v-else-if="users.length === 0">
        <td colspan="5" style="text-align: center;">No users found.</td>
      </tr>
      <tr v-else v-for="user in users" :key="user.id">
        <td>{{ user.id }}</td>
        <td>{{ user.externalId }}</td>
        <td>
          <pre>{{ user.userAttributes }}</pre>
        </td>
        <td>{{ new Date(user.createdAt).toLocaleString() }}</td>
        <td>
          <button @click="emit('edit-user', user)">Edit</button>
          <button @click="emit('delete-user', user.id)">Delete</button>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<style scoped>
table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 1.5rem;
}

th,
td {
  border: 1px solid #ddd;
  padding: 0.8rem;
  text-align: left;
}

th {
  background-color: #f2f2f2;
}

tr:nth-child(even) {
  background-color: #f9f9f9;
}

tr:hover {
  background-color: #f1f1f1;
}

button {
  margin-right: 0.5rem;
  padding: 0.3rem 0.7rem;
  cursor: pointer;
  border: 1px solid #ccc;
  border-radius: 4px;
}

button:hover {
  background-color: #e9e9e9;
}

pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
}
</style> 