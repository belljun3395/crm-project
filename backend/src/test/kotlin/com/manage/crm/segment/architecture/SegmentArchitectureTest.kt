package com.manage.crm.segment.architecture

import com.manage.crm.architecture.BaseModuleArchitectureTest
import com.manage.crm.architecture.ControllerReturnPolicy
import com.manage.crm.architecture.ModuleRuleSpec

class SegmentArchitectureTest : BaseModuleArchitectureTest() {
    override val spec = ModuleRuleSpec(
        moduleName = "segment",
        packageToken = "segment",
        controllerReturnPolicy = ControllerReturnPolicy.USE_CASE_OUT_OR_DTO,
        enforceCrossModuleDependencyViaQueryFacade = true,
        enforceCrossModuleReadPortPrefix = true,
        enforceUtilPureFunctions = false
    )
}
