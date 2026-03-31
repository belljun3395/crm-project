package com.manage.crm.event.architecture

import com.manage.crm.architecture.BaseModuleArchitectureTest
import com.manage.crm.architecture.ControllerReturnPolicy
import com.manage.crm.architecture.ModuleRuleSpec

class EventArchitectureTest : BaseModuleArchitectureTest() {
    override val spec = ModuleRuleSpec(
        moduleName = "event",
        packageToken = "event",
        controllerReturnPolicy = ControllerReturnPolicy.USE_CASE_OUT_OR_DTO,
        enforceCrossModuleDependencyViaQueryFacade = true,
        enforceCrossModuleReadPortPrefix = true,
        enforceUtilPureFunctions = true
    )
}
