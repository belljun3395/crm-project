import React from 'react';
import type { Preview } from '@storybook/react';
import '../src/index.css';

// 다크모드 토글을 위한 데코레이터
const withThemeProvider = (Story: React.ComponentType, context: any) => {
  const theme = context.globals.theme || 'dark';

  return (
    <div className={theme === 'dark' ? 'dark' : ''}>
      <div className={theme === 'dark' ? 'bg-gray-900 text-white min-h-screen p-4' : 'bg-white text-black min-h-screen p-4'}>
        <Story />
      </div>
    </div>
  );
};

const preview: Preview = {
  parameters: {
    actions: { argTypesRegex: '^on[A-Z].*' },
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
    backgrounds: {
      disable: true, // 커스텀 테마 사용으로 비활성화
    },
    a11y: {
      element: '#storybook-root',
      config: {},
      options: {},
    },
  },
  globalTypes: {
    theme: {
      description: 'Global theme for components',
      defaultValue: 'dark',
      toolbar: {
        title: 'Theme',
        icon: 'circlehollow',
        items: [
          { value: 'light', title: 'Light', icon: 'sun' },
          { value: 'dark', title: 'Dark', icon: 'moon' },
        ],
        dynamicTitle: true,
      },
    },
  },
  decorators: [withThemeProvider],
};

export default preview;