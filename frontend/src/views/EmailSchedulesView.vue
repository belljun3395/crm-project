<template>
  <div>
    <div class="header-row">
      <h2>Email Schedules</h2>
      <div class="button-group">
        <button class="btn blue" @click="openCreateScheduleModal">+ Schedule Email</button>
      </div>
    </div>
    
    <!-- 로딩 상태 표시 -->
    <div v-if="loading">Loading email schedules...</div>
    <!-- 에러 상태 표시 -->
    <div v-else-if="error" class="error-message">Error loading email schedules.</div>
    <!-- 스케줄 목록 테이블 -->
    <div v-else-if="schedules.length > 0">
        <EmailScheduleTable :schedules="schedules" @cancel="cancelSchedule" />
    </div>
     <div v-else>
      <p>No email schedules found.</p>
    </div>

    <!-- 스케줄 등록 모달 (나중에 연결) -->
    <EmailScheduleCreateModal :show="showCreateScheduleModal" @close="closeCreateScheduleModal" @saved="handleScheduleSaved" />

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import api from '../api';
import type { EmailNotificationScheduleDto } from '../types/Email';
import EmailScheduleTable from '../components/EmailScheduleTable.vue'; // 스케줄 테이블 컴포넌트 import
import EmailScheduleCreateModal from '../components/EmailScheduleCreateModal.vue'; // 스케줄 등록 모달 import

const schedules = ref<EmailNotificationScheduleDto[]>([]);
const loading = ref(true);
const error = ref(false);

// 스케줄 등록 모달 표시 여부
const showCreateScheduleModal = ref(false);

// 이메일 스케줄 목록 조회
async function fetchSchedules() {
  loading.value = true;
  error.value = false;
  try {
    const res = await api.getEmailNotificationSchedules();
    schedules.value = res.schedules ?? [];
  } catch (e) {
    console.error('Error fetching email schedules:', e);
    error.value = true;
    schedules.value = [];
  } finally {
    loading.value = false;
  }
}

// 스케줄 취소
async function cancelSchedule(scheduleId: string) {
  if (confirm('정말로 이 스케줄을 취소하시겠습니까?\nTask Name: ' + scheduleId)) {
    try {
      const res = await api.cancelEmailNotificationSchedule(scheduleId);
      if (res.success) {
        alert('스케줄이 취소되었습니다.');
        fetchSchedules(); // 목록 갱신
      } else {
        alert('스케줄 취소에 실패했습니다.');
      }
    } catch (e) {
      console.error('Error cancelling schedule:', e);
      alert('스케줄 취소 중 오류가 발생했습니다.');
    }
  }
}

// 컴포넌트 마운트 시 스케줄 목록 조회
onMounted(fetchSchedules);

// 스케줄 등록 모달 열기
function openCreateScheduleModal() {
    showCreateScheduleModal.value = true; // 모달 연결 시 사용
}

// 스케줄 등록 모달 닫기
function closeCreateScheduleModal() {
    showCreateScheduleModal.value = false;
}

// 스케줄 등록 완료 후 처리
function handleScheduleSaved() {
    console.log('Schedule saved.');
    fetchSchedules(); // 목록 갱신
}

</script>

<style scoped>
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}
.button-group {
  display: flex;
  gap: 1rem;
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
.btn.blue {
  background: #2563eb;
}
.btn.blue:hover {
  background: #1746a2;
}
</style> 