<template>
  <div v-if="show" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <h3>Schedule Email Notification</h3>
      
      <form @submit.prevent="saveSchedule">
        <div class="form-group">
          <label for="template">Select Template:</label>
          <select id="template" v-model="selectedTemplateId" required :disabled="loadingTemplates || !availableTemplates.length">
            <option value="">-- Select a Template --</option>
            <option v-for="template in availableTemplates" :key="template.template.id" :value="template.template.id">
              {{ template.template.templateName }} (ver: {{ template.template.version }})
            </option>
          </select>
           <div v-if="loadingTemplates">Loading templates...</div>
           <div v-if="templateError" class="error-message">Error loading templates.</div>
        </div>
        
        <div class="form-group">
          <label for="userIds">Recipient User IDs (comma-separated):</label>
          <input id="userIds" v-model="userIdsInput" placeholder="e.g., 1, 2, 3" required />
          <p class="help-text">Enter comma-separated User IDs who will receive this email.</p>
        </div>

        <div class="form-group">
          <label for="expiredTime">Expiration Time:</label>
          <!-- Datetime-local input for easy selection -->
          <input type="datetime-local" id="expiredTime" v-model="expiredTimeInput" required />
          <p class="help-text">Schedule the email to be sent before this time.</p>
        </div>
        
        <div class="modal-actions">
          <button type="submit" class="btn blue" :disabled="loading || loadingTemplates || !availableTemplates.length || !selectedTemplateId || !userIdsInput || !expiredTimeInput">{{ loading ? 'Scheduling...' : 'Schedule Email' }}</button>
          <button type="button" class="btn" @click="closeModal" :disabled="loading">Cancel</button>
        </div>
      </form>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import api from '../api';
import type { TemplateWithHistoryDto, PostNotificationEmailRequest } from '../types/Email';

const props = defineProps<{ 
  show: boolean; // 모달 표시 여부
}>();

const emit = defineEmits(['close', 'saved']);

const availableTemplates = ref<TemplateWithHistoryDto[]>([]);
const selectedTemplateId = ref<number | ''>('');
const userIdsInput = ref('');
const expiredTimeInput = ref(''); // ISO 8601 format string for datetime-local input

const loading = ref(false);
const loadingTemplates = ref(false);
const templateError = ref(false);

// 사용 가능한 이메일 템플릿 불러오기
async function fetchTemplates() {
  loadingTemplates.value = true;
  templateError.value = false;
  try {
    // history 없이 최신 템플릿만 조회
    const res = await api.getEmailTemplates(false);
    availableTemplates.value = res.templates ?? [];
  } catch (e) {
    console.error('Error fetching templates:', e);
    templateError.value = true;
    availableTemplates.value = [];
  } finally {
    loadingTemplates.value = false;
  }
}

// 모달이 열릴 때 템플릿 목록 불러오기 및 폼 초기화
watch(() => props.show, (newVal) => {
  if (newVal) {
    fetchTemplates();
    resetForm();
  }
});

// 폼 초기화
function resetForm() {
  selectedTemplateId.value = '';
  userIdsInput.value = '';
  expiredTimeInput.value = '';
}

// 모달 닫기
function closeModal() {
  resetForm();
  emit('close');
}

// 스케줄 저장
async function saveSchedule() {
  if (!selectedTemplateId.value || !userIdsInput.value || !expiredTimeInput.value) {
    alert('Please select a template, enter recipient User IDs, and set an expiration time.');
    return;
  }

  const userIds = userIdsInput.value.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
  if (userIds.length === 0) {
      alert('Please enter valid comma-separated User IDs.');
      return;
  }

  // datetime-local input value is typically in 'YYYY-MM-DDTHH:mm' format.
  // The API expects date-time format (ISO 8601), which is compatible.
  // No explicit formatting is needed for the API call.

  loading.value = true;
  try {
    const dataToSave: PostNotificationEmailRequest = {
      templateId: selectedTemplateId.value as number,
      userIds: userIds,
      expiredTime: expiredTimeInput.value, // Pass directly
      // templateVersion은 필요시 추가 로직 구현
    };
    const res = await api.createEmailNotificationSchedule(dataToSave);
    
    if (res.newSchedule) { // newSchedule taskName이 리턴되면 성공으로 간주
      alert('이메일 스케줄이 성공적으로 등록되었습니다.\nTask Name: ' + res.newSchedule);
      emit('saved'); // 저장 후 목록 갱신을 위해 이벤트 발생
      closeModal();
    } else {
       alert('이메일 스케줄 등록에 실패했습니다.');
    }
  } catch (e) {
    console.error('Error saving schedule:', e);
    alert('이메일 스케줄 등록 중 오류가 발생했습니다.\n' + e.message); // 에러 메시지 포함
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
.form-group select,
.form-group input[type="datetime-local"] {
  width: 100%;
  padding: 0.8em;
  border: 1px solid #ccc;
  border-radius: 4px;
  box-sizing: border-box; /* 패딩 포함 */
  font-size: 1rem; /* input 기본 폰트 크기 */
}

.help-text {
    font-size: 0.8em;
    color: #6b7280;
    margin-top: 0.5em;
}

.error-message {
    font-size: 0.8em;
    color: #dc2626;
    margin-top: 0.5em;
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