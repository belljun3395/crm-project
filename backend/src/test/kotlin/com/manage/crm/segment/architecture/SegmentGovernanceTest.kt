package com.manage.crm.segment.architecture

import com.manage.crm.architecture.BaseModuleGovernanceTest
import com.manage.crm.architecture.ModuleRuleSpec

class SegmentGovernanceTest : BaseModuleGovernanceTest() {
    override val spec = ModuleRuleSpec(
        moduleName = "segment",
        packageToken = "segment"
    )
}
