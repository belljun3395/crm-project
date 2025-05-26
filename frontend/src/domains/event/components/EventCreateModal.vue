<template>
  <div v-if="show" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <h3>Add New Event</h3>
      
      <form @submit.prevent="saveEvent">
        <div class="form-group">
          <label for="eventName">Event Name:</label>
          <input id="eventName" v-model="editableEvent.name" required />
        </div>
        
        <div class="form-group">
          <label for="externalId">External ID:</label>
          <input id="externalId" v-model="editableEvent.externalId" required />
        </div>

        <h4>Properties</h4>
        <div class="properties-list">
          <div class="property-item" v-for="(property, index) in editableEvent.properties" :key="index">
            <input v-model="property.key" placeholder="Key" required class="property-input" />
            <input v-model="property.value" placeholder="Value" required class="property-input" />
            <button type="button" class="btn small danger" @click="removeProperty(index)">Remove</button>
          </div>
        </div>
        <button type="button" class="btn small" @click="addProperty">+ Add Property</button>
        
        <div class="modal-actions">
          <button type="submit" class="btn blue" :disabled="loading">{{ loading ? 'Saving...' : 'Save Event' }}</button>
          <button type="button" class="btn" @click="closeModal" :disabled="loading">Cancel</button>
        </div>
      </form>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
// 이벤트 도메인 API를 import
import * as eventApi from '../api';
import type { PostEventRequest, PostEventPropertyDto } from '../types/EventRequest';

const props = defineProps<{ 
  show: boolean; // 모달 표시 여부
}>();

const emit = defineEmits(['close', 'saved']);

const editableEvent = ref<PostEventRequest>({
  name: '',
  externalId: '',
  properties: [],
});

const loading = ref(false);

// Property 추가
function addProperty() {
  editableEvent.value.properties.push({ key: '', value: '' });
}

// Property 제거
function removeProperty(index: number) {
  editableEvent.value.properties.splice(index, 1);
}

// 폼 초기화
function resetForm() {
  editableEvent.value = {
    name: '',
    externalId: '',
    properties: [],
  };
}

// 모달 닫기
function closeModal() {
  resetForm();
  emit('close');
}

// 이벤트 저장
async function saveEvent() {
  // properties 유효성 검사 (key, value 모두 입력되었는지)
  for (const prop of editableEvent.value.properties) {
    if (!prop.key || !prop.value) {
      alert('Property Key와 Value를 모두 입력해주세요.');
      return;
    }
  }

  loading.value = true;
  try {
    // 이벤트 생성 API 호출
    const res = await eventApi.createEvent(editableEvent.value);
    // 응답 결과에 따라 성공/실패 처리
    if (res.id) { // id가 리턴되면 성공으로 간주
      alert('이벤트가 성공적으로 등록되었습니다.');
      emit('saved'); // 저장 후 이벤트 목록 갱신을 위해 이벤트 발생 (필요시)
      closeModal();
    } else {
       alert('이벤트 등록에 실패했습니다.');
    }
  } catch (e) {
    console.error('Error saving event:', e);
    alert('이벤트 등록 중 오류가 발생했습니다.');
  } finally {
    loading.value = false;
  }
}

// 모달이 열릴 때 폼 초기화
watch(() => props.show, (newVal) => {
  if (newVal) {
    resetForm();
  }
});

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
  max-width: 600px;
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

.properties-list {
    margin-bottom: 1rem;
}

.property-item {
    display: flex;
    gap: 0.5rem;
    margin-bottom: 0.5rem;
    align-items: center;
}

.property-input {
    flex: 1; /* 입력 필드가 남은 공간 채우도록 */
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

.btn.small {
  padding: 0.3em 0.8em;
  font-size: 0.9rem;
}

.btn.danger {
  background: #dc2626;
}
.btn.danger:hover {
  background: #991b1b;
}
</style> 