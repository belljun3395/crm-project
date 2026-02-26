import React, { useMemo, useState } from 'react';
import {
  HomeModernIcon,
  Squares2X2Icon,
  ChartBarSquareIcon,
  LinkIcon,
  UsersIcon,
  CalendarDaysIcon,
  EnvelopeOpenIcon,
  ClockIcon,
  FunnelIcon,
  SparklesIcon,
  BoltIcon,
  DocumentTextIcon
} from '@heroicons/react/24/outline';
import { DashboardPage } from 'page/dashboard';
import { CampaignDashboardPage } from 'page/campaign-dashboard';
import { WebhookManagementPage } from 'page/webhook';
import { UserPage } from 'page/user';
import { EventPage } from 'page/event';
import { TemplateManagementPage } from 'page/email/template';
import { EmailHistoryPage } from 'page/email/history';
import { ScheduleManagementPage } from 'page/email/schedule';
import { SegmentManagementPage } from 'page/segment';
import { JourneyManagementPage } from 'page/journey';
import { ActionDispatchPage } from 'page/action';
import { AuditLogPage } from 'page/audit-log';
import type { TabType } from 'shared/type';

interface NavItem {
  id: TabType;
  label: string;
  description: string;
  icon: React.ReactNode;
}

interface NavSection {
  id: string;
  label: string;
  description: string;
  itemIds: TabType[];
}

const navItems: NavItem[] = [
  {
    id: 'dashboard',
    label: 'Overview',
    description: '운영 지표',
    icon: <Squares2X2Icon className="h-4 w-4" />
  },
  {
    id: 'campaign-dashboard',
    label: 'Campaign',
    description: '캠페인 대시보드',
    icon: <ChartBarSquareIcon className="h-4 w-4" />
  },
  {
    id: 'webhook',
    label: 'Webhooks',
    description: '전달 로그 포함',
    icon: <LinkIcon className="h-4 w-4" />
  },
  {
    id: 'user',
    label: 'Users',
    description: '가입/조회',
    icon: <UsersIcon className="h-4 w-4" />
  },
  {
    id: 'event',
    label: 'Events',
    description: '이벤트 생성',
    icon: <CalendarDaysIcon className="h-4 w-4" />
  },
  {
    id: 'email-template',
    label: 'Templates',
    description: '메일 템플릿',
    icon: <EnvelopeOpenIcon className="h-4 w-4" />
  },
  {
    id: 'email-history',
    label: 'Histories',
    description: '메일 발송 이력',
    icon: <EnvelopeOpenIcon className="h-4 w-4" />
  },
  {
    id: 'email-schedule',
    label: 'Schedules',
    description: '발송 예약',
    icon: <ClockIcon className="h-4 w-4" />
  },
  {
    id: 'segments',
    label: 'Segments',
    description: '조건 기반 타겟',
    icon: <FunnelIcon className="h-4 w-4" />
  },
  {
    id: 'journeys',
    label: 'Journeys',
    description: '실행/이력 포함',
    icon: <SparklesIcon className="h-4 w-4" />
  },
  {
    id: 'actions',
    label: 'Actions',
    description: '즉시 디스패치',
    icon: <BoltIcon className="h-4 w-4" />
  },
  {
    id: 'audit-logs',
    label: 'Audit Logs',
    description: '운영 감사 로그',
    icon: <DocumentTextIcon className="h-4 w-4" />
  }
];

const navSections: NavSection[] = [
  {
    id: 'overview',
    label: '개요',
    description: '상태 모니터링',
    itemIds: ['dashboard', 'campaign-dashboard', 'audit-logs']
  },
  {
    id: 'customer',
    label: '고객 관리',
    description: '고객/행동/여정',
    itemIds: ['user', 'event', 'segments', 'journeys', 'actions']
  },
  {
    id: 'messaging',
    label: '메시지 운영',
    description: '템플릿/발송',
    itemIds: ['email-template', 'email-history', 'email-schedule']
  },
  {
    id: 'integration',
    label: '연동',
    description: '외부 전송',
    itemIds: ['webhook']
  }
];

