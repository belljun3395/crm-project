<script setup lang="ts">
import { ref, watch } from 'vue';
import api from '../api';
import type { EmailTemplate } from '../types/Email';

const props = defineProps<{ show: boolean; template?: EmailTemplate }>();
const emit = defineEmits(['close', 'saved']);

const editableTemplate = ref<EmailTemplate>({
  id: undefined,
  subject: '',
  body: '',
  createdAt: '',
  updatedAt: '',
});

const loading = ref(false);

// props.template 변경 감지하여 editableTemplate 업데이트
watch(() => props.template, (newVal) => {
  if (newVal) {
    // Note: Copy all properties
    editableTemplate.value = { ...newVal };
  } else {
    resetForm();
  }
}, { immediate: true }); // 컴포넌트 마운트 시에도 watch 실행

// 폼 초기화
function resetForm() {
  editableTemplate.value = {
    id: undefined,
    subject: '',
    body: '',
    createdAt: '',
    updatedAt: '',
  };
}

// 모달 닫기
function closeModal() {
  resetForm();
  emit('close');
}

// 템플릿 저장 (등록 또는 수정)
async function saveTemplate() {
  loading.value = true;
  try {
    const dataToSave: EmailTemplate = { ...editableTemplate.value };

    // subject와 body는 필수
    if (!dataToSave.subject || !dataToSave.body) {
        alert('Subject와 Body를 모두 입력해주세요.');
        return;
    }

    let res;
    if (dataToSave.id) {
      // res = await api.updateEmailTemplate(dataToSave.id, dataToSave);
      // TODO: OpenAPI에 updateEmailTemplate 엔드포인트 없음. 필요시 구현
    } else {
      res = await api.createEmailTemplate(dataToSave);
    }

    // 응답 결과에 따라 성공/실패 처리
    if (res && res.id) { // id가 리턴되면 성공으로 간주
      alert('이메일 템플릿이 성공적으로 저장되었습니다.');
      emit('saved'); // 저장 후 목록 갱신을 위해 이벤트 발생
      closeModal();
    } else {
       alert('이메일 템플릿 저장에 실패했습니다.');
    }
  } catch (e) {
    console.error('Error saving email template:', e);
    alert('이메일 템플릿 저장 중 오류가 발생했습니다.');
  } finally {
    loading.value = false;
  }
}

</script>

<template>
  <div v-if="show" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <h3>{{ template ? 'Edit' : 'Add' }} Email Template</h3>

      <form @submit.prevent="saveTemplate">
        <div class="form-group">
          <label for="subject">Subject:</label>
          <input id="subject" v-model="editableTemplate.subject" required />
        </div>

        <div class="form-group">
          <label for="body">Body (HTML/Text):</label>
          <textarea id="body" v-model="editableTemplate.body" required rows="10"></textarea>
        </div>

        <div class="modal-actions">
          <button type="submit" class="btn blue" :disabled="loading">{{ loading ? 'Saving...' : 'Save Template' }}</button>
          <button type="button" class="btn" @click="closeModal" :disabled="loading">Cancel</button>
        </div>
      </form>

    </div>
  </div>
</template>

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
  box-sizing: border-box;
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