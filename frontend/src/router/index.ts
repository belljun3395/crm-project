import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/users',
      name: 'users',
      // 도메인 구조에 맞게 경로 수정
      component: () => import('../domains/user/views/UsersView.vue'),
    },
    {
      path: '/emails',
      name: 'emails',
      // 도메인 구조에 맞게 경로 수정
      component: () => import('../domains/email/views/EmailsView.vue'),
    },
    {
      path: '/email-schedules',
      name: 'email-schedules',
      // 도메인 구조에 맞게 경로 수정
      component: () => import('../domains/email/views/EmailSchedulesView.vue'),
    },
    {
      path: '/events',
      name: 'events',
      // 도메인 구조에 맞게 경로 수정
      component: () => import('../domains/event/views/EventsView.vue'),
    },
  ],
});

export default router; 