package eu.andrer757.verifier

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Fast in-memory verification of module dependency boundaries with no task outputs.")
abstract class VerifyArchitectureTask : DefaultTask() {

    @get:Input
    abstract val abortOnError: Property<Boolean>

    @get:Input
    abstract val modulePath: Property<String>

    @get:Input
    abstract val dependentProjectPaths: SetProperty<String>

    @get:Input
    abstract val externalDeps: SetProperty<String>

    init {
        description = "Verifies Clean Architecture module dependency rules."
        group = "verification"
    }

    @TaskAction
    fun verify() {
        val currentPath = modulePath.get()
        val violations = mutableListOf<String>()

        for (extDep in externalDeps.get()) {
            if (currentPath == ":core:model" && (extDep.startsWith("androidx.") || extDep.startsWith("com.google.android."))) {
                violations.add("Module ':core:model' cannot depend on Android framework library '$extDep'.")
            }
            if (currentPath == ":sdk-core" && (extDep.startsWith("com.google.dagger:hilt") || extDep.startsWith("androidx.hilt:"))) {
                violations.add("Headless engine ':sdk-core' cannot depend on Dagger/Hilt runtime '$extDep' (zero-Hilt public surface rule).")
            }
        }

        for (depPath in dependentProjectPaths.get()) {
            // App Clean Architecture Invariants
            if (currentPath == ":core:model" && depPath != currentPath) {
                violations.add("Module ':core:model' cannot depend on internal module '$depPath'. It must be innermost.")
            }
            if (currentPath.startsWith(":feature:")) {
                if (depPath.startsWith(":feature:") && depPath != currentPath) {
                    violations.add("Feature module '$currentPath' cannot depend on another feature module '$depPath'. Cross-feature communication must go through :app navigation or :core:model.")
                }
                if (depPath == ":core:database" || depPath == ":core:network" || depPath == ":core:datastore") {
                    violations.add("Feature module '$currentPath' cannot directly depend on storage layer '$depPath'. Access data through repository layer :core:data.")
                }
            }
            if (currentPath.startsWith(":core:") && currentPath != ":core:testing") {
                if (depPath.startsWith(":feature:")) {
                    violations.add("Core module '$currentPath' cannot depend on feature module '$depPath'.")
                }
                if (depPath == ":app") {
                    violations.add("Core module '$currentPath' cannot depend on ':app'.")
                }
            }

            // SDK Architecture Invariants
            if (currentPath.startsWith(":sdk-")) {
                if (depPath == ":sample") {
                    violations.add("SDK module '$currentPath' cannot depend on ':sample' application.")
                }
                if (currentPath == ":sdk-core" && (depPath == ":sdk-ui" || depPath == ":sdk-testing")) {
                    violations.add("Headless engine ':sdk-core' cannot depend on '$depPath'.")
                }
            }
        }

        if (violations.isNotEmpty()) {
            val message = buildString {
                appendLine("Clean Architecture violations found in $currentPath:")
                violations.forEach { appendLine("  - $it") }
            }
            if (abortOnError.getOrElse(false)) {
                throw GradleException(message)
            } else {
                logger.error(message)
            }
        } else {
            logger.lifecycle("Architecture verification passed for $currentPath.")
        }
    }
}
