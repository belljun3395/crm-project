<script setup lang="ts">
import { ref, watch } from 'vue';
import api from '../api';

const props = defineProps<{ show: boolean; templateId: number | null }>();
const emit = defineEmits(['close', 'sent']);

const testEmailAddress = ref('');
const loading = ref(false);

// props.show 변경 감지하여 testEmailAddress 초기화
watch(() => props.show, (newVal) => {
  if (newVal) {
    testEmailAddress.value = ''; // 모달 열릴 때마다 초기화
  }
});

// 테스트 이메일 발송
async function sendTestEmail() {
  if (!props.templateId) {
    alert('이메일 템플릿 ID가 없습니다.');
    return;
  }
  if (!testEmailAddress.value) {
    alert('테스트 이메일 주소를 입력해주세요.');
    return;
  }

  loading.value = true;
  try {
    // await api.post(`/email-templates/${props.templateId}/send-test`, {
    //   to: testEmailAddress.value
    // });
    await api.sendTestEmail(props.templateId, testEmailAddress.value);
    alert('테스트 이메일이 성공적으로 발송되었습니다.');
    emit('sent');
    closeModal();
  } catch (e) {
    console.error('Error sending test email:', e);
    alert('테스트 이메일 발송에 실패했습니다.');
  } finally {
    loading.value = false;
  }
}

// 모달 닫기
function closeModal() {
  emit('close');
}
</script>

<template>
  <div v-if="show" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <h3>Send Test Email</h3>

      <form @submit.prevent="sendTestEmail">
        <div class="form-group">
          <label for="testEmail">Recipient Email:</label>
          <input id="testEmail" type="email" v-model="testEmailAddress" required />
        </div>

        <div class="modal-actions">
          <button type="submit" class="btn blue" :disabled="loading">{{ loading ? 'Sending...' : 'Send' }}</button>
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
  max-width: 400px;
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

.form-group input[type="email"] {
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