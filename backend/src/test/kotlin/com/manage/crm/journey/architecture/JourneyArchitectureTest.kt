package com.manage.crm.journey.architecture

import com.manage.crm.architecture.BaseModuleArchitectureTest
import com.manage.crm.architecture.ControllerReturnPolicy
import com.manage.crm.architecture.ModuleRuleSpec

class JourneyArchitectureTest : BaseModuleArchitectureTest() {
    override val spec =
        ModuleRuleSpec(
            moduleName = "journey",
            packageToken = "journey",
            controllerReturnPolicy = ControllerReturnPolicy.USE_CASE_OUT_OR_DTO,
            enforceCrossModuleDependencyViaQueryFacade = true,
            enforceCrossModuleReadPortPrefix = true,
            enforceUtilPureFunctions = true,
        )
}
