import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { Input } from './Input';

describe('Input', () => {
  it('렌더링이 올바르게 된다', () => {
    render(<Input placeholder="Enter text" />);
    expect(screen.getByPlaceholderText('Enter text')).toBeInTheDocument();
  });

  it('기본 props로 렌더링된다', () => {
    render(<Input />);
    const input = screen.getByRole('textbox');
    
    expect(input).toHaveAttribute('type', 'text');
    expect(input).not.toBeDisabled();
    expect(input).not.toBeRequired();
  });

  it('label이 올바르게 렌더링된다', () => {
    render(<Input label="Username" />);
    expect(screen.getByLabelText('Username')).toBeInTheDocument();
    expect(screen.getByText('Username')).toBeInTheDocument();
  });

  it('error 상태가 올바르게 표시된다', () => {
    render(<Input label="Email" error="Invalid email format" />);
    
    const input = screen.getByRole('textbox');
    expect(input).toHaveClass('border-red-500');
    expect(screen.getByText('Invalid email format')).toBeInTheDocument();
    expect(screen.getByText('Invalid email format')).toHaveClass('text-red-500');
  });

  it('다양한 type이 올바르게 설정된다', () => {
    const { rerender } = render(<Input type="email" />);
    expect(screen.getByRole('textbox')).toHaveAttribute('type', 'email');

    rerender(<Input type="password" />);
    expect(screen.getByLabelText(/password/i)).toHaveAttribute('type', 'password');

    rerender(<Input type="number" />);
    expect(screen.getByRole('spinbutton')).toHaveAttribute('type', 'number');
  });

  it('disabled 상태가 올바르게 동작한다', () => {
    const handleChange = jest.fn();
    render(<Input disabled onChange={handleChange} />);
    
    const input = screen.getByRole('textbox');
    expect(input).toBeDisabled();
    expect(input).toHaveClass('opacity-50', 'cursor-not-allowed');
  });

  it('required 속성이 올바르게 설정된다', () => {
    render(<Input required />);
    expect(screen.getByRole('textbox')).toBeRequired();
  });

  it('onChange 핸들러가 올바르게 호출된다', async () => {
    const handleChange = jest.fn();
    const user = userEvent.setup();
    render(<Input onChange={handleChange} />);
    
    const input = screen.getByRole('textbox');
    await user.type(input, 'test');
    
    expect(handleChange).toHaveBeenCalledTimes(4); // 't', 'e', 's', 't'
    expect(input).toHaveValue('test');
  });

  it('value prop이 올바르게 동작한다', () => {
    render(<Input value="controlled value" readOnly />);
    expect(screen.getByDisplayValue('controlled value')).toBeInTheDocument();
  });

  it('placeholder가 올바르게 표시된다', () => {
    render(<Input placeholder="Enter your name" />);
    expect(screen.getByPlaceholderText('Enter your name')).toBeInTheDocument();
  });

  it('커스텀 className이 올바르게 적용된다', () => {
    render(<Input className="custom-input" />);
    expect(screen.getByRole('textbox')).toHaveClass('custom-input');
  });

  it('label과 input이 올바르게 연결된다', () => {
    render(<Input label="Email Address" />);
    const label = screen.getByText('Email Address');
    const input = screen.getByRole('textbox');
    
    expect(label).toHaveAttribute('for');
    expect(input).toHaveAttribute('id');
    expect(label.getAttribute('for')).toBe(input.getAttribute('id'));
  });

  it('error가 있을 때 aria-invalid가 설정된다', () => {
    render(<Input error="This field is required" />);
    expect(screen.getByRole('textbox')).toHaveAttribute('aria-invalid', 'true');
  });

  it('label과 error가 함께 렌더링된다', () => {
    render(<Input label="Username" error="Username is taken" />);
    
    expect(screen.getByText('Username')).toBeInTheDocument();
    expect(screen.getByText('Username is taken')).toBeInTheDocument();
    expect(screen.getByRole('textbox')).toHaveClass('border-red-500');
  });
});