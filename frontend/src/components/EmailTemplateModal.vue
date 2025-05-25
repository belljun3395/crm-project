<template>
  <div v-if="show" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <h3>{{ isEditing ? 'Edit' : 'Add' }} Email Template</h3>
      
      <form @submit.prevent="saveTemplate">
        <div class="form-group">
          <label for="templateName">Template Name:</label>
          <input id="templateName" v-model="editableTemplate.templateName" required />
        </div>
        
        <div class="form-group">
          <label for="subject">Subject:</label>
          <input id="subject" v-model="editableTemplate.subject" />
        </div>

        <div class="form-group">
          <label for="body">Body:</label>
          <textarea id="body" v-model="editableTemplate.body" required rows="10"></textarea>
        </div>

        <div class="form-group">
          <label for="variables">Variables (comma-separated):</label>
          <input id="variables" v-model="variablesInput" placeholder="e.g., name:default, email" />
        </div>
        
        <div class="modal-actions">
          <button type="submit" class="btn blue">Save</button>
          <button type="button" class="btn" @click="closeModal">Cancel</button>
        </div>
      </form>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import type { TemplateDto, PostTemplateRequest } from '../types/Email';
import api from '../api';

const props = defineProps<{ 
  show: boolean; // 모달 표시 여부
  template?: TemplateDto; // 수정 시 전달될 템플릿 데이터
}>();

const emit = defineEmits(['close', 'saved']);

const editableTemplate = ref<PostTemplateRequest>({
  templateName: '',
  subject: '',
  body: '',
  variables: [],
});

const variablesInput = ref('');

const isEditing = computed(() => !!props.template);

// props.template 변경 감지하여 editableTemplate 업데이트
watch(() => props.template, (newVal) => {
  if (newVal) {
    editableTemplate.value = { ...newVal, id: newVal.id };
    variablesInput.value = newVal.variables ? newVal.variables.join(', ') : '';
  } else {
    resetForm();
  }
}, { immediate: true }); // 컴포넌트 마운트 시에도 watch 실행

// variablesInput 변경 감지하여 editableTemplate.variables 업데이트
watch(variablesInput, (newVal) => {
  editableTemplate.value.variables = newVal ? newVal.split(',').map(v => v.trim()).filter(v => v) : [];
});

// 폼 초기화
function resetForm() {
  editableTemplate.value = {
    templateName: '',
    subject: '',
    body: '',
    variables: [],
  };
  variablesInput.value = '';
}

// 모달 닫기
function closeModal() {
  resetForm();
  emit('close');
}

// 템플릿 저장 (등록 또는 수정)
async function saveTemplate() {
  try {
    const dataToSave: PostTemplateRequest = { ...editableTemplate.value };
    
    // ID가 있으면 수정, 없으면 등록
    if (isEditing.value) {
      // 수정 API는 openapi.json에 직접적으로 정의되어 있지 않지만, PostTemplateRequest 스키마에 id가 포함되어 있으므로 POST로 수정한다고 가정
      // 만약 PUT이나 다른 엔드포인트가 있다면 수정 필요
      await api.createEmailTemplate(dataToSave); // OpenAPI에 update(수정) 엔드포인트 없음. 필요시 구현
    } else {
      await api.postEmailTemplate(dataToSave);
    }
    
    alert('템플릿이 성공적으로 저장되었습니다.');
    emit('saved'); // 저장 후 목록 갱신을 위해 이벤트 발생
    closeModal();
  } catch (e) {
    console.error('Error saving template:', e);
    alert('템플릿 저장 중 오류가 발생했습니다.');
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

.modal-actions {
  margin-top: 2rem;
  text-align: right;
}

.btn {
  background: #60a5fa; /* UserTable의 파란색 버튼과 통일 */
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