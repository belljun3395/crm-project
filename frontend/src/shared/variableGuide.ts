export interface VariableGuideItem {
  key: string;
  description: string;
  required?: boolean;
}

export const COLLECTED_USER_FIELDS: VariableGuideItem[] = [
  {
    key: 'externalId',
    description: '고객 고유 식별자',
    required: true
  },
  {
    key: 'userAttributes.email',
    description: '발송/개인화에 사용하는 기본 이메일',
    required: true
  },
  {
    key: 'userAttributes.name',
    description: '개인화 문구(예: 이름) 권장',
    required: false
  }
];

export const JOURNEY_AUTO_VARIABLES: VariableGuideItem[] = [
  { key: 'eventId', description: '트리거 이벤트 ID' },
  { key: 'userId', description: '트리거 사용자 ID' },
  { key: 'eventName', description: '트리거 이벤트 이름' },
  { key: 'event.<propertyKey>', description: '이벤트 properties 값' },
  { key: 'user.id', description: '내부 사용자 ID' },
  { key: 'user.externalId', description: '사용자 externalId' },
  { key: 'user.email', description: '사용자 이메일' },
  { key: 'user.name', description: '사용자 이름(있을 때)' }
];

export const EMAIL_TEMPLATE_VARIABLE_GUIDE: string[] = [
  'user.<attributeKey>: 사용자 속성 키를 참조합니다. (예: user.email, user.name)',
  'campaign.<propertyKey>: 캠페인 이벤트 속성 키를 참조합니다. (예: campaign.eventCount)',
  '기본값 문법: user.name:고객님 처럼 :default 를 붙일 수 있습니다.'
];

