<template>
  <!-- 모달 전체 배경 (클릭 시 닫힘) -->
  <div v-if="show" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <!-- 모달 제목: 등록/수정 구분 -->
      <h3>{{ user ? 'Edit' : 'Add' }} User</h3>
      <form @submit.prevent="saveUser">
        <!-- 외부 시스템 ID 입력 -->
        <div class="form-group">
          <label for="externalId">External ID:</label>
          <input id="externalId" v-model="editableUser.externalId" required />
        </div>
        <!-- 사용자 속성: key-value 쌍 동적 입력 -->
        <div class="form-group">
          <label for="userAttributes">User Attributes (JSON):</label>
          <textarea id="userAttributes" v-model="editableUser.userAttributes" required rows="5"></textarea>
        </div>
        <div class="modal-actions">
          <!-- 등록/수정 구분 버튼 -->
          <button type="submit" class="btn blue" :disabled="loading">{{ loading ? 'Saving...' : 'Save User' }}</button>
          <button type="button" class="btn" @click="closeModal" :disabled="loading">Cancel</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import api from '../api';
import type { User, EnrollUserRequest } from '../types/User';

const props = defineProps<{ 
  show: boolean; // 모달 표시 여부
  user?: User; // 수정 시 전달될 사용자 데이터
}>();

const emit = defineEmits(['close', 'saved']);

const editableUser = ref<EnrollUserRequest>({
  externalId: '',
  userAttributes: '',
});

const loading = ref(false);

// props.user 변경 감지하여 editableUser 업데이트
watch(() => props.user, (newVal) => {
  if (newVal) {
    // Note: Copy all properties, including the optional id for update
    editableUser.value = { ...newVal };
  } else {
    resetForm();
  }
}, { immediate: true }); // 컴포넌트 마운트 시에도 watch 실행

// 폼 초기화
function resetForm() {
  editableUser.value = {
    externalId: '',
    userAttributes: '',
  };
}

// 모달 닫기
function closeModal() {
  resetForm();
  emit('close');
}

// 사용자 저장 (등록 또는 수정)
async function saveUser() {
  loading.value = true;
  try {
    const dataToSave: EnrollUserRequest = { ...editableUser.value };
    
    // externalId와 userAttributes는 필수
    if (!dataToSave.externalId || !dataToSave.userAttributes) {
        alert('External ID와 User Attributes를 모두 입력해주세요.');
        return;
    }

    const res = await api.createUser(dataToSave);
    
    // 응답 결과에 따라 성공/실패 처리 (openapi.json 스키마 기반)
    if (res.id) { // id가 리턴되면 성공으로 간주
      alert('사용자가 성공적으로 저장되었습니다.');
      emit('saved'); // 저장 후 목록 갱신을 위해 이벤트 발생
      closeModal();
    } else {
       alert('사용자 저장에 실패했습니다.');
    }
  } catch (e) {
    console.error('Error saving user:', e);
    alert('사용자 저장 중 오류가 발생했습니다.');
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: #fff;
  padding: 2rem;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  max-height: 90%;
  overflow-y: auto;
  position: relative;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: bold;
}

.form-group input[type="text"],
.form-group textarea {
  width: 100%;
  padding: 0.8em;
  border: 1px solid #ccc;
  border-radius: 4px;
  box-sizing: border-box; /* 패딩 포함 */
}

.modal-actions {
  margin-top: 2rem;
  text-align: right;
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

.btn + .btn {
  margin-left: 1rem;
}

.btn.blue {
   background: #2563eb;
}
.btn.blue:hover {
    background: #1746a2;
}
</style> 