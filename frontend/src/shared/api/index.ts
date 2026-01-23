// API 통합 export
export { userAPI } from './crm/user';
export { eventAPI } from './crm/event';
export { templateAPI } from './crm/email/template';
export { emailScheduleAPI } from './crm/email/schedule';
export { webhookAPI } from './crm/webhook';
export { campaignAPI } from './crm/campaign';
export { emailHistoryAPI } from './crm/email/history';

// Instance export (필요시 사용)
export { crmApi } from './crm/instance';