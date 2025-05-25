<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import api from '../api';
import type { User, EnrollUserRequest } from '../types/User';
import UserTable from '../components/UserTable.vue';
import UserCreateModal from '../components/UserCreateModal.vue';

const users = ref<User[]>([]);
const totalCount = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const isModalVisible = ref(false);
const userToEdit = ref<User | undefined>(undefined);

// 사용자 목록 및 총 개수 가져오기
async function fetchUsers() {
  loading.value = true;
  try {
    console.log(`Fetching users: page=${currentPage.value}, pageSize=${pageSize.value}`);
    // Corrected API call using wrapper functions
    const response = await api.getUsers(); // TODO: Pagination 지원 시 파라미터 추가
    users.value = response.users || [];
    totalCount.value = response.totalCount || 0;
    console.log('Users fetched:', users.value);
    console.log('Total count:', totalCount.value);
  } catch (error) {
    console.error('Error fetching users:', error);
    users.value = [];
    totalCount.value = 0;
  } finally {
    loading.value = false;
  }
}

// 페이지 변경 감지
watch(currentPage, fetchUsers);

// 컴포넌트 마운트 시 사용자 목록 가져오기
onMounted(fetchUsers);

// 모달 열기 (등록)
function openCreateModal() {
  userToEdit.value = undefined; // 등록 모드
  isModalVisible.value = true;
}

// 모달 열기 (수정)
function openEditModal(user: User) {
  userToEdit.value = user; // 수정 모드
  isModalVisible.value = true;
}

// 모달 닫기
function closeUserModal() {
  isModalVisible.value = false;
}

// 사용자 저장/수정 완료 후 목록 갱신
function handleUserSaved() {
  fetchUsers(); // 목록 다시 불러오기
}

// 사용자 삭제
async function deleteUser(userId: number) {
  if (confirm('Are you sure you want to delete this user?')) {
    try {
      //await api.delete(`/users/${userId}`);
      // await api.deleteUser(userId); // TODO: OpenAPI에 deleteUser 엔드포인트 없음. 필요시 구현
      alert('User deleted successfully.');
      fetchUsers(); // 목록 갱신
    } catch (error) {
      console.error('Error deleting user:', error);
      alert('Failed to delete user.');
    }
  }
}

// 페이지네이션 계산
const totalPages = ref(1);
watch(totalCount, (newVal) => {
  totalPages.value = Math.ceil(newVal / pageSize.value);
});

function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value--;
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++;
  }
}
</script>

<template>
  <div class="users-view">
    <h2>User Management</h2>

    <button @click="openCreateModal" class="add-user-button">Add New User</button>

    <UserTable :users="users" :loading="loading" @edit-user="openEditModal" @delete-user="deleteUser" />

    <!-- 페이지네이션 -->
    <div class="pagination">
      <button @click="prevPage" :disabled="currentPage === 1">Previous</button>
      <span>Page {{ currentPage }} of {{ totalPages }}</span>
      <button @click="nextPage" :disabled="currentPage === totalPages">Next</button>
    </div>

    <!-- 사용자 등록/수정 모달 -->
    <UserCreateModal :show="isModalVisible" :user="userToEdit" @close="closeUserModal" @saved="handleUserSaved" />

  </div>
</template>

<style scoped>
.users-view {
  padding: 20px;
}

h2 {
  color: #333;
  margin-bottom: 1.5rem;
}

.add-user-button {
  background-color: #28a745;
  color: white;
  padding: 10px 15px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 1rem;
  margin-bottom: 1.5rem;
  transition: background-color 0.2s ease;
}

.add-user-button:hover {
  background-color: #218838;
}

.pagination {
  margin-top: 1.5rem;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
}

.pagination button {
  padding: 8px 12px;
  border: 1px solid #ccc;
  border-radius: 4px;
  cursor: pointer;
  background-color: #f8f9fa;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination button:hover:not(:disabled) {
  background-color: #e2e6ea;
}
</style> 