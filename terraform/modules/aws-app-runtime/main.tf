terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.0"
    }
  }
}

locals {
  cache_invalidation_topic_name = coalesce(var.cache_invalidation_topic_name, "${var.name_prefix}-cache-invalidation-topic")
  schedule_group_name        = coalesce(var.schedule_group_name, "${var.name_prefix}-schedule-group")
  schedule_role_name         = coalesce(var.schedule_role_name, "${var.name_prefix}-scheduler-role")
  ses_configuration_set_name = coalesce(var.ses_configuration_set_name, "${var.name_prefix}-ses-configuration-set")
  runtime_user_name          = coalesce(var.runtime_user_name, "${var.name_prefix}-app-runtime")

  queue_arns = [
    aws_sqs_queue.cache_invalidation.arn,
    aws_sqs_queue.schedule_event.arn,
    aws_sqs_queue.ses.arn,
  ]
}

resource "aws_sns_topic" "cache_invalidation" {
  name = local.cache_invalidation_topic_name

  tags = var.tags
}

resource "aws_sqs_queue" "cache_invalidation" {
  name = var.cache_invalidation_queue_name

  tags = var.tags
}

resource "aws_sqs_queue" "schedule_event" {
  name = var.schedule_queue_name

  tags = var.tags
}

resource "aws_sqs_queue" "ses" {
  name = var.ses_queue_name

  tags = var.tags
}

data "aws_iam_policy_document" "cache_invalidation_queue" {
  statement {
    sid = "AllowSnsTopicToSendMessages"

    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }

    actions   = ["sqs:SendMessage"]
    resources = [aws_sqs_queue.cache_invalidation.arn]

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [aws_sns_topic.cache_invalidation.arn]
    }
  }
}

resource "aws_sqs_queue_policy" "cache_invalidation" {
  queue_url = aws_sqs_queue.cache_invalidation.id
  policy    = data.aws_iam_policy_document.cache_invalidation_queue.json
}

resource "aws_sns_topic_subscription" "cache_invalidation" {
  topic_arn            = aws_sns_topic.cache_invalidation.arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.cache_invalidation.arn
  raw_message_delivery = false

  depends_on = [aws_sqs_queue_policy.cache_invalidation]
}

resource "aws_scheduler_schedule_group" "this" {
  name = local.schedule_group_name

  tags = var.tags
}

resource "aws_iam_role" "scheduler_execution" {
  name               = local.schedule_role_name
  assume_role_policy = data.aws_iam_policy_document.scheduler_assume_role.json

  tags = var.tags
}

data "aws_iam_policy_document" "scheduler_assume_role" {
  statement {
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["scheduler.amazonaws.com"]
    }

    actions = ["sts:AssumeRole"]
  }
}

data "aws_iam_policy_document" "scheduler_execution" {
  statement {
    sid = "AllowScheduleToSendToQueue"

    effect = "Allow"

    actions = ["sqs:SendMessage"]
    resources = [aws_sqs_queue.schedule_event.arn]
  }
}

resource "aws_iam_role_policy" "scheduler_execution" {
  name   = "${local.schedule_role_name}-policy"
  role   = aws_iam_role.scheduler_execution.id
  policy = data.aws_iam_policy_document.scheduler_execution.json
}

resource "aws_ses_configuration_set" "this" {
  name = local.ses_configuration_set_name
}

resource "aws_iam_user" "runtime" {
  name = local.runtime_user_name

  tags = var.tags
}

resource "aws_iam_access_key" "runtime" {
  user = aws_iam_user.runtime.name
}

data "aws_iam_policy_document" "runtime" {
  statement {
    sid = "AllowCacheInvalidation"

    effect = "Allow"

    actions   = ["sns:Publish", "sns:GetTopicAttributes"]
    resources = [aws_sns_topic.cache_invalidation.arn]
  }

  statement {
    sid = "AllowQueueConsumption"

    effect = "Allow"

    actions = [
      "sqs:GetQueueUrl",
      "sqs:GetQueueAttributes",
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:ChangeMessageVisibility",
      "sqs:SendMessage",
    ]
    resources = local.queue_arns
  }

  statement {
    sid = "AllowMailAndScheduling"

    effect = "Allow"

    actions = [
      "ses:SendEmail",
      "ses:SendRawEmail",
      "scheduler:CreateSchedule",
      "scheduler:DeleteSchedule",
      "scheduler:GetSchedule",
      "scheduler:ListSchedules",
      "scheduler:ListScheduleGroups",
      "scheduler:UpdateSchedule",
    ]
    resources = ["*"]
  }

  statement {
    sid = "AllowPassSchedulerRole"

    effect = "Allow"

    actions   = ["iam:PassRole"]
    resources = [aws_iam_role.scheduler_execution.arn]

    condition {
      test     = "StringEquals"
      variable = "iam:PassedToService"
      values   = ["scheduler.amazonaws.com"]
    }
  }
}

resource "aws_iam_user_policy" "runtime" {
  name   = "${local.runtime_user_name}-policy"
  user   = aws_iam_user.runtime.name
  policy = data.aws_iam_policy_document.runtime.json
}
