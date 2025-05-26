<template>
  <div v-if="show" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <h3>{{ modalTitle }}</h3>

      <form @submit.prevent="saveCampaign">
        <div class="form-group">
          <label for="name">Name:</label>
          <input
            id="name"
            type="text"
            v-model="editableCampaign.name"
            required
          />
        </div>

        <div class="form-group">
          <label for="description">Description:</label>
          <textarea
            id="description"
            v-model="editableCampaign.description"
            rows="5"
          ></textarea>
        </div>

        <div class="form-group">
          <label for="status">Status:</label>
          <select id="status" v-model="editableCampaign.status">
            <option value="Draft">Draft</option>
            <option value="Active">Active</option>
            <option value="Archived">Archived</option>
          </select>
        </div>

        <div class="modal-actions">
          <button type="submit" class="btn blue" :disabled="loading">
            {{ loading ? "Saving..." : "Save Campaign" }}
          </button>
          <button
            type="button"
            class="btn"
            @click="closeModal"
            :disabled="loading"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from "vue";
// 이벤트 도메인 API를 import
import * as eventApi from "../api";
import type { Campaign } from '../types/CampaignModel';
import type { CreateCampaignRequest, UpdateCampaignRequest } from '../types/CampaignRequest';

const props = defineProps<{
  show: boolean; // 모달 표시 여부
  campaign?: Campaign; // 수정 시 전달될 캠페인 데이터
}>();

const emit = defineEmits(["close", "saved"]);

const editableCampaign = ref<CreateCampaignRequest | UpdateCampaignRequest>({
  name: "",
  description: "",
  status: "Draft", // Default status
  // no 'id' here for creation, add it for update
});

const loading = ref(false);

// props.campaign 변경 감지하여 editableCampaign 업데이트
watch(
  () => props.campaign,
  (newVal) => {
    if (newVal) {
      // 수정 모드: 기존 데이터 복사
      editableCampaign.value = { ...newVal };
    } else {
      // 등록 모드: 폼 초기화
      resetForm();
    }
  },
  { immediate: true }
); // 컴포넌트 마운트 시에도 watch 실행

// 폼 초기화
function resetForm() {
  editableCampaign.value = {
    name: "",
    description: "",
    status: "Draft",
  };
}

// 모달 닫기
function closeModal() {
  resetForm();
  emit("close");
}

// 캠페인 저장 (등록 또는 수정)
async function saveCampaign() {
  loading.value = true;
  try {
    const dataToSave = { ...editableCampaign.value };

    // 필수 필드 검증
    if (!dataToSave.name) {
      alert("Campaign Name은 필수 항목입니다.");
      return;
    }

    let res;
    if ("id" in dataToSave && dataToSave.id) {
      // res = await api.updateCampaign(
      //   dataToSave.id,
      //   dataToSave as UpdateCampaignRequest
      // );
      // TODO: OpenAPI에 updateCampaign 엔드포인트 없음. 필요시 구현
    } else {
      // 캠페인 생성 API 호출
      res = await eventApi.createCampaign(dataToSave as CreateCampaignRequest);
    }

    // 응답 결과에 따라 성공/실패 처리 (openapi.json 스키마 기반)
    if (res && res.id) {
      // id가 리턴되면 성공으로 간주
      alert("캠페인이 성공적으로 저장되었습니다.");
      emit("saved"); // 저장 후 목록 갱신을 위해 이벤트 발생
      closeModal();
    } else {
      alert("캠페인 저장에 실패했습니다.");
    }
  } catch (e) {
    console.error("Error saving campaign:", e);
    alert("캠페인 저장 중 오류가 발생했습니다.");
  } finally {
    loading.value = false;
  }
}

const modalTitle = computed(() =>
  props.campaign ? "Edit Campaign" : "Create Campaign"
);
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
.form-group select,
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