function App() {
  const [activeTab, setActiveTab] = useState<TabType>('dashboard');

  const navItemMap = useMemo(() => {
    return new Map(navItems.map((item) => [item.id, item]));
  }, []);

  const activeNav = useMemo(() => {
    return navItemMap.get(activeTab) ?? navItems[0];
  }, [activeTab, navItemMap]);

  const renderContent = () => {
    switch (activeTab) {
      case 'dashboard':
        return <DashboardPage />;
      case 'campaign-dashboard':
        return <CampaignDashboardPage />;
      case 'webhook':
        return <WebhookManagementPage />;
      case 'user':
        return <UserPage />;
      case 'event':
        return <EventPage />;
      case 'email-template':
        return <TemplateManagementPage />;
      case 'email-history':
        return <EmailHistoryPage />;
      case 'email-schedule':
        return <ScheduleManagementPage />;
      case 'segments':
        return <SegmentManagementPage />;
      case 'journeys':
        return <JourneyManagementPage />;
      case 'actions':
        return <ActionDispatchPage />;
      case 'audit-logs':
        return <AuditLogPage />;
      default:
        return <DashboardPage />;
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100" style={{ fontFamily: '"Space Grotesk", "IBM Plex Sans KR", sans-serif' }}>
      <div className="pointer-events-none fixed inset-0 -z-10 bg-[radial-gradient(circle_at_10%_10%,rgba(34,211,238,0.18),transparent_35%),radial-gradient(circle_at_90%_0%,rgba(244,114,182,0.14),transparent_30%),linear-gradient(180deg,#020617_0%,#0f172a_100%)]" />

      <div className="mx-auto flex min-h-screen w-full max-w-[1800px] flex-col lg:flex-row">
        <aside className="border-b border-slate-800/70 bg-slate-900/50 px-4 py-5 backdrop-blur lg:w-80 lg:border-b-0 lg:border-r lg:px-5">
          <div className="mb-5 flex items-center gap-3">
            <div className="rounded-xl bg-gradient-to-br from-cyan-400 to-blue-500 p-2.5 text-slate-950">
              <HomeModernIcon className="h-5 w-5" />
            </div>
            <div>
              <p className="text-sm font-semibold text-white">CRM Control</p>
              <p className="text-xs text-slate-300">Frontend Console</p>
            </div>
          </div>

          <nav className="space-y-3">
            {navSections.map((section) => (
              <section key={section.id} className="rounded-xl border border-slate-800/80 bg-slate-900/55 p-2">
                <div className="px-2 pb-2 pt-1">
                  <p className="text-[11px] font-semibold uppercase tracking-[0.12em] text-cyan-300">{section.label}</p>
                  <p className="text-[11px] text-slate-400">{section.description}</p>
                </div>
                <div className="space-y-1">
                  {section.itemIds.map((itemId) => {
                    const item = navItemMap.get(itemId);
                    if (!item) {
                      return null;
                    }

                    return (
                      <button
                        key={item.id}
                        onClick={() => setActiveTab(item.id)}
                        className={`w-full rounded-lg border px-3 py-2 text-left transition ${
                          activeTab === item.id
                            ? 'border-cyan-400/80 bg-cyan-400/10 text-white shadow-[0_0_0_1px_rgba(34,211,238,0.15)]'
                            : 'border-slate-800 bg-slate-900/70 text-slate-300 hover:border-slate-700 hover:bg-slate-800/70 hover:text-white'
                        }`}
                      >
                        <div className="flex items-center gap-2 text-sm font-semibold">
                          {item.icon}
                          {item.label}
                        </div>
                        <p className="mt-1 pl-6 text-xs text-slate-400">{item.description}</p>
                      </button>
                    );
                  })}
                </div>
              </section>
            ))}
          </nav>
        </aside>

        <main className="flex-1 px-4 py-6 sm:px-6 lg:px-10 lg:py-8">
          <header className="rounded-2xl border border-slate-800/80 bg-slate-900/55 px-5 py-4 backdrop-blur">
            <p className="text-xs uppercase tracking-[0.18em] text-cyan-300">Active View</p>
            <div className="mt-1 flex flex-wrap items-end justify-between gap-3">
              <div>
                <h1 className="text-2xl font-semibold text-white">{activeNav.label}</h1>
                <p className="text-sm text-slate-300">{activeNav.description}</p>
              </div>
              <p className="text-xs text-slate-400">
                {new Date().toLocaleString('ko-KR', { dateStyle: 'long', timeStyle: 'short' })}
              </p>
            </div>
          </header>

          <section className="mt-6">{renderContent()}</section>
        </main>
      </div>
    </div>
  );
}

export default App;
