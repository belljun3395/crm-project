import React, { useState } from 'react';
import { DashboardPage } from 'page/dashboard';
import { UserPage } from 'page/user';
import { EventPage } from 'page/event';
import { TemplateManagementPage } from 'page/email/template';
import { ScheduleManagementPage } from 'page/email/schedule';
import type { TabType } from 'shared/type';

function App() {
  const [activeTab, setActiveTab] = useState<TabType>('dashboard');

  const renderContent = () => {
    switch (activeTab) {
      case 'dashboard':
        return <DashboardPage />;
      case 'user':
        return <UserPage />;
      case 'event':
        return <EventPage />;
      case 'email-template':
        return <TemplateManagementPage />;
      case 'email-schedule':
        return <ScheduleManagementPage />;
      default:
        return <DashboardPage />;
    }
  };

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100" style={{ fontFamily: '"Spline Sans", "Noto Sans", sans-serif' }}>
      <div className="flex min-h-screen">
        {/* 사이드바 */}
        <aside className="w-64 shrink-0 border-r border-gray-800 bg-gray-900 p-6">
          <div className="flex h-full flex-col">
            <div className="flex flex-col gap-8">
              {/* 로고 */}
              <div className="flex items-center gap-3">
                <div className="rounded-lg bg-[#22c55e] p-2">
                  <div className="h-6 w-6 text-white">🏠</div>
                </div>
                <h1 className="text-xl font-bold">CRM</h1>
              </div>
              
              {/* 네비게이션 */}
              <nav className="flex flex-col gap-2">
                <button
                  onClick={() => setActiveTab('dashboard')}
                  className={`flex items-center gap-3 rounded-lg px-4 py-2 font-semibold transition-colors text-left ${
                    activeTab === 'dashboard' 
                      ? 'bg-gray-800 text-white' 
                      : 'text-gray-400 hover:bg-gray-800 hover:text-white'
                  }`}
                >
                  <span className="text-lg">📊</span>
                  Dashboard
                </button>
                
                <button
                  onClick={() => setActiveTab('user')}
                  className={`flex items-center gap-3 rounded-lg px-4 py-2 font-semibold transition-colors text-left ${
                    activeTab === 'user' 
                      ? 'bg-gray-800 text-white' 
                      : 'text-gray-400 hover:bg-gray-800 hover:text-white'
                  }`}
                >
                  <span className="text-lg">👥</span>
                  Users
                </button>
                
                <button
                  onClick={() => setActiveTab('event')}
                  className={`flex items-center gap-3 rounded-lg px-4 py-2 font-semibold transition-colors text-left ${
                    activeTab === 'event' 
                      ? 'bg-gray-800 text-white' 
                      : 'text-gray-400 hover:bg-gray-800 hover:text-white'
                  }`}
                >
                  <span className="text-lg">📅</span>
                  Events
                </button>
                
                <button
                  onClick={() => setActiveTab('email-template')}
                  className={`flex items-center gap-3 rounded-lg px-4 py-2 font-semibold transition-colors text-left ${
                    activeTab === 'email-template' 
                      ? 'bg-gray-800 text-white' 
                      : 'text-gray-400 hover:bg-gray-800 hover:text-white'
                  }`}
                >
                  <span className="text-lg">📧</span>
                  Email Templates
                </button>
                
                <button
                  onClick={() => setActiveTab('email-schedule')}
                  className={`flex items-center gap-3 rounded-lg px-4 py-2 font-semibold transition-colors text-left ${
                    activeTab === 'email-schedule' 
                      ? 'bg-gray-800 text-white' 
                      : 'text-gray-400 hover:bg-gray-800 hover:text-white'
                  }`}
                >
                  <span className="text-lg">⏰</span>
                  Email Schedules
                </button>
              </nav>
            </div>
          </div>
        </aside>

        {/* 메인 콘텐츠 */}
        <main className="flex-1 overflow-y-auto p-8">
          <div className="max-w-7xl mx-auto">
            {renderContent()}
          </div>
        </main>
      </div>
    </div>
  );
}

export default App;