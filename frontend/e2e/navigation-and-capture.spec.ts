import { expect, test, type Page, type TestInfo } from '@playwright/test';
import { mkdir, copyFile } from 'fs/promises';
import path from 'path';
import { registerCrmApiMocks } from './fixtures/apiMocks';

const recordedDir = path.resolve(__dirname, 'screenshots');

const captureStep = async (
  page: Page,
  testInfo: TestInfo,
  fileName: string
) => {
  const outputPath = testInfo.outputPath(fileName);
  await page.screenshot({ path: outputPath, fullPage: true });
  await testInfo.attach(fileName, { path: outputPath, contentType: 'image/png' });

  await mkdir(recordedDir, { recursive: true });
  await copyFile(outputPath, path.join(recordedDir, fileName));
};

const acceptNextDialog = (page: Page) => {
  page.once('dialog', async (dialog) => {
    await dialog.accept();
  });
};

test.describe('CRM Console Navigation', () => {
  test('고객/이벤트 기능 플로우를 검증하고 캡처를 남긴다', async ({ page }, testInfo) => {
    await registerCrmApiMocks(page);
    await page.goto('/');

    await expect(page.getByRole('heading', { name: 'Overview' }).first()).toBeVisible();
    await expect(page.getByText('Total Users')).toBeVisible();
    await expect(page.getByText('2').first()).toBeVisible();
    await captureStep(page, testInfo, '01-dashboard-overview.png');

    await page.getByRole('button', { name: /^Users/ }).click();
    await expect(page.getByRole('heading', { name: 'Users' }).first()).toBeVisible();
    await expect(page.getByText('usr-alpha')).toBeVisible();
    await page.getByRole('button', { name: 'Enroll User' }).click();

    const userDialog = page.getByRole('dialog', { name: 'Enroll User' });
    await userDialog.getByLabel('External ID').fill('usr-charlie');
    await userDialog.getByLabel('User Attributes').fill('{"tier":"bronze","region":"incheon"}');
    await userDialog.getByRole('button', { name: 'Enroll' }).click();

    await expect(page.getByText('usr-charlie')).toBeVisible();
    await page.getByPlaceholder('Search users by external ID or attributes...').fill('usr-charlie');
    await expect(page.getByText('usr-charlie')).toBeVisible();
    await captureStep(page, testInfo, '02-users-enroll.png');

    await page.getByRole('button', { name: /^Events/ }).click();
    await expect(page.getByRole('heading', { name: 'Events' }).first()).toBeVisible();
    await page.getByLabel('Event Name').fill('view_product');
    await page.getByLabel('Where').fill('category&electronics&=&end');
    await page.getByRole('button', { name: 'Search' }).click();
    await expect(page.getByText('view_product')).toBeVisible();

    await page.getByRole('button', { name: 'New Event' }).click();
    const eventDialog = page.getByRole('dialog', { name: 'Create Event' });
    await eventDialog.getByLabel('Event Name').fill('purchase_completed');
    await eventDialog.getByLabel('Campaign Name').fill('promo-campaign');
    await eventDialog.getByLabel('External ID').fill('evt-3001');
    await eventDialog.getByLabel('Property Key').fill('category');
    await eventDialog.getByLabel('Property Value').fill('electronics');
    await eventDialog.getByRole('button', { name: 'Create' }).click();

    await page.getByLabel('Event Name').fill('purchase_completed');
    await page.getByRole('button', { name: 'Search' }).click();
    await expect(page.getByText('purchase_completed')).toBeVisible();
    await captureStep(page, testInfo, '03-events-search-create.png');
  });

  test('메시징 기능 플로우를 검증하고 캡처를 남긴다', async ({ page }, testInfo) => {
    await registerCrmApiMocks(page);
    await page.goto('/');

    await page.getByRole('button', { name: /^Templates/ }).click();
    await expect(page.getByRole('heading', { name: 'Email Templates' })).toBeVisible();
    await page.getByRole('button', { name: 'New Template' }).click();

    const templateDialog = page.getByRole('dialog', { name: 'Create Template' });
    await templateDialog.getByLabel('Template Name').fill('promo-template');
    await templateDialog.getByLabel('Email Subject').fill('Promotion');
    await templateDialog.getByLabel('Email Body').fill('Hi {{name}}, welcome to our promo!');
    await templateDialog.getByLabel('Variables').fill('name, discount');
    await templateDialog.getByRole('button', { name: 'Create' }).click();

    await page.getByPlaceholder('Search templates...').fill('promo-template');
    await expect(page.getByText('promo-template')).toBeVisible();
    await captureStep(page, testInfo, '04-templates-create.png');

    await page.getByRole('button', { name: /^Schedules/ }).click();
    await expect(page.getByRole('heading', { name: 'Email Schedules' })).toBeVisible();
    await page.getByRole('button', { name: 'Schedule Email' }).click();

    const scheduleDialog = page.getByRole('dialog', { name: 'Schedule Email' });
    await scheduleDialog.locator('select').selectOption({ label: 'promo-template' });
    await scheduleDialog.getByLabel('User IDs').fill('101, 103');
    await scheduleDialog.getByLabel('Expiry Time').fill('2026-12-31T09:00');
    await scheduleDialog.getByRole('button', { name: 'Schedule' }).click();

    const newScheduleRow = page.locator('tbody tr', { hasText: 'scheduled-1000' });
    await expect(newScheduleRow).toBeVisible();

    acceptNextDialog(page);
    await newScheduleRow.getByRole('button', { name: 'Cancel' }).click();
    await expect(page.locator('tbody tr', { hasText: 'scheduled-1000' })).toHaveCount(0);
    await captureStep(page, testInfo, '05-schedules-create-cancel.png');

    await page.getByRole('button', { name: /^Histories/ }).click();
    await expect(page.getByRole('heading', { name: 'Email Histories' })).toBeVisible();
    await page.getByLabel('User ID').fill('101');
    await page.getByLabel('Send Status').fill('SENT');
    await page.getByRole('button', { name: '조회', exact: true }).click();
    await expect(page.getByText('alpha@example.com')).toBeVisible();
    await captureStep(page, testInfo, '06-email-histories-filter.png');
  });

  test('운영 기능 플로우를 검증하고 캡처를 남긴다', async ({ page }, testInfo) => {
    await registerCrmApiMocks(page);
    await page.goto('/');

    await page.getByRole('button', { name: /^Campaign/ }).click();
    await expect(page.getByRole('heading', { name: 'Campaign Dashboard' })).toBeVisible();
    await page.getByRole('button', { name: 'Refresh' }).click();
    await expect(page.getByText('Total Events')).toBeVisible();
    await expect(page.getByText('128')).toBeVisible();
    await captureStep(page, testInfo, '07-campaign-dashboard.png');

    await page.getByRole('button', { name: /^Webhooks/ }).click();
    await expect(page.getByRole('heading', { name: 'Webhooks' }).first()).toBeVisible();
    await page.getByRole('button', { name: 'New Webhook' }).click();

    const createWebhookDialog = page.getByRole('dialog', { name: 'Create Webhook' });
    await createWebhookDialog.getByLabel('Name').fill('billing-events');
    await createWebhookDialog.getByLabel('URL').fill('https://hooks.example.com/billing');
    await createWebhookDialog.getByLabel('Events (comma separated)').fill('INVOICE_CREATED');
    await createWebhookDialog.getByRole('button', { name: 'Create' }).click();

    const createdWebhookRow = page.locator('tbody tr', { hasText: 'billing-events' });
    await expect(createdWebhookRow).toBeVisible();
    await createdWebhookRow.getByRole('button', { name: 'Edit' }).click();

    const editWebhookDialog = page.getByRole('dialog', { name: /Edit Webhook/ });
    await editWebhookDialog.getByLabel('Name').fill('billing-events-v2');
    await editWebhookDialog.getByRole('button', { name: 'Update' }).click();

    const updatedWebhookRow = page.locator('tbody tr', { hasText: 'billing-events-v2' });
    await expect(updatedWebhookRow).toBeVisible();

    acceptNextDialog(page);
    await updatedWebhookRow.getByRole('button', { name: 'Delete' }).click();
    await expect(page.locator('tbody tr', { hasText: 'billing-events-v2' })).toHaveCount(0);
    await captureStep(page, testInfo, '08-webhooks-crud.png');

    await page.getByRole('button', { name: /^Segments/ }).click();
    await expect(page.getByRole('heading', { name: 'Segments' }).first()).toBeVisible();
    await page.getByRole('button', { name: '새 세그먼트' }).click();

    const createSegmentDialog = page.getByRole('dialog', { name: 'Create Segment' });
    await createSegmentDialog.getByLabel('Name').fill('vip-users');
    await createSegmentDialog.getByLabel('Description').fill('vip targeting group');
    await createSegmentDialog.getByRole('button', { name: '생성' }).click();

    const createdSegmentRow = page.locator('tbody tr', { hasText: 'vip-users' });
    await expect(createdSegmentRow).toBeVisible();
    await createdSegmentRow.getByRole('button', { name: 'Edit' }).click();

    const editSegmentDialog = page.getByRole('dialog', { name: /Edit Segment/ });
    await editSegmentDialog.getByLabel('Name').fill('vip-users-updated');
    await editSegmentDialog.getByRole('button', { name: '수정' }).click();
    await expect(page.locator('tbody tr', { hasText: 'vip-users-updated' })).toBeVisible();
    await captureStep(page, testInfo, '09-segments-create-update.png');

    await page.getByRole('button', { name: /^Journeys/ }).click();
    await expect(page.getByRole('heading', { name: 'Journeys' }).first()).toBeVisible();
    await expect(page.getByText('welcome-journey')).toBeVisible();
    await page.getByRole('button', { name: 'History' }).first().click();
    await expect(page.getByText('10001')).toBeVisible();
    await page.getByRole('button', { name: '새 여정' }).click();

    const createJourneyDialog = page.getByRole('dialog', { name: 'Create Journey' });
    await createJourneyDialog.getByLabel('Journey Name').fill('retention-journey');
    await createJourneyDialog.getByRole('button', { name: '생성' }).click();
    await expect(page.getByText('retention-journey')).toBeVisible();
    await captureStep(page, testInfo, '10-journeys-history-create.png');

    await page.getByRole('button', { name: /^Actions/ }).click();
    await expect(page.getByRole('heading', { name: 'Action Dispatch' })).toBeVisible();
    await page.getByLabel('Destination').fill('ops@example.com');
    await page.getByLabel('Message Body').fill('incident notice');
    await page.getByRole('button', { name: 'Dispatch' }).click();
    await expect(page.getByText('ops@example.com')).toBeVisible();
    await captureStep(page, testInfo, '11-actions-dispatch.png');

    await page.getByRole('button', { name: /^Audit Logs/ }).click();
    await expect(page.getByRole('heading', { name: 'Audit Logs', level: 2 })).toBeVisible();
    await page.getByLabel('Action').fill('ACTION_DISPATCHED');
    await page.getByRole('button', { name: '조회', exact: true }).click();
    await expect(page.getByText('ACTION_DISPATCHED')).toBeVisible();
    await captureStep(page, testInfo, '12-audit-filter.png');
  });
});
