import { expect, test, type Page, type TestInfo } from '@playwright/test';
import { copyFile, mkdir } from 'fs/promises';
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

test.describe('CRM Console Validation/Error', () => {
  test('입력 검증 실패 메시지를 표시한다', async ({ page }, testInfo) => {
    await registerCrmApiMocks(page);
    await page.goto('/');

    await page.getByRole('button', { name: /^Webhooks/ }).click();
    await expect(page.getByRole('heading', { name: 'Webhooks' }).first()).toBeVisible();
    await page.getByRole('button', { name: 'New Webhook' }).click();

    const webhookDialog = page.getByRole('dialog', { name: 'Create Webhook' });
    await webhookDialog.getByLabel('Name').fill('invalid-hook');
    await webhookDialog.getByLabel('URL').fill('invalid-url');
    await webhookDialog.getByLabel('Events (comma separated)').fill('ORDER_CREATED');
    await webhookDialog.getByRole('button', { name: 'Create' }).click();
    await expect(page.getByText('Webhook URL must start with http:// or https://')).toBeVisible();

    await webhookDialog.getByLabel('URL').fill('https://hooks.example.com/valid');
    await webhookDialog.getByLabel('Events (comma separated)').fill('');
    await webhookDialog.getByRole('button', { name: 'Create' }).click();
    await expect(page.getByText('At least one event type is required')).toBeVisible();
    await captureStep(page, testInfo, '13-validation-webhook.png');
    await webhookDialog.getByRole('button', { name: 'Cancel' }).click();
    await expect(webhookDialog).toHaveCount(0);

    await page.getByRole('button', { name: /^Actions/ }).click();
    await expect(page.getByRole('heading', { name: 'Action Dispatch' })).toBeVisible();
    await page.getByRole('button', { name: 'Dispatch' }).click();
    await expect(page.getByText('destination, body는 필수입니다.')).toBeVisible();

    await page.getByLabel('Destination').fill('ops@example.com');
    await page.getByLabel('Message Body').fill('test-body');
    await page.getByLabel('Variables JSON').fill('{broken');
    await page.getByRole('button', { name: 'Dispatch' }).click();
    await expect(page.getByText('variables JSON 형식을 확인해주세요.')).toBeVisible();
    await captureStep(page, testInfo, '14-validation-actions.png');
  });

  test('API 오류를 UI 에러 메시지로 표시한다', async ({ page }, testInfo) => {
    await registerCrmApiMocks(page, {
      failureRules: [
        { method: 'GET', path: '/api/v1/webhooks', status: 500 },
        { method: 'POST', path: '/api/v1/actions/dispatch', status: 500 }
      ]
    });
    await page.goto('/');

    await page.getByRole('button', { name: /^Webhooks/ }).click();
    await expect(page.getByRole('heading', { name: 'Webhooks' }).first()).toBeVisible();
    await expect(page.getByText('Failed to load webhooks')).toBeVisible();
    await captureStep(page, testInfo, '15-error-webhooks-fetch.png');

    await page.getByRole('button', { name: /^Actions/ }).click();
    await expect(page.getByRole('heading', { name: 'Action Dispatch' })).toBeVisible();
    await page.getByLabel('Destination').fill('ops@example.com');
    await page.getByLabel('Message Body').fill('error-case-body');
    await page.getByRole('button', { name: 'Dispatch' }).click();
    await expect(page.getByText('Failed to dispatch action')).toBeVisible();
    await captureStep(page, testInfo, '16-error-action-dispatch.png');
  });
});
