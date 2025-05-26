<template>
  <div>
    <div class="header-row">
      <h2>Email Templates</h2>
      <div class="button-group">
        <button class="btn blue" @click="openCreateModal">+ Add Template</button>
        <button class="btn blue" @click="openSendEmailModal">Send Email</button>
      </div>
    </div>
    
    <!-- 로딩 상태 표시 -->
    <div v-if="loading">Loading email templates...</div>
    <!-- 에러 상태 표시 -->
    <div v-else-if="error">Error loading email templates.</div>
    <!-- 템플릿 목록 테이블 -->
    <div v-else>
      <EmailTemplateTable
        :emailTemplates="templateList"
        :loading="loading"
        @edit="openEditModal"
        @delete="deleteTemplate"
      />
    </div>

    <!-- 템플릿 등록/수정 모달 -->
    <EmailTemplateModal 
      :show="showModal"
      :template="editingTemplate"
      @close="closeModal"
      @saved="fetchEmailTemplates"
    />

    <!-- 이메일 발송 모달 -->
    <SendNotificationEmailModal
      :show="showSendEmailModal"
      @close="closeSendEmailModal"
      @sent="handleEmailSent" 
    />

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
// 이메일 도메인 API를 import
import * as emailApi from '../api';
import type { TemplateWithHistoryDto } from '../types/EmailModel';
import EmailTemplateTable from '../components/EmailTemplateTable.vue'; // 템플릿 테이블 컴포넌트 import
import EmailTemplateModal from '../components/EmailTemplateModal.vue'; // 모달 컴포넌트 import
import SendNotificationEmailModal from '../components/SendNotificationEmailModal.vue'; // 발송 모달 컴포넌트 import
import type { TemplateDto } from '../types/EmailModel';

const emailTemplates = ref<TemplateWithHistoryDto[]>([]);
const totalCount = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const error = ref(false);
// 수정할 템플릿 정보
const editingTemplate = ref<TemplateDto | undefined>(undefined);
// 등록/수정 모달 표시 여부
const showModal = ref(false);
// 이메일 발송 모달 표시 여부
const showSendEmailModal = ref(false);

const templateList = computed(() => emailTemplates.value.map(t => t.template));

// 이메일 템플릿 목록 및 총 개수 가져오기
async function fetchEmailTemplates() {
  console.log('Fetching email templates...'); // 로딩 시작 로그
  loading.value = true;
  error.value = false;
  try {
    console.log(`Fetching email templates: page=${currentPage.value}, pageSize=${pageSize.value}`);
    // 이메일 템플릿 목록을 불러옵니다.
    const response = await emailApi.getEmailTemplates(); // TODO: Pagination 지원 시 파라미터 추가
    emailTemplates.value = response.templates || [];
    // totalCount는 OpenAPI에 없음. 페이지네이션 구현 시 API 확장 필요
    // totalCount.value = response.totalCount || 0; // TODO: 페이지네이션 구현 시 사용
    console.log('Email templates fetched:', emailTemplates.value);
    console.log('Total count:', totalCount.value);
    error.value = false;
  } catch (e) {
    console.error('Error fetching email templates:', e);
    error.value = true;
    emailTemplates.value = [];
    totalCount.value = 0;
  } finally {
    loading.value = false;
    console.log('Fetching templates finished. Loading:', loading.value, 'Error:', error.value, 'Count:', emailTemplates.value.length); // 로딩 종료 로그
  }
}

// 페이지 변경 감지
watch(currentPage, fetchEmailTemplates);

// 컴포넌트 마운트 시 이메일 템플릿 목록 가져오기
onMounted(fetchEmailTemplates);

// 등록 모달 열기
function openCreateModal() {
  editingTemplate.value = undefined; // 빈 폼
  showModal.value = true;
}

// 수정 모달 열기
function openEditModal(template: TemplateDto) {
  editingTemplate.value = template; // 선택된 템플릿 데이터
  showModal.value = true;
}

// 템플릿 삭제
async function deleteTemplate(templateId: number) {
  if (confirm('정말로 이 템플릿을 삭제하시겠습니까?')) {
    try {
      // force 옵션은 필요에 따라 true/false 결정
      // 이메일 템플릿 삭제 API 호출
      const res = await emailApi.deleteEmailTemplate(templateId, false);
      if (res.success) {
        alert('템플릿이 삭제되었습니다.');
        fetchEmailTemplates(); // 목록 갱신
      } else {
        alert('템플릿 삭제에 실패했습니다.');
      }
    } catch (e) {
      console.error('Error deleting template:', e);
      alert('템플릿 삭제 중 오류가 발생했습니다.');
    }
  }
}

// 모달 닫기 함수
function closeModal() {
  showModal.value = false;
}

// 이메일 발송 모달 열기
function openSendEmailModal() {
    showSendEmailModal.value = true;
}

// 이메일 발송 모달 닫기
function closeSendEmailModal() {
    showSendEmailModal.value = false;
}

// 이메일 발송 완료 처리
function handleEmailSent() {
    console.log('Email sent.');
    // 발송 완료 후 추가적인 처리 (예: 성공 메시지 표시 등)
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