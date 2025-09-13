import React, { useId } from 'react';

type BaseTextareaProps = {
  placeholder?: string;
  disabled?: boolean;
  error?: string;
  label?: string;
  required?: boolean;
  rows?: number;
  className?: string;
  id?: string;
};

type ControlledTextareaProps = BaseTextareaProps & {
  value: string;
  onChange: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
  defaultValue?: never;
};

type UncontrolledTextareaProps = BaseTextareaProps & {
  value?: never;
  onChange?: never;
  defaultValue?: string;
};

type TextareaProps = ControlledTextareaProps | UncontrolledTextareaProps;

export const Textarea: React.FC<TextareaProps> = ({
  value,
  defaultValue,
  onChange,
  placeholder,
  disabled = false,
  error,
  label,
  required = false,
  rows = 3,
  className = '',
  id,
}) => {
  const generatedId = useId();
  const textareaId = id ?? generatedId;
  const errorId = `${textareaId}-error`;
  const textareaClasses = `
    w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 transition-colors resize-y
    ${error 
      ? 'border-red-500 focus:ring-red-500 focus:border-red-500' 
      : 'border-gray-700 bg-gray-800 text-white focus:ring-[#22c55e] focus:border-[#22c55e]'
    }
    ${disabled ? 'opacity-50 cursor-not-allowed' : ''}
    ${className}
  `.trim();

  return (
    <div className="w-full">
      {label && (
        <label htmlFor={textareaId} className="block text-sm font-medium text-gray-300 mb-2">
          {label}
          {required && <span className="text-red-400 ml-1">*</span>}
        </label>
      )}
      <textarea
        id={textareaId}
        value={value}
        defaultValue={defaultValue}
        onChange={onChange}
        placeholder={placeholder}
        disabled={disabled}
        required={required}
        rows={rows}
        className={textareaClasses}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? errorId : undefined}
      />
      {error && (
        <p id={errorId} className="mt-1 text-sm text-red-400">{error}</p>
      )}
    </div>
  );
};