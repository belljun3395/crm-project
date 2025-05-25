import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/users',
      name: 'users',
      component: () => import('../views/UsersView.vue'),
    },
    {
      path: '/emails',
      name: 'emails',
      component: () => import('../views/EmailsView.vue'),
    },
    {
      path: '/email-schedules',
      name: 'email-schedules',
      component: () => import('../views/EmailSchedulesView.vue'),
    },
    {
      path: '/events',
      name: 'events',
      component: () => import('../views/EventsView.vue'),
    },
  ],
});

export default router; 