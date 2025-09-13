import React, { useId } from 'react';

type BaseInputProps = {
  type?: 'text' | 'email' | 'password' | 'number' | 'tel' | 'url' | 'search' | 'date' | 'datetime-local';
  placeholder?: string;
  disabled?: boolean;
  readOnly?: boolean;
  error?: string;
  label?: string;
  required?: boolean;
  className?: string;
  id?: string;
};

type ControlledInputProps = BaseInputProps & {
  value: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  defaultValue?: never;
};

type UncontrolledInputProps = BaseInputProps & {
  value?: never;
  onChange?: never;
  defaultValue?: string;
};

type InputProps = ControlledInputProps | UncontrolledInputProps;

export const Input: React.FC<InputProps> = ({
  type = 'text',
  value,
  defaultValue,
  onChange,
  placeholder,
  disabled = false,
  readOnly = false,
  error,
  label,
  required = false,
  className = '',
  id,
}) => {
  const autoId = useId();
  const inputId = id || autoId;
  const errorId = error ? `${inputId}-error` : undefined;
  const inputClasses = `
    w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 transition-colors
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
        <label htmlFor={inputId} className="block text-sm font-medium text-gray-300 mb-2">
          {label}
          {required && <span className="text-red-400 ml-1">*</span>}
        </label>
      )}
      <input
        id={inputId}
        type={type}
        value={value}
        defaultValue={defaultValue}
        onChange={onChange}
        placeholder={placeholder}
        disabled={disabled}
        readOnly={readOnly}
        required={required}
        aria-invalid={error ? 'true' : 'false'}
        aria-describedby={errorId}
        className={inputClasses}
      />
      {error && (
        <p id={errorId} className="mt-1 text-sm text-red-500" role="alert">
          {error}
        </p>
      )}
    </div>
  );
};