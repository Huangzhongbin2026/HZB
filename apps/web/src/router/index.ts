import { createRouter, createWebHistory } from 'vue-router'

import WorkspaceStudioView from '../views/WorkspaceStudioView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: WorkspaceStudioView,
    },
    {
      path: '/notes/:slug?',
      name: 'notes',
      component: WorkspaceStudioView,
      props: true,
    },
    {
      path: '/passwords',
      name: 'passwords',
      component: WorkspaceStudioView,
    },
    {
      path: '/topics',
      name: 'topics',
      component: WorkspaceStudioView,
    },
    {
      path: '/ideas',
      name: 'ideas',
      component: WorkspaceStudioView,
    },
    {
      path: '/settings',
      name: 'settings',
      component: WorkspaceStudioView,
    },
    {
      path: '/ai',
      name: 'ai',
      component: WorkspaceStudioView,
    },
    {
      path: '/graph',
      name: 'graph',
      component: WorkspaceStudioView,
    },
    {
      path: '/whiteboard',
      name: 'whiteboard',
      component: WorkspaceStudioView,
    },
    {
      path: '/daily',
      name: 'daily',
      component: WorkspaceStudioView,
    },
    {
      path: '/templates',
      name: 'templates',
      component: WorkspaceStudioView,
    },
    {
      path: '/models',
      redirect: '/settings',
    },
  ],
})
