package com.manage.crm.journey.architecture

import com.manage.crm.architecture.BaseModuleGovernanceTest
import com.manage.crm.architecture.ModuleRuleSpec

class JourneyGovernanceTest : BaseModuleGovernanceTest() {
    override val spec =
        ModuleRuleSpec(
            moduleName = "journey",
            packageToken = "journey",
        )
}
