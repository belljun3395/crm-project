import type { Meta, StoryObj } from '@storybook/react';
import { Input } from './Input';

const meta = {
  title: 'Common/Input',
  component: Input,
  parameters: {
    layout: 'centered',
    docs: {
      description: {
        component: '재사용 가능한 입력 컴포넌트입니다. 다양한 타입과 검증 상태를 지원합니다.',
      },
    },
  },
  tags: ['autodocs'],
  argTypes: {
    type: {
      control: { type: 'select' },
      options: ['text', 'email', 'password', 'number', 'tel', 'url'],
      description: 'HTML input의 type 속성입니다.',
    },
    disabled: {
      control: { type: 'boolean' },
      description: '입력 필드의 비활성화 상태를 결정합니다.',
    },
    required: {
      control: { type: 'boolean' },
      description: '필수 입력 여부를 결정합니다.',
    },
    error: {
      control: { type: 'text' },
      description: '에러 메시지를 표시합니다.',
    },
  },
  args: { onChange: () => {} },
} satisfies Meta<typeof Input>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    placeholder: 'Enter text...',
  },
};

export const WithLabel: Story = {
  args: {
    label: 'Username',
    placeholder: 'Enter your username',
  },
};

export const WithError: Story = {
  args: {
    label: 'Email',
    placeholder: 'Enter your email',
    error: 'Please enter a valid email address',
    value: 'invalid-email',
  },
};

export const Disabled: Story = {
  args: {
    label: 'Disabled Field',
    placeholder: 'This field is disabled',
    disabled: true,
    value: 'Cannot edit this',
  },
};

export const Required: Story = {
  args: {
    label: 'Required Field',
    placeholder: 'This field is required',
    required: true,
  },
};

export const Password: Story = {
  args: {
    label: 'Password',
    type: 'password',
    placeholder: 'Enter your password',
  },
};

export const Email: Story = {
  args: {
    label: 'Email Address',
    type: 'email',
    placeholder: 'user@example.com',
  },
};

export const Number: Story = {
  args: {
    label: 'Age',
    type: 'number',
    placeholder: '25',
  },
};

export const FormExample: Story = {
  args: {
    placeholder: 'Example input',
  },
  render: () => (
    <div className="space-y-4 w-80">
      <Input 
        label="Full Name" 
        placeholder="Enter your full name"
        required
      />
      <Input 
        label="Email Address" 
        type="email"
        placeholder="user@example.com"
        required
      />
      <Input 
        label="Password" 
        type="password"
        placeholder="Enter password"
        required
      />
      <Input 
        label="Phone Number" 
        type="tel"
        placeholder="(555) 123-4567"
      />
      <Input 
        label="Website" 
        type="url"
        placeholder="https://example.com"
      />
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: '실제 폼에서 사용되는 다양한 입력 필드들의 예시입니다.',
      },
    },
  },
};

export const ValidationStates: Story = {
  args: {
    placeholder: 'Validation example',
  },
  render: () => (
    <div className="space-y-4 w-80">
      <Input 
        label="Valid Field" 
        placeholder="This is valid"
        value="valid@example.com"
      />
      <Input 
        label="Invalid Field" 
        placeholder="This has an error"
        value="invalid-email"
        error="Please enter a valid email address"
      />
      <Input 
        label="Required Field" 
        placeholder="This is required"
        required
        error="This field is required"
      />
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: '입력 필드의 다양한 검증 상태를 확인할 수 있습니다.',
      },
    },
  },
};