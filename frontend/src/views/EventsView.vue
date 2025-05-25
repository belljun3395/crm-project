<template>
  <div>
    <h2>Events</h2>

    <!-- 이벤트 검색 폼 -->
    <div class="search-form">
      <div class="form-group">
        <label for="eventName">Event Name:</label>
        <input id="eventName" v-model="searchParams.eventName" placeholder="e.g., button_click" />
      </div>
      <div class="form-group">
        <label for="where">Where Condition:</label>
        <input id="where" v-model="searchParams.where" placeholder="e.g., key1&value1&operation&joinOperation,key2&value2&operation&joinOperation" />
        <p class="help-text">Format: key&value(&key&value)&operation&joinOperation,... (operation: =, !=, >, >=, <, <=, like, between; joinOperation: and, or, end)</p>
      </div>
      <button class="btn blue" @click="performSearch">Search</button>
    </div>

    <hr />

    <!-- 이벤트 등록 버튼 -->
    <div class="action-row">
      <div class="button-group">
        <button class="btn blue" @click="openCreateModal">+ Add Event</button>
        <button class="btn blue" @click="openCreateCampaignModal">+ Add Campaign</button>
      </div>
    </div>

    <!-- 로딩 상태 표시 -->
    <div v-if="loading">Searching events...</div>
    <!-- 에러 상태 표시 -->
    <div v-else-if="error">Error searching events.</div>
    
    <!-- 검색 결과 표시 -->
    <div v-else-if="events.length > 0">
      <h3>Search Results ({{ events.length }} events)</h3>
      <!-- 이벤트 목록 테이블 컴포넌트 -->
      <EventTable :events="events" />
    </div>
    
    <div v-else>
      <p>No events found. Please adjust your search criteria.</p>
    </div>

    <!-- 이벤트 등록 모달 -->
    <EventCreateModal 
      :show="showCreateModal"
      @close="closeCreateModal"
      @saved="handleEventSaved" 
    />

    <!-- 캠페인 등록 모달 -->
    <CampaignCreateModal
        :show="showCreateCampaignModal"
        @close="closeCreateCampaignModal"
        @saved="handleCampaignSaved" 
    />

  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import api from '../api';
import type { EventDto } from '../types/Event';
import EventTable from '../components/EventTable.vue'; // EventTable 컴포넌트 import
import EventCreateModal from '../components/EventCreateModal.vue'; // EventCreateModal 컴포넌트 import
import CampaignCreateModal from '../components/CampaignCreateModal.vue'; // CampaignCreateModal 컴포넌트 import

const searchParams = ref({
  eventName: '',
  where: '',
});

const events = ref<EventDto[]>([]);
const loading = ref(false);
const error = ref(false);

// 이벤트 등록 모달 표시 여부
const showCreateModal = ref(false);
// 캠페인 등록 모달 표시 여부
const showCreateCampaignModal = ref(false);

async function performSearch() {
  // eventName과 where 조건이 모두 있어야 검색 실행
  if (!searchParams.value.eventName || !searchParams.value.where) {
    alert('Event Name과 Where Condition을 모두 입력해주세요.');
    return;
  }

  loading.value = true;
  error.value = false;
  events.value = [];

  try {
    // API 호출
    const res = await api.searchEvents(searchParams.value.eventName, searchParams.value.where);
    events.value = res.events ?? [];
  } catch (e) {
    console.error('Error searching events:', e);
    error.value = true;
    events.value = [];
  } finally {
    loading.value = false;
  }
}

// 등록 모달 열기
function openCreateModal() {
  showCreateModal.value = true;
}

// 등록 모달 닫기
function closeCreateModal() {
  showCreateModal.value = false;
}

// 캠페인 등록 모달 열기
function openCreateCampaignModal() {
    showCreateCampaignModal.value = true;
}

// 캠페인 등록 모달 닫기
function closeCreateCampaignModal() {
    showCreateCampaignModal.value = false;
}

// 이벤트 저장 완료 처리
function handleEventSaved() {
  // 이벤트 등록 후 자동 목록 갱신 대신 사용자에게 재검색 안내 또는 다른 처리
  // 예를 들어, alert 메시지 후 모달만 닫기
  console.log('Event saved, please perform search again to see the updated list.');
  // 또는 자동 갱신을 원하면 여기서 performSearch() 호출
  // performSearch();
}

// 캠페인 저장 완료 처리
function handleCampaignSaved() {
    console.log('Campaign saved.');
    // 캠페인 목록 조회가 있다면 여기서 갱신
}

</script>

<style scoped>
.search-form {
  display: flex;
  gap: 1rem;
  align-items: flex-end;
  margin-bottom: 2rem;
  flex-wrap: wrap; /* 반응형 */
}

.form-group {
  flex: 1;
  min-width: 200px; /* 최소 너비 */
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: bold;
}

.form-group input[type="text"] {
  width: 100%;
  padding: 0.8em;
  border: 1px solid #ccc;
  border-radius: 4px;
  box-sizing: border-box;
}

.search-form button {
    padding: 0.8em 1.5em; /* 입력창 높이에 맞춤 */
}

.help-text {
    font-size: 0.8em;
    color: #6b7280;
    margin-top: 0.5em;
}

hr {
  margin: 2rem 0;
  border: 0;
  border-top: 1px solid #eee;
}

pre {
    background: #f4f4f4;
    padding: 1em;
    border-radius: 4px;
    overflow-x: auto;
}

.action-row {
    margin-bottom: 1.5rem;
    text-align: right; 
    /* 버튼 오른쪽 정렬 */
}

.button-group {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
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