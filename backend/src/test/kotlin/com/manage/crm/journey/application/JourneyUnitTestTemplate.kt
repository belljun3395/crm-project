package com.manage.crm.journey.application

import io.kotest.core.spec.style.BehaviorSpec

abstract class JourneyUnitTestTemplate(
    body: BehaviorSpec.() -> Unit = {},
) : BehaviorSpec(body)
