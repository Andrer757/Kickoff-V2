package pt.simdea.verifier

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

open class QualityVerifierExtension @Inject constructor(objects: ObjectFactory) {
    val enableDetekt: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val enableSpotless: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val enableLint: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val enableKover: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val abortOnError: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    val detektConfigFile: RegularFileProperty = objects.fileProperty()
    val lintConfigFile: RegularFileProperty = objects.fileProperty()
}
