# API Domain Capabilities

## User

`backend/.../user/controller/UserController.kt`

- `GET /api/v1/users`
- `POST /api/v1/users`
- `GET /api/v1/users/count`

## Email

`backend/.../email/controller/EmailController.kt`

- `GET /api/v1/emails/templates`
- `POST /api/v1/emails/templates`
- `DELETE /api/v1/emails/templates/{templateId}`
- `GET /api/v1/emails/templates/variable-catalog`
- `POST /api/v1/emails/send/notifications`
- `GET /api/v1/emails/schedules/notifications/email`
- `POST /api/v1/emails/schedules/notifications/email`
- `DELETE /api/v1/emails/schedules/notifications/email/{scheduleId}`
- `GET /api/v1/emails/histories`

## Event

`backend/.../event/controller/EventController.kt`

- `POST /api/v1/events`
- `GET /api/v1/events`
- `GET /api/v1/events/all`
- `POST /api/v1/events/campaign`

## Campaign Dashboard

`backend/.../event/controller/CampaignDashboardController.kt`

- `GET /api/v1/campaigns`
- `GET /api/v1/campaigns/{campaignId}`
- `POST /api/v1/campaigns`
- `PUT /api/v1/campaigns/{campaignId}`
- `DELETE /api/v1/campaigns/{campaignId}`
- `GET /api/v1/campaigns/{campaignId}/dashboard`
- `GET /api/v1/campaigns/{campaignId}/dashboard/stream`
- `GET /api/v1/campaigns/{campaignId}/dashboard/summary`
- `GET /api/v1/campaigns/{campaignId}/dashboard/stream/status`
- `GET /api/v1/campaigns/{campaignId}/analytics/funnel`
- `GET /api/v1/campaigns/{campaignId}/analytics/segment-comparison`

## Segment

`backend/.../segment/controller/SegmentController.kt`

- `POST /api/v1/segments`
- `PUT /api/v1/segments/{id}`
- `DELETE /api/v1/segments/{id}`
- `GET /api/v1/segments`
- `GET /api/v1/segments/{id}`
- `GET /api/v1/segments/{id}/users`

## Journey

`backend/.../journey/controller/JourneyController.kt`

- `POST /api/v1/journeys`
- `PUT /api/v1/journeys/{journeyId}`
- `POST /api/v1/journeys/{journeyId}/pause`
- `POST /api/v1/journeys/{journeyId}/resume`
- `POST /api/v1/journeys/{journeyId}/archive`
- `GET /api/v1/journeys`
- `GET /api/v1/journeys/executions`
- `GET /api/v1/journeys/executions/{executionId}/histories`

## Webhook

`backend/.../webhook/controller/WebhookController.kt`

- `POST /api/v1/webhooks`
- `PUT /api/v1/webhooks/{id}`
- `DELETE /api/v1/webhooks/{id}`
- `GET /api/v1/webhooks`
- `GET /api/v1/webhooks/{id}`
- `GET /api/v1/webhooks/{id}/deliveries`
- `GET /api/v1/webhooks/{id}/dead-letters`
- `POST /api/v1/webhooks/{id}/dead-letters/{deadLetterId}/retry`
- `POST /api/v1/webhooks/{id}/dead-letters/retry`

## Action

`backend/.../action/controller/ActionController.kt`

- `POST /api/v1/actions/dispatch`
- `GET /api/v1/actions/dispatch/histories`

## Audit

`backend/.../audit/controller/AuditLogController.kt`

- `GET /api/v1/audit-logs`
