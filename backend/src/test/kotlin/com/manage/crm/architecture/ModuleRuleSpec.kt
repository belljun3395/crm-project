package com.manage.crm.architecture

enum class ControllerReturnPolicy {
    DISABLED,
    USE_CASE_OUT_ONLY,
    USE_CASE_OUT_OR_DTO
}

data class ModuleRuleSpec(
    val moduleName: String,
    val packageToken: String,
    val ucCodeRegex: Regex = Regex("UC-([A-Z]+)-(\\d{3})"),
    val enforceControllerDependencyOnApplication: Boolean = true,
    val enforceServiceMinCollaborators: Boolean = true,
    val serviceMinCollaborators: Int = 2,
    val enforceUseCaseExecuteSignature: Boolean = true,
    val enforceUseCaseDtoNoSpringAnnotations: Boolean = true,
    val enforceRepositoryImplContract: Boolean = true,
    val controllerReturnPolicy: ControllerReturnPolicy = ControllerReturnPolicy.DISABLED,
    val allowStreamingControllerReturn: Boolean = true,
    val allowVoidControllerReturn: Boolean = true,
    val enforceCrossModuleDependencyViaQueryFacade: Boolean = false,
    val enforceCrossModuleReadPortPrefix: Boolean = false,
    val crossModuleForbiddenLayerSegments: List<String> = DEFAULT_CROSS_MODULE_FORBIDDEN_LAYER_SEGMENTS,
    val crossModuleAllowedPortNameSuffixes: List<String> = DEFAULT_CROSS_MODULE_ALLOWED_PORT_NAME_SUFFIXES,
    val crossModuleReadMethodPrefixes: List<String> = DEFAULT_CROSS_MODULE_READ_METHOD_PREFIXES,
    val enforceUtilPureFunctions: Boolean = false,
    val utilForbiddenImportPrefixes: List<String> = DEFAULT_UTIL_FORBIDDEN_IMPORT_PREFIXES
) {
    val moduleBasePackage: String get() = "com.manage.crm.$packageToken"
    val modulePackagePattern: String get() = "..$packageToken.."
    val applicationPackagePattern: String get() = "..$packageToken.application.."
    val applicationDtoPackagePattern: String get() = "..$packageToken.application.dto.."
    val controllerPackagePattern: String get() = "..$packageToken.controller.."
    val servicePackagePattern: String get() = "..$packageToken.service.."
    val utilPackagePattern: String get() = "..$packageToken.util.."
    val repositoryPackagePattern: String get() = "..$packageToken.domain.repository.."
    val utilPackageName: String get() = "$moduleBasePackage.util"
}

val DEFAULT_UTIL_FORBIDDEN_IMPORT_PREFIXES = listOf(
    "java.io.",
    "java.net.",
    "java.nio.file.",
    "kotlin.io.",
    "kotlin.random.",
    "java.util.Random",
    "java.time.LocalDateTime",
    "java.time.Instant",
    "java.time.ZonedDateTime",
    "java.time.OffsetDateTime"
)

val DEFAULT_CROSS_MODULE_FORBIDDEN_LAYER_SEGMENTS = listOf(
    ".domain.",
    ".service.",
    ".controller."
)

val DEFAULT_CROSS_MODULE_ALLOWED_PORT_NAME_SUFFIXES = listOf(
    "Query",
    "Facade",
    "Port"
)

val DEFAULT_CROSS_MODULE_READ_METHOD_PREFIXES = listOf(
    "find",
    "get",
    "load",
    "exists",
    "count"
)
