<template>
  <div v-if="show" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <h3>Send Notification Email</h3>
      <form @submit.prevent="sendEmail">
        <div class="form-group">
          <label for="templateId">Template ID:</label>
          <input id="templateId" v-model.number="templateId" required type="number" />
        </div>
        <div class="form-group">
          <label for="templateVersion">Template Version (optional):</label>
          <input id="templateVersion" v-model.number="templateVersion" type="number" />
        </div>
        <div class="form-group">
          <label for="userIds">User IDs (comma separated):</label>
          <input id="userIds" v-model="userIdsInput" required placeholder="e.g. 1,2,3" />
        </div>
        <div class="modal-actions">
          <button type="submit" class="btn blue" :disabled="loading">{{ loading ? 'Sending...' : 'Send' }}</button>
          <button type="button" class="btn" @click="closeModal" :disabled="loading">Cancel</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import api from '../api';

const props = defineProps<{ show: boolean }>();
const emit = defineEmits(['close', 'sent']);

const templateId = ref<number | null>(null);
const templateVersion = ref<number | null>(null);
const userIdsInput = ref('');
const loading = ref(false);

function closeModal() {
  emit('close');
  templateId.value = null;
  templateVersion.value = null;
  userIdsInput.value = '';
}

async function sendEmail() {
  loading.value = true;
  try {
    const userIds = userIdsInput.value.split(',').map(id => parseInt(id.trim(), 10)).filter(id => !isNaN(id));
    if (!templateId.value || userIds.length === 0) {
      alert('Template ID와 User IDs를 모두 입력해주세요.');
      return;
    }
    await api.sendNotificationEmail({
      templateId: templateId.value,
      templateVersion: templateVersion.value || undefined,
      userIds,
    });
    alert('Notification email sent successfully.');
    emit('sent');
    closeModal();
  } catch (e) {
    alert('Failed to send notification email.');
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
.form-group input[type="number"] {
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
