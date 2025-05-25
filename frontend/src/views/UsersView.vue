<template>
  <div>
    <div class="header-row">
      <div class="header-title">
        <h2>Users</h2>
        <span class="user-count">총 {{ totalCount }}명</span>
      </div>
      <button class="btn blue" @click="openCreateModal">+ Add User</button>
    </div>
    <UserTable :users="users" @edit="openEditModal" />
    <UserCreateModal
      v-if="showModal"
      :user="editingUser"
      @close="closeModal"
      @saved="onSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import api from '../api';
import UserTable from '../components/UserTable.vue';
import UserCreateModal from '../components/UserCreateModal.vue';
import type { User } from '../types/User';

const users = ref<User[]>([]);
const showModal = ref(false);
const editingUser = ref<User | undefined>(undefined);
const totalCount = ref<number>(0);

async function fetchUsers() {
  try {
    const res = await api.getUsers(); // TODO: Pagination 지원 시 파라미터 추가
    users.value = res.users ?? [];
  } catch (e) {
    console.error('Error fetching users:', e);
  }
}

async function fetchTotalCount() {
  try {
    const res = await api.getTotalUserCount();
    totalCount.value = res.totalCount ?? 0;
  } catch (e) {
    console.error('Error fetching user count:', e);
  }
}

function openCreateModal() {
  editingUser.value = undefined;
  showModal.value = true;
}
function openEditModal(user: User) {
  editingUser.value = user;
  showModal.value = true;
}
function closeModal() {
  showModal.value = false;
}
async function onSaved() {
  showModal.value = false;
  await fetchUsers();
  await fetchTotalCount();
}
onMounted(async () => {
  await fetchUsers();
  await fetchTotalCount();
});
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}
.header-title {
  display: flex;
  align-items: baseline;
  gap: 1.2rem;
}
.user-count {
  color: #2563eb;
  font-size: 1.1rem;
  font-weight: 500;
  background: #e6f0fa;
  border-radius: 1em;
  padding: 0.2em 1em;
}
.btn {
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 0.5em 1.2em;
  cursor: pointer;
  font-weight: 500;
  font-size: 1rem;
  transition: background 0.2s;
}
.btn:hover {
  background: #1746a2;
}
</style> 