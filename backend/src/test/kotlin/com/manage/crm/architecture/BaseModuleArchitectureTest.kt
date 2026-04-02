package com.manage.crm.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoFunctionDeclaration
import com.lemonappdev.konsist.api.declaration.KoTypeArgumentDeclaration
import com.lemonappdev.konsist.api.declaration.type.KoTypeDeclaration
import com.lemonappdev.konsist.api.ext.list.withAllAnnotationsOf
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController

abstract class BaseModuleArchitectureTest {
    protected abstract val spec: ModuleRuleSpec

    @Test
    fun `use case classes reside in application package`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withNameEndingWith("UseCase")
            .filter { it.resideInPackage(spec.modulePackagePattern) }
            .assertTrue { it.resideInPackage(spec.applicationPackagePattern) }
    }

    @Test
    fun `component classes in application end with UseCase`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withAllAnnotationsOf(Component::class)
            .filter { it.resideInPackage(spec.applicationPackagePattern) }
            .assertTrue { it.hasNameEndingWith("UseCase") }
    }

    @Test
    fun `application does not use Service annotation`() {
        val applicationServiceBeans =
            Konsist
                .scopeFromProduction()
                .classes()
                .withAllAnnotationsOf(Service::class)
                .filter { it.resideInPackage(spec.applicationPackagePattern) }

        assertTrue(applicationServiceBeans.isEmpty())
    }

    @Test
    fun `controllers depend only on application layer`() {
        if (!spec.enforceControllerDependencyOnApplication) {
            return
        }

        val controllers =
            Konsist
                .scopeFromProduction()
                .classes()
                .withAllAnnotationsOf(RestController::class)
                .filter { it.resideInPackage(spec.controllerPackagePattern) }

        controllers.assertTrue(
            additionalMessage = "Controller constructor dependencies must reside in ${spec.applicationPackagePattern}",
        ) { controller ->
            constructorDependencyTypes(controller).all { dependency ->
                dependency.residesInSourcePackage(spec.applicationPackagePattern)
            }
        }
    }

    @Test
    fun `services coordinate multiple collaborators`() {
        if (!spec.enforceServiceMinCollaborators) {
            return
        }

        val services =
            Konsist
                .scopeFromProduction()
                .classes()
                .withAllAnnotationsOf(Service::class)
                .filter { it.resideInPackage(spec.servicePackagePattern) }

        services.assertTrue(
            additionalMessage = "Service should have at least ${spec.serviceMinCollaborators} constructor dependencies",
        ) { service ->
            (service.primaryConstructor?.numParameters ?: 0) >= spec.serviceMinCollaborators
        }
    }

    @Test
    fun `application cross-module dependencies use external application Query or Facade or Port only`() {
        if (!spec.enforceCrossModuleDependencyViaQueryFacade) {
            return
        }

        val applicationClasses =
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.resideInPackage(spec.applicationPackagePattern) }

        val violations =
            applicationClasses.flatMap { owner ->
                constructorDependencyTypes(owner).mapNotNull { dependencyType ->
                    val dependency =
                        dependencyType.sourceDeclaration?.asClassOrInterfaceOrObjectDeclaration()
                            ?: return@mapNotNull null
                    val dependencyPackageName = dependency.packagee?.name ?: return@mapNotNull null
                    val dependencyModule = dependencyPackageName.crmModuleToken() ?: return@mapNotNull null
                    if (dependencyModule == spec.packageToken) {
                        return@mapNotNull null
                    }
                    if (!spec.crossModuleForbiddenLayerSegments.any { dependencyPackageName.contains(it) }) {
                        return@mapNotNull null
                    }

                    val isAllowedPort =
                        dependency.resideInPackage("..$dependencyModule.application..") &&
                            spec.crossModuleAllowedPortNameSuffixes.any { suffix -> dependency.name.endsWith(suffix) }
                    if (isAllowedPort) {
                        null
                    } else {
                        "${owner.name} -> $dependencyPackageName.${dependency.name}"
                    }
                }
            }

        check(violations.isEmpty()) {
            violations.joinToString(
                prefix = "[FAIL] Cross-module dependencies must use external application Query/Facade/Port only: ",
                separator = ", ",
            )
        }
    }

    @Test
    fun `cross-module Query or Facade methods start with configured read prefixes`() {
        if (!spec.enforceCrossModuleReadPortPrefix) {
            return
        }

        val applicationClasses =
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.resideInPackage(spec.applicationPackagePattern) }

        val externalPorts =
            applicationClasses
                .flatMap { owner ->
                    constructorDependencyTypes(owner)
                        .mapNotNull { dependencyType ->
                            dependencyType.sourceDeclaration?.asClassOrInterfaceOrObjectDeclaration()
                        }
                }.filter { dependency ->
                    val dependencyPackageName = dependency.packagee?.name ?: return@filter false
                    val dependencyModule = dependencyPackageName.crmModuleToken() ?: return@filter false
                    dependencyModule != spec.packageToken &&
                        dependency.resideInPackage("..$dependencyModule.application..") &&
                        spec.crossModuleAllowedPortNameSuffixes.any { suffix -> dependency.name.endsWith(suffix) }
                }.distinctBy { dependency ->
                    "${dependency.packagee?.name}.${dependency.name}"
                }

        val violations =
            externalPorts.flatMap { dependency ->
                dependency
                    .functions()
                    .filterNot { function -> function.name in OBJECT_FUNCTION_NAMES }
                    .filterNot { function ->
                        spec.crossModuleReadMethodPrefixes.any { prefix ->
                            function.name.startsWith(prefix)
                        }
                    }.map { function ->
                        "${dependency.packagee?.name}.${dependency.name}.${function.name}"
                    }
            }

        check(violations.isEmpty()) {
            violations.joinToString(
                prefix = "[FAIL] Cross-module Query/Facade methods must start with ${spec.crossModuleReadMethodPrefixes}: ",
                separator = ", ",
            )
        }
    }

    @Test
    fun `service and adapter layers do not depend on external crm modules`() {
        if (!spec.enforceInternalLayerNoCrossModuleDependency) {
            return
        }

        val allClasses =
            Konsist
                .scopeFromProduction()
                .classes()

        val violations =
            allClasses.flatMap { owner ->
                val allowedModules =
                    when {
                        owner.resideInPackage(spec.adapterPackagePattern) ->
                            spec.internalLayerAllowedExternalModules + spec.adapterLayerAllowedExternalModules
                        owner.resideInPackage(spec.servicePackagePattern) ->
                            spec.internalLayerAllowedExternalModules
                        else -> return@flatMap emptyList()
                    }
                constructorDependencyTypes(owner).mapNotNull { dependencyType ->
                    val dependency =
                        dependencyType.sourceDeclaration?.asClassOrInterfaceOrObjectDeclaration()
                            ?: return@mapNotNull null
                    val dependencyPackageName = dependency.packagee?.name ?: return@mapNotNull null
                    val dependencyModule = dependencyPackageName.crmModuleToken() ?: return@mapNotNull null
                    if (dependencyModule == spec.packageToken || dependencyModule in allowedModules) {
                        return@mapNotNull null
                    }
                    "${owner.name} -> $dependencyPackageName.${dependency.name}"
                }
            }

        check(violations.isEmpty()) {
            violations.joinToString(
                prefix = "[FAIL] service/adapter must not depend on external crm modules. Violations: ",
                separator = ", ",
            )
        }
    }

    @Test
    fun `use case has single execute signature with UseCaseIn parameter`() {
        if (!spec.enforceUseCaseExecuteSignature) {
            return
        }

        val useCases =
            Konsist
                .scopeFromProduction()
                .classes()
                .withNameEndingWith("UseCase")
                .filter { it.resideInPackage(spec.applicationPackagePattern) }

        val invalidUseCases =
            useCases.mapNotNull { useCase ->
                val executeFunctions = useCase.functions().filter { it.name == "execute" }

                when {
                    executeFunctions.size != 1 ->
                        "${useCase.name}: execute function count must be exactly 1 (actual=${executeFunctions.size})"
                    else -> {
                        val execute = executeFunctions.single()
                        val hasSingleUseCaseInParam =
                            execute.numParameters == 1 &&
                                execute.parameters
                                    .first()
                                    .type.name
                                    .endsWith("UseCaseIn") &&
                                execute.parameters
                                    .first()
                                    .type
                                    .residesInSourcePackage(spec.applicationDtoPackagePattern)
                        if (hasSingleUseCaseInParam) {
                            null
                        } else {
                            "${useCase.name}: execute must take exactly one ${spec.moduleName} application dto parameter ending with UseCaseIn"
                        }
                    }
                }
            }

        check(invalidUseCases.isEmpty()) {
            invalidUseCases.joinToString(
                prefix = "[FAIL] UseCase execute signature violations: ",
                separator = " | ",
            )
        }
    }

    @Test
    fun `application dto package does not use Spring annotations`() {
        if (!spec.enforceUseCaseDtoNoSpringAnnotations) {
            return
        }

        val dtoClasses =
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.resideInPackage(spec.applicationDtoPackagePattern) }

        dtoClasses.assertTrue(additionalMessage = "application.dto must not use Spring annotations") { dtoClass ->
            !dtoClass.hasAnySpringAnnotation()
        }
    }

    @Test
    fun `repository implementations implement matching repository interface`() {
        if (!spec.enforceRepositoryImplContract) {
            return
        }

        val repositoryImpls =
            Konsist
                .scopeFromProduction()
                .classes()
                .withNameEndingWith("RepositoryImpl")
                .filter { it.resideInPackage(spec.repositoryPackagePattern) }

        repositoryImpls.assertTrue(
            additionalMessage = "RepositoryImpl class must implement corresponding *Repository interface",
        ) { impl ->
            val expectedInterfaceName = impl.name.removeSuffix("Impl")
            impl.hasParentInterfaceWithName(expectedInterfaceName)
        }
    }

    @Test
    fun `controller endpoint returns follow module policy`() {
        if (spec.controllerReturnPolicy == ControllerReturnPolicy.DISABLED) {
            return
        }

        val controllers =
            Konsist
                .scopeFromProduction()
                .classes()
                .withAllAnnotationsOf(RestController::class)
                .filter { it.resideInPackage(spec.controllerPackagePattern) }

        val invalidEndpoints =
            controllers.flatMap { controller ->
                controller
                    .functions()
                    .filter { it.hasAnnotationWithName(CONTROLLER_MAPPING_ANNOTATION_NAMES) }
                    .filterNot { endpoint -> isAllowedControllerReturn(endpoint.returnType) }
                    .map { endpoint -> "${controller.name}.${endpoint.name}" }
            }

        check(invalidEndpoints.isEmpty()) {
            invalidEndpoints.joinToString(
                prefix = "[FAIL] Controller return type policy violations: ",
                separator = ", ",
            )
        }
    }

    @Test
    fun `util package keeps pure function style`() {
        if (!spec.enforceUtilPureFunctions) {
            return
        }

        val utilFiles =
            Konsist
                .scopeFromProduction()
                .files
                .filter { file ->
                    val packageName = file.packagee?.name ?: return@filter false
                    packageName == spec.utilPackageName || packageName.startsWith("${spec.utilPackageName}.")
                }

        utilFiles.assertTrue(additionalMessage = "util package should contain only top-level functions") { file ->
            file.classes().isEmpty() && file.interfaces().isEmpty() && file.objects().isEmpty()
        }

        utilFiles.assertTrue(additionalMessage = "util package must not declare mutable top-level state") { file ->
            file.properties().none { it.isVar }
        }

        utilFiles.assertTrue(additionalMessage = "util package must not use Spring annotations") { file ->
            file.functions().all { function -> !function.hasAnySpringAnnotation() }
        }

        utilFiles.assertTrue(additionalMessage = "util package imports should avoid I/O, randomness and wall-clock APIs") { file ->
            file.imports.none { importDeclaration ->
                spec.utilForbiddenImportPrefixes.any { forbiddenPrefix ->
                    importDeclaration.text.contains(forbiddenPrefix)
                }
            }
        }
    }

    private fun isAllowedControllerReturn(returnType: KoTypeDeclaration?): Boolean {
        if (returnType == null) {
            return false
        }

        val typeNames = returnType.allTypeNames()

        if (spec.allowStreamingControllerReturn && typeNames.any { it == "Flux" || it == "ServerSentEvent" }) {
            return true
        }

        if (spec.allowVoidControllerReturn && typeNames.any { it == "Void" || it == "Unit" }) {
            return true
        }

        val hasUseCaseOut = typeNames.any { it.endsWith("UseCaseOut") }
        if (hasUseCaseOut) {
            return true
        }

        return when (spec.controllerReturnPolicy) {
            ControllerReturnPolicy.DISABLED -> true
            ControllerReturnPolicy.USE_CASE_OUT_ONLY -> false
            ControllerReturnPolicy.USE_CASE_OUT_OR_DTO -> typeNames.any { it.endsWith("Dto") || it.endsWith("Response") }
        }
    }
}

private val CONTROLLER_MAPPING_ANNOTATION_NAMES =
    listOf(
        "RequestMapping",
        "GetMapping",
        "PostMapping",
        "PutMapping",
        "PatchMapping",
        "DeleteMapping",
    )

private fun constructorDependencyTypes(clazz: KoClassDeclaration) =
    clazz.primaryConstructor
        ?.parameters
        .orEmpty()
        .map { it.type }

private fun KoTypeDeclaration.residesInSourcePackage(packagePattern: String): Boolean =
    sourceDeclaration
        ?.asClassOrInterfaceOrObjectDeclaration()
        ?.resideInPackage(packagePattern) == true

private fun KoClassDeclaration.hasAnySpringAnnotation(): Boolean =
    annotations.any { annotation ->
        annotation.name.startsWith("org.springframework") ||
            annotation.text.contains("org.springframework") ||
            annotation.name in SPRING_ANNOTATION_SIMPLE_NAMES
    }

private fun KoFunctionDeclaration.hasAnySpringAnnotation(): Boolean =
    annotations.any { annotation ->
        annotation.name.startsWith("org.springframework") ||
            annotation.text.contains("org.springframework") ||
            annotation.name in SPRING_ANNOTATION_SIMPLE_NAMES
    }

private val SPRING_ANNOTATION_SIMPLE_NAMES =
    setOf(
        "Component",
        "Service",
        "Repository",
        "Controller",
        "RestController",
        "Configuration",
    )

private val OBJECT_FUNCTION_NAMES =
    setOf(
        "equals",
        "hashCode",
        "toString",
    )

private fun String.crmModuleToken(): String? {
    val prefix = "com.manage.crm."
    if (!startsWith(prefix)) {
        return null
    }
    return removePrefix(prefix).substringBefore(".").ifBlank { null }
}

private fun KoTypeDeclaration.allTypeNames(): Set<String> {
    val names = mutableSetOf<String>()
    names.add(name)
    sourceDeclaration?.let { names.add(it.name) }
    typeArguments.orEmpty().forEach { it.collectTypeArgumentNames(names) }
    return names
}

private fun KoTypeArgumentDeclaration.collectTypeArgumentNames(names: MutableSet<String>) {
    names.add(name)
    sourceDeclaration?.let { names.add(it.name) }
    typeArguments.orEmpty().forEach { it.collectTypeArgumentNames(names) }
}
