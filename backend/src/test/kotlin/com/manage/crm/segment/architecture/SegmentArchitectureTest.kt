package com.manage.crm.segment.architecture

import com.manage.crm.architecture.BaseModuleArchitectureTest
import com.manage.crm.architecture.ControllerReturnPolicy
import com.manage.crm.architecture.DEFAULT_CROSS_MODULE_READ_METHOD_PREFIXES
import com.manage.crm.architecture.DEFAULT_INTERNAL_LAYER_ALLOWED_EXTERNAL_MODULES
import com.manage.crm.architecture.ModuleRuleSpec

class SegmentArchitectureTest : BaseModuleArchitectureTest() {
    override val spec =
        ModuleRuleSpec(
            moduleName = "segment",
            packageToken = "segment",
            controllerReturnPolicy = ControllerReturnPolicy.USE_CASE_OUT_OR_DTO,
            enforceCrossModuleDependencyViaQueryFacade = true,
            enforceCrossModuleReadPortPrefix = true,
            enforceInternalLayerNoCrossModuleDependency = true,
            enforceUtilPureFunctions = false,
            crossModuleReadMethodPrefixes = DEFAULT_CROSS_MODULE_READ_METHOD_PREFIXES + "trigger",
            internalLayerAllowedExternalModules = DEFAULT_INTERNAL_LAYER_ALLOWED_EXTERNAL_MODULES + setOf("user", "event"),
        )
}
