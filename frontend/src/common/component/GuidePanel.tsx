import React from 'react';

interface GuidePanelProps {
  title?: string;
  description: string;
  items?: string[];
  note?: string;
  className?: string;
  collapsible?: boolean;
  defaultOpen?: boolean;
}

export const GuidePanel: React.FC<GuidePanelProps> = ({
  title = '화면 안내',
  description,
  items = [],
  note,
  className = '',
  collapsible = true,
  defaultOpen = false
}) => {
  const content = (
    <>
      <p className="mt-2 text-sm text-cyan-50/90">{description}</p>

      {items.length > 0 && (
        <ul className="mt-2 list-disc space-y-1 pl-5 text-xs text-cyan-50/90">
          {items.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      )}

      {note && <p className="mt-2 text-xs text-cyan-50/80">{note}</p>}
    </>
  );

  if (!collapsible) {
    return (
      <section className={`rounded-xl border border-cyan-500/30 bg-cyan-500/10 p-4 ${className}`.trim()}>
        <h3 className="text-sm font-semibold text-cyan-100">{title}</h3>
        {content}
      </section>
    );
  }

  return (
    <details
      open={defaultOpen}
      className={`group rounded-xl border border-cyan-500/30 bg-cyan-500/10 p-4 ${className}`.trim()}
    >
      <summary className="flex cursor-pointer list-none items-center justify-between gap-3 [&::-webkit-details-marker]:hidden">
        <span className="text-sm font-semibold text-cyan-100">{title}</span>
        <span className="text-xs text-cyan-200">
          <span className="group-open:hidden">펼치기</span>
          <span className="hidden group-open:inline">접기</span>
        </span>
      </summary>
      <div className="mt-2 border-t border-cyan-400/20 pt-2">{content}</div>
    </details>
  );
};
