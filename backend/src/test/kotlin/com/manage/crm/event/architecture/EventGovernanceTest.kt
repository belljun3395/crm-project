package com.manage.crm.event.architecture

import com.manage.crm.architecture.BaseModuleGovernanceTest
import com.manage.crm.architecture.ModuleRuleSpec

class EventGovernanceTest : BaseModuleGovernanceTest() {
    override val spec =
        ModuleRuleSpec(
            moduleName = "event",
            packageToken = "event",
        )
}
