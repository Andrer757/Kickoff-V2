package eu.andrer757.kickoff.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import com.github.ajalt.clikt.core.ProgramResult

@Serializable
data class AddResult(
    val status: String,
    val message: String,
    val createdFiles: List<String> = emptyList(),
    val modifiedFiles: List<String> = emptyList()
)

class AddCommand : CliktCommand(name = "add") {
    override fun run() = Unit
}

class AddFeatureCommand : CliktCommand(name = "feature") {
    private val name by option("-n", "--name", help = "Feature name (e.g. settings, profile)").required()
    private val projectDir by option("-d", "--project-dir", help = "Project root directory").default("")
    private val json by option("--json", help = "Output result as structured JSON").flag(default = false)

    private val jsonSerializer = Json { prettyPrint = true }

    override fun run() {
        val root = if (projectDir.isNotBlank()) File(projectDir) else ProjectUtils.findProjectRoot()
        if (root == null || !root.exists()) {
            val res = AddResult("error", "Kickoff project root not found (settings.gradle.kts missing).")
            if (json) echo(jsonSerializer.encodeToString(res)) else echo(" Error: ${res.message}", err = true)
            throw ProgramResult(1)
        }

        if (ProjectUtils.isSdkProject(root)) {
            val res = AddResult("error", "Cannot add an app feature module to an SDK project. Use 'kickoff add module --type sdk' instead.")
            if (json) echo(jsonSerializer.encodeToString(res)) else echo(" Error: ${res.message}", err = true)
            throw ProgramResult(1)
        }

        if (!ProjectUtils.isValidIdentifier(name)) {
            val res = AddResult("error", "Invalid feature name '$name'. Must start with a letter and contain only letters, digits, underscores, or hyphens (and not be a Kotlin keyword).")
            if (json) echo(jsonSerializer.encodeToString(res)) else echo(" Error: ${res.message}", err = true)
            throw ProgramResult(1)
        }

        val packageName = ProjectUtils.detectPackageName(root) ?: "com.example.app"
        val packagePath = packageName.replace('.', File.separatorChar)

        val slug = name.lowercase().replace(Regex("[^a-z0-9_-]"), "-").trim('-')
        val pkgSegment = slug.replace("-", "").replace("_", "")
        val pascalCase = slug.split('-', '_').filter { it.isNotEmpty() }.joinToString("") { it.replaceFirstChar(Char::titlecase) }

        val featureDir = File(root, "feature/$slug")
        if (!ProjectUtils.isContainedIn(root, featureDir)) {
            val res = AddResult("error", "Invalid feature path: target escapes project root.")
            if (json) echo(jsonSerializer.encodeToString(res)) else echo(" Error: ${res.message}", err = true)
            throw ProgramResult(1)
        }
        if (featureDir.exists()) {
            val res = AddResult("error", "Feature directory 'feature/$slug' already exists.")
            if (json) echo(jsonSerializer.encodeToString(res)) else echo(" Error: ${res.message}", err = true)
            throw ProgramResult(1)
        }

        val createdFiles = mutableListOf<String>()
        val modifiedFiles = mutableListOf<String>()

        // 1. Create feature build.gradle.kts
        featureDir.mkdirs()
        val buildFile = File(featureDir, "build.gradle.kts")
        buildFile.writeText(
            """
            plugins {
                id("kickoff.android.feature")
                alias(libs.plugins.kotlin.serialization)
            }

            android {
                namespace = "$packageName.feature.$pkgSegment"
            }

            dependencies {
                implementation(libs.kotlinx.serialization.json)
            }
            """.trimIndent() + "\n"
        )
        createdFiles.add(buildFile.absolutePath)

        // 2. Create Screen, ViewModel, and Navigation Route
        val srcDir = File(featureDir, "src/main/kotlin/$packagePath/feature/$pkgSegment")
        srcDir.mkdirs()

        val navFile = File(srcDir, "${pascalCase}Route.kt")
        navFile.writeText(
            """
            package $packageName.feature.$pkgSegment

            import kotlinx.serialization.Serializable

            @Serializable
            data object ${pascalCase}Route
            """.trimIndent() + "\n"
        )
        createdFiles.add(navFile.absolutePath)

        val screenFile = File(srcDir, "${pascalCase}Screen.kt")
        screenFile.writeText(
            """
            package $packageName.feature.$pkgSegment

            import androidx.compose.foundation.layout.Arrangement
            import androidx.compose.foundation.layout.Column
            import androidx.compose.foundation.layout.fillMaxSize
            import androidx.compose.foundation.layout.padding
            import androidx.compose.material3.MaterialTheme
            import androidx.compose.material3.Scaffold
            import androidx.compose.material3.Text
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Alignment
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp
            import androidx.hilt.navigation.compose.hiltViewModel

            @Composable
            fun ${pascalCase}Route(
                modifier: Modifier = Modifier,
                viewModel: ${pascalCase}ViewModel = hiltViewModel(),
            ) {
                ${pascalCase}Screen(
                    name = viewModel.title,
                    modifier = modifier,
                )
            }

            @Composable
            fun ${pascalCase}Screen(
                name: String,
                modifier: Modifier = Modifier,
            ) {
                Scaffold(modifier = modifier) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "${'$'}name Screen",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                }
            }
            """.trimIndent() + "\n"
        )
        createdFiles.add(screenFile.absolutePath)

        val vmFile = File(srcDir, "${pascalCase}ViewModel.kt")
        vmFile.writeText(
            """
            package $packageName.feature.$pkgSegment

            import androidx.lifecycle.ViewModel
            import dagger.hilt.android.lifecycle.HiltViewModel
            import javax.inject.Inject

            @HiltViewModel
            class ${pascalCase}ViewModel @Inject constructor() : ViewModel() {
                val title: String = "$pascalCase"
            }
            """.trimIndent() + "\n"
        )
        createdFiles.add(vmFile.absolutePath)

        // 3. Create unit test
        val testDir = File(featureDir, "src/test/kotlin/$packagePath/feature/$pkgSegment")
        testDir.mkdirs()
        val testFile = File(testDir, "${pascalCase}ViewModelTest.kt")
        testFile.writeText(
            """
            package $packageName.feature.$pkgSegment

            import org.junit.jupiter.api.Assertions.assertNotNull
            import org.junit.jupiter.api.Test

            class ${pascalCase}ViewModelTest {

                @Test
                fun `viewmodel initializes successfully`() {
                    val viewModel = ${pascalCase}ViewModel()
                    assertNotNull(viewModel)
                }
            }
            """.trimIndent() + "\n"
        )
        createdFiles.add(testFile.absolutePath)

        // 4. Register in settings.gradle.kts
        val settingsFile = File(root, "settings.gradle.kts")
        if (settingsFile.exists()) {
            val settingsText = settingsFile.readText()
            val includeStatement = "include(\":feature:$slug\")"
            if (!settingsText.contains(includeStatement)) {
                settingsFile.appendText("\n$includeStatement\n")
                modifiedFiles.add(settingsFile.absolutePath)
            }
        }

        // 5. Wire into app/build.gradle.kts
        val appBuildFile = File(root, "app/build.gradle.kts")
        if (appBuildFile.exists()) {
            val appBuildText = appBuildFile.readText()
            val depLine = "    implementation(project(\":feature:$slug\"))"
            if (!appBuildText.contains(":feature:$slug")) {
                if (appBuildText.contains("dependencies {")) {
                    val updated = appBuildText.replaceFirst("dependencies {", "dependencies {\n$depLine")
                    appBuildFile.writeText(updated)
                    modifiedFiles.add(appBuildFile.absolutePath)
                }
            }
        }

        // 6. Wire route into MainActivity.kt NavHost
        val mainActivityFile = File(root, "app/src/main/kotlin/$packagePath/MainActivity.kt")
        if (mainActivityFile.exists()) {
            var activityText = mainActivityFile.readText()
            val importLine = "import $packageName.feature.$pkgSegment.${pascalCase}Route"
            if (!activityText.contains(importLine)) {
                val lines = activityText.split("\n").toMutableList()
                val firstImportIdx = lines.indexOfFirst { it.startsWith("import ") }
                if (firstImportIdx != -1) {
                    var endImportIdx = firstImportIdx
                    while (endImportIdx < lines.size && lines[endImportIdx].startsWith("import ")) {
                        endImportIdx++
                    }
                    val sortedImports = (lines.subList(firstImportIdx, endImportIdx) + importLine).sorted()
                    activityText = (lines.subList(0, firstImportIdx) + sortedImports + lines.subList(endImportIdx, lines.size)).joinToString("\n")
                } else {
                    activityText = activityText.replaceFirst(
                        "package $packageName",
                        "package $packageName\n\n$importLine"
                    )
                }
            }

            val composableBlock = "                    composable<${pascalCase}Route> {\n                        ${pascalCase}Route()\n                    }"

            if (!activityText.contains("<${pascalCase}Route>")) {
                val navHostIdx = activityText.indexOf("NavHost(")
                if (navHostIdx != -1) {
                    var depth = 0
                    var i = activityText.indexOf('{', navHostIdx)
                    var navHostCloseIdx = -1
                    while (i in 0 until activityText.length) {
                        when (activityText[i]) {
                            '{' -> depth++
                            '}' -> {
                                depth--
                                if (depth == 0) {
                                    navHostCloseIdx = i
                                    break
                                }
                            }
                        }
                        i++
                    }
                    if (navHostCloseIdx != -1) {
                        val beforeClose = activityText.substring(0, navHostCloseIdx).trimEnd()
                        val afterClose = activityText.substring(navHostCloseIdx)
                        activityText = "$beforeClose\n$composableBlock\n                $afterClose"
                        mainActivityFile.writeText(activityText)
                        modifiedFiles.add(mainActivityFile.absolutePath)
                    }
                }
            }
        }

        val result = AddResult(
            status = "success",
            message = "Feature ':feature:$slug' created and auto-wired successfully!",
            createdFiles = createdFiles,
            modifiedFiles = modifiedFiles
        )

        if (json) {
            echo(jsonSerializer.encodeToString(result))
        } else {
            echo(" Kickoff: Feature ':feature:$slug' added successfully!")
            echo(" Created files:")
            createdFiles.forEach { echo("   + $it") }
            echo(" Modified files:")
            modifiedFiles.forEach { echo("   * $it") }
        }
    }
}

class AddModuleCommand : CliktCommand(name = "module") {
    private val name by option("-n", "--name", help = "Module name").required()
    private val type by option("-t", "--type", help = "Module type: core, library, compose, sdk").default("core")
    private val projectDir by option("-d", "--project-dir", help = "Project root directory").default("")
    private val json by option("--json", help = "Output result as structured JSON").flag(default = false)

    private val jsonSerializer = Json { prettyPrint = true }

    override fun run() {
        val root = if (projectDir.isNotBlank()) File(projectDir) else ProjectUtils.findProjectRoot()
        if (root == null || !root.exists()) {
            val res = AddResult("error", "Kickoff project root not found (settings.gradle.kts missing).")
            if (json) echo(jsonSerializer.encodeToString(res)) else echo(" Error: ${res.message}", err = true)
            throw ProgramResult(1)
        }

        if (!ProjectUtils.isValidIdentifier(name)) {
            val res = AddResult("error", "Invalid module name '$name'. Must start with a letter and contain only letters, digits, underscores, or hyphens (and not be a Kotlin keyword).")
            if (json) echo(jsonSerializer.encodeToString(res)) else echo(" Error: ${res.message}", err = true)
            throw ProgramResult(1)
        }

        val isSdk = ProjectUtils.isSdkProject(root) || type.lowercase() == "sdk"
        val packageName = ProjectUtils.detectPackageName(root) ?: "com.example.app"
        val packagePath = packageName.replace('.', File.separatorChar)

        val slug = name.lowercase().replace(Regex("[^a-z0-9_-]"), "-").trim('-')
        val pkgSegment = slug.replace("-", "").replace("_", "")
        val pascalCase = slug.split('-', '_').filter { it.isNotEmpty() }.joinToString("") { it.replaceFirstChar(Char::titlecase) }

        val modulePath = if (isSdk) "sdk-$slug" else "core/$slug"
        val moduleGradlePath = if (isSdk) ":sdk-$slug" else ":core:$slug"

        val moduleDir = File(root, modulePath)
        if (!ProjectUtils.isContainedIn(root, moduleDir)) {
            val res = AddResult("error", "Invalid module path: target escapes project root.")
            if (json) echo(jsonSerializer.encodeToString(res)) else echo(" Error: ${res.message}", err = true)
            throw ProgramResult(1)
        }
        if (moduleDir.exists()) {
            val res = AddResult("error", "Module directory '$modulePath' already exists.")
            if (json) echo(jsonSerializer.encodeToString(res)) else echo(" Error: ${res.message}", err = true)
            throw ProgramResult(1)
        }

        val createdFiles = mutableListOf<String>()
        val modifiedFiles = mutableListOf<String>()

        moduleDir.mkdirs()

        // 1. build.gradle.kts
        val buildFile = File(moduleDir, "build.gradle.kts")
        val buildContent = if (isSdk) {
            """
            plugins {
                id("kickoff.sdk.library")
                id("kickoff.sdk.publish")
            }

            android {
                namespace = "$packageName.sdk.$pkgSegment"
            }

            dependencies {
                implementation(libs.androidx.core.ktx)
                testImplementation(libs.junit.jupiter)
            }
            """.trimIndent() + "\n"
        } else {
            val composePlugin = if (type.lowercase() == "compose") "    id(\"kickoff.android.compose\")\n" else ""
            """
            plugins {
                id("kickoff.android.library")
                id("kickoff.android.hilt")
            $composePlugin}

            android {
                namespace = "$packageName.core.$pkgSegment"
            }

            dependencies {
                implementation(project(":core:common"))
                testImplementation(libs.junit.jupiter)
            }
            """.trimIndent() + "\n"
        }
        buildFile.writeText(buildContent)
        createdFiles.add(buildFile.absolutePath)

        if (isSdk) {
            val rulesFile = File(moduleDir, "consumer-rules.pro")
            rulesFile.writeText(
                """
                -keep public class $packageName.sdk.$pkgSegment.** {
                    public protected *;
                }
                """.trimIndent() + "\n"
            )
            createdFiles.add(rulesFile.absolutePath)
        }

        // 2. Starter source class
        val pkgSub = if (isSdk) "sdk.$pkgSegment" else "core.$pkgSegment"
        val srcDir = File(moduleDir, "src/main/kotlin/$packagePath/${pkgSub.replace('.', File.separatorChar)}")
        srcDir.mkdirs()
        val classFile = File(srcDir, "${pascalCase}.kt")
        classFile.writeText(
            if (isSdk) {
                """
                package $packageName.$pkgSub

                public class $pascalCase(
                    public val name: String = "$pascalCase",
                )
                """.trimIndent() + "\n"
            } else {
                """
                package $packageName.$pkgSub

                class $pascalCase(
                    val name: String = "$pascalCase",
                )
                """.trimIndent() + "\n"
            }
        )
        createdFiles.add(classFile.absolutePath)

        if (isSdk) {
            val apiDir = File(moduleDir, "api")
            apiDir.mkdirs()
            val apiPkgPath = "$packagePath/${pkgSub.replace('.', '/')}"
            val apiFile = File(apiDir, "${moduleDir.name}.api")
            apiFile.writeText(
                """
                public final class $apiPkgPath/$pascalCase {
                	public fun <init> ()V
                	public fun <init> (Ljava/lang/String;)V
                	public synthetic fun <init> (Ljava/lang/String;ILkotlin/jvm/internal/DefaultConstructorMarker;)V
                	public final fun getName ()Ljava/lang/String;
                }
                """.trimIndent() + "\n\n"
            )
            createdFiles.add(apiFile.absolutePath)
        }

        // 3. Starter unit test
        val testDir = File(moduleDir, "src/test/kotlin/$packagePath/${pkgSub.replace('.', File.separatorChar)}")
        testDir.mkdirs()
        val testFile = File(testDir, "${pascalCase}Test.kt")
        testFile.writeText(
            """
            package $packageName.$pkgSub

            import org.junit.jupiter.api.Assertions.assertNotNull
            import org.junit.jupiter.api.Test

            class ${pascalCase}Test {

                @Test
                fun `instantiates successfully`() {
                    val item = $pascalCase()
                    assertNotNull(item)
                }
            }
            """.trimIndent() + "\n"
        )
        createdFiles.add(testFile.absolutePath)

        // 4. Register in settings.gradle.kts
        val settingsFile = File(root, "settings.gradle.kts")
        if (settingsFile.exists()) {
            val settingsText = settingsFile.readText()
            val includeLine = "include(\"$moduleGradlePath\")"
            if (!settingsText.contains(includeLine)) {
                settingsFile.appendText("\n$includeLine\n")
                modifiedFiles.add(settingsFile.absolutePath)
            }
        }

        val result = AddResult(
            status = "success",
            message = "Module '$moduleGradlePath' created and registered successfully!",
            createdFiles = createdFiles,
            modifiedFiles = modifiedFiles
        )

        if (json) {
            echo(jsonSerializer.encodeToString(result))
        } else {
            echo(" Kickoff: Module '$moduleGradlePath' created successfully!")
            createdFiles.forEach { echo("   + $it") }
            modifiedFiles.forEach { echo("   * $it") }
        }
    }
}

class AddEntityCommand : CliktCommand(name = "entity") {
    private val name by option("-n", "--name", help = "Entity name (e.g. Article, Product, Note)").required()
    private val fields by option("-f", "--fields", help = "Fields as name:Type (e.g. id:String,title:String)").default("id:String,title:String")
    private val projectDir by option("-d", "--project-dir", help = "Project root directory").default("")
    private val force by option("--force", help = "Overwrite existing entity files if present").flag(default = false)
    private val noMigration by option("--no-migration", help = "Skip incrementing Room @Database(version) and generating a Migration stub").flag(default = false)
    private val json by option("--json", help = "Output result as structured JSON").flag(default = false)

    private val jsonSerializer = Json { prettyPrint = true }

    override fun run() {
        val root = if (projectDir.isNotBlank()) File(projectDir) else ProjectUtils.findProjectRoot()
        if (root == null || !root.exists()) {
            val res = AddResult("error", "Kickoff project root not found (settings.gradle.kts missing).")
            if (json) echo(jsonSerializer.encodeToString(res)) else echo(" Error: ${res.message}", err = true)
            throw ProgramResult(1)
        }

        if (ProjectUtils.isSdkProject(root)) {
            val res = AddResult("error", "Cannot add a Room entity to an SDK project.")
            if (json) echo(jsonSerializer.encodeToString(res)) else echo(" Error: ${res.message}", err = true)
            throw ProgramResult(1)
        }

        if (!name.trim().matches(Regex("^[A-Za-z][A-Za-z0-9]*$")) || !ProjectUtils.isValidIdentifier(name)) {
            val res = AddResult("error", "Invalid entity name '$name'. Must be a valid alphanumeric Kotlin class identifier (e.g. Article, UserProfile).")
            if (json) echo(jsonSerializer.encodeToString(res)) else echo(" Error: ${res.message}", err = true)
            throw ProgramResult(1)
        }

        val packageName = ProjectUtils.detectPackageName(root) ?: "com.example.app"
        val packagePath = packageName.replace('.', File.separatorChar)

        val entityName = name.trim().replaceFirstChar(Char::titlecase)
        val entityLower = entityName.replaceFirstChar(Char::lowercase)
        val tableName = "${entityLower}s"

        val parsedFields = fields.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { fieldStr ->
            val parts = fieldStr.split(":")
            val fName = parts[0].trim()
            val fType = if (parts.size > 1) parts[1].trim() else "String"
            fName to fType
        }

        val supportedSqlTypes = mapOf(
            "String" to "TEXT",
            "Int" to "INTEGER",
            "Long" to "INTEGER",
            "Boolean" to "INTEGER",
            "Double" to "REAL",
            "Float" to "REAL",
        )
        val unsupportedField = parsedFields.firstOrNull { (_, fType) ->
            fType.removeSuffix("?") !in supportedSqlTypes
        }
        if (unsupportedField != null) {
            val res = AddResult(
                "error",
                "Unsupported field type '${unsupportedField.second}' for '${unsupportedField.first}'. Supported types: ${supportedSqlTypes.keys.joinToString(", ")}."
            )
            if (json) echo(jsonSerializer.encodeToString(res)) else echo(" Error: ${res.message}", err = true)
            throw ProgramResult(1)
        }

        val modelDir = File(root, "core/model/src/main/kotlin/$packagePath/core/model")
        val dbDir = File(root, "core/database/src/main/kotlin/$packagePath/core/database")
        val dataDir = File(root, "core/data/src/main/kotlin/$packagePath/core/data")

        val modelFile = File(modelDir, "$entityName.kt")
        val entityFile = File(dbDir, "${entityName}Entity.kt")
        val daoFile = File(dbDir, "${entityName}Dao.kt")
        val repoFile = File(dataDir, "${entityName}Repository.kt")
        val implFile = File(dataDir, "OfflineFirst${entityName}Repository.kt")

        val targetFiles = listOf(modelFile, entityFile, daoFile, repoFile, implFile)
        if (targetFiles.any { !ProjectUtils.isContainedIn(root, it) }) {
            val res = AddResult("error", "Invalid entity path: target escapes project root.")
            if (json) echo(jsonSerializer.encodeToString(res)) else echo(" Error: ${res.message}", err = true)
            throw ProgramResult(1)
        }

        val existing = targetFiles.filter { it.exists() }
        if (existing.isNotEmpty() && !force) {
            val res = AddResult("error", "Entity '$entityName' already exists (${existing.first().name}). Pass --force to overwrite.")
            if (json) echo(jsonSerializer.encodeToString(res)) else echo(" Error: ${res.message}", err = true)
            throw ProgramResult(1)
        }

        val createdFiles = mutableListOf<String>()
        val modifiedFiles = mutableListOf<String>()

        // 1. Domain Model in :core:model
        if (modelDir.exists()) {
            val fieldsKotlin = parsedFields.joinToString(",\n    ", postfix = ",") { (fName, fType) -> "val $fName: $fType" }
            modelFile.writeText(
                buildString {
                    appendLine("package $packageName.core.model")
                    appendLine()
                    appendLine("import kotlinx.serialization.Serializable")
                    appendLine()
                    appendLine("@Serializable")
                    appendLine("data class $entityName(")
                    appendLine("    $fieldsKotlin")
                    appendLine(")")
                }
            )
            createdFiles.add(modelFile.absolutePath)
        }

        // 2. Room Entity & DAO in :core:database
        if (dbDir.exists()) {
            val entityFields = parsedFields.mapIndexed { idx, (fName, fType) ->
                if (idx == 0) "@PrimaryKey val $fName: $fType" else "val $fName: $fType"
            }.joinToString(",\n    ", postfix = ",")

            val toDomainMapping = parsedFields.joinToString(",\n    ", postfix = ",") { (fName, _) -> "$fName = $fName" }
            val toEntityMapping = parsedFields.joinToString(",\n    ", postfix = ",") { (fName, _) -> "$fName = $fName" }

            entityFile.writeText(
                buildString {
                    appendLine("package $packageName.core.database")
                    appendLine()
                    appendLine("import androidx.room.Entity")
                    appendLine("import androidx.room.PrimaryKey")
                    appendLine("import $packageName.core.model.$entityName")
                    appendLine()
                    appendLine("@Entity(tableName = \"$tableName\")")
                    appendLine("data class ${entityName}Entity(")
                    appendLine("    $entityFields")
                    appendLine(")")
                    appendLine()
                    appendLine("fun ${entityName}Entity.asExternalModel(): $entityName = $entityName(")
                    appendLine("    $toDomainMapping")
                    appendLine(")")
                    appendLine()
                    appendLine("fun $entityName.asEntity(): ${entityName}Entity = ${entityName}Entity(")
                    appendLine("    $toEntityMapping")
                    appendLine(")")
                }
            )
            createdFiles.add(entityFile.absolutePath)

            daoFile.writeText(
                """
                package $packageName.core.database

                import androidx.room.Dao
                import androidx.room.Insert
                import androidx.room.OnConflictStrategy
                import androidx.room.Query
                import kotlinx.coroutines.flow.Flow

                @Dao
                interface ${entityName}Dao {
                    @Query("SELECT * FROM $tableName")
                    fun get${entityName}s(): Flow<List<${entityName}Entity>>

                    @Insert(onConflict = OnConflictStrategy.REPLACE)
                    suspend fun insert${entityName}s(items: List<${entityName}Entity>)
                }
                """.trimIndent() + "\n"
            )
            createdFiles.add(daoFile.absolutePath)

            // Auto-wire AppDatabase.kt & DatabaseModule
            val appDbFile = File(dbDir, "AppDatabase.kt")
            if (appDbFile.exists()) {
                var dbText = appDbFile.readText()
                val isNewEntity = !dbText.contains("${entityName}Entity::class")
                if (isNewEntity) {
                    dbText = Regex("""entities\s*=\s*\[([^\]]*)\]""").replace(dbText) { matchResult ->
                        val existingEntities = matchResult.groupValues[1]
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        val updatedEntities = (existingEntities + "${entityName}Entity::class").joinToString(", ")
                        "entities = [$updatedEntities]"
                    }
                }
                if (!dbText.contains("fun ${entityLower}Dao()")) {
                    dbText = dbText.replaceFirst(
                        "abstract class AppDatabase : RoomDatabase() {",
                        "abstract class AppDatabase : RoomDatabase() {\n    abstract fun ${entityLower}Dao(): ${entityName}Dao"
                    )
                }
                if (!noMigration && isNewEntity) {
                    val versionRegex = Regex("""(@Database\([^\)]*\bversion\s*=\s*)(\d+)""")
                    val versionMatch = versionRegex.find(dbText)
                    if (versionMatch == null) {
                        val res = AddResult("error", "Could not parse @Database(version = ...) in AppDatabase.kt.")
                        if (json) echo(jsonSerializer.encodeToString(res)) else echo(" Error: ${res.message}", err = true)
                        throw ProgramResult(1)
                    }
                    val currentVersion = versionMatch.groupValues[2].toInt()
                    val nextVersion = currentVersion + 1
                    val migrationName = "MIGRATION_${currentVersion}_${nextVersion}"
                    dbText = dbText.replaceRange(versionMatch.groups[2]!!.range, nextVersion.toString())

                    if (!dbText.contains("import androidx.room.migration.Migration")) {
                        dbText = dbText.replaceFirst(
                            "import androidx.room.RoomDatabase",
                            "import androidx.room.RoomDatabase\nimport androidx.room.migration.Migration"
                        )
                    }
                    if (!dbText.contains("import androidx.sqlite.db.SupportSQLiteDatabase")) {
                        dbText = dbText.replaceFirst(
                            "import androidx.room.migration.Migration",
                            "import androidx.room.migration.Migration\nimport androidx.sqlite.db.SupportSQLiteDatabase"
                        )
                    }

                    val columnDefs = parsedFields.mapIndexed { idx, (fName, fType) ->
                        val sqlType = supportedSqlTypes.getValue(fType.removeSuffix("?"))
                        val nullability = if (fType.endsWith("?")) "" else " NOT NULL"
                        val pkClause = if (idx == 0) " PRIMARY KEY" else ""
                        "`$fName` $sqlType$nullability$pkClause"
                    }.joinToString(", ")

                    if (!dbText.contains("val $migrationName")) {
                        val migrationBlock = """
val $migrationName = object : Migration($currentVersion, $nextVersion) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `$tableName` ($columnDefs)")
    }
}

@Module"""
                        dbText = dbText.replaceFirst("@Module", migrationBlock.trimStart())
                    }

                    val existingMigrationsRegex = Regex("""\.addMigrations\(([^)]*)\)""")
                    val existingMigMatch = existingMigrationsRegex.find(dbText)
                    dbText = if (existingMigMatch != null) {
                        val current = existingMigMatch.groupValues[1].trim()
                        if (current.contains(migrationName)) {
                            dbText
                        } else {
                            dbText.replaceRange(existingMigMatch.range, ".addMigrations($current, $migrationName)")
                        }
                    } else {
                        dbText.replaceFirst(").build()", ").addMigrations($migrationName).build()")
                    }
                }
                if (!dbText.contains("fun provides${entityName}Dao")) {
                    val lastBrace = dbText.lastIndexOf('}')
                    if (lastBrace != -1) {
                        val providerBlock = """

    @Provides
    fun provides${entityName}Dao(database: AppDatabase): ${entityName}Dao = database.${entityLower}Dao()
}"""
                        dbText = dbText.substring(0, lastBrace).trimEnd() + providerBlock + "\n"
                    }
                }
                appDbFile.writeText(dbText)
                modifiedFiles.add(appDbFile.absolutePath)
            }
        }

        // 3. Repository in :core:data
        if (dataDir.exists()) {
            repoFile.writeText(
                """
                package $packageName.core.data

                import $packageName.core.model.$entityName
                import kotlinx.coroutines.flow.Flow

                interface ${entityName}Repository {
                    val ${entityLower}s: Flow<List<$entityName>>
                    suspend fun upsert${entityName}s(items: List<$entityName>)
                }
                """.trimIndent() + "\n"
            )
            createdFiles.add(repoFile.absolutePath)

            implFile.writeText(
                """
                package $packageName.core.data

                import $packageName.core.database.${entityName}Dao
                import $packageName.core.database.asEntity
                import $packageName.core.database.asExternalModel
                import $packageName.core.model.$entityName
                import kotlinx.coroutines.flow.Flow
                import kotlinx.coroutines.flow.map
                import javax.inject.Inject
                import javax.inject.Singleton

                @Singleton
                class OfflineFirst${entityName}Repository @Inject constructor(
                    private val dao: ${entityName}Dao,
                ) : ${entityName}Repository {

                    override val ${entityLower}s: Flow<List<$entityName>> = dao.get${entityName}s().map { list ->
                        list.map { it.asExternalModel() }
                    }

                    override suspend fun upsert${entityName}s(items: List<$entityName>) {
                        dao.insert${entityName}s(items.map { it.asEntity() })
                    }
                }
                """.trimIndent() + "\n"
            )
            createdFiles.add(implFile.absolutePath)

            // Auto-wire DataModule.kt
            val dataModuleFile = File(dataDir, "DataModule.kt")
            if (dataModuleFile.exists()) {
                var dmText = dataModuleFile.readText()
                if (!dmText.contains("binds${entityName}Repository")) {
                    val lastBrace = dmText.lastIndexOf('}')
                    if (lastBrace != -1) {
                        val bindBlock = """

    @Binds
    @Singleton
    abstract fun binds${entityName}Repository(
        repository: OfflineFirst${entityName}Repository,
    ): ${entityName}Repository
}"""
                        dmText = dmText.substring(0, lastBrace).trimEnd() + bindBlock + "\n"
                        dataModuleFile.writeText(dmText)
                        modifiedFiles.add(dataModuleFile.absolutePath)
                    }
                }
            }
        }

        val result = AddResult(
            status = "success",
            message = "Entity '$entityName' generated across model, database, and data layers!",
            createdFiles = createdFiles,
            modifiedFiles = modifiedFiles
        )

        if (json) {
            echo(jsonSerializer.encodeToString(result))
        } else {
            echo(" Kickoff: Entity '$entityName' scaffolded successfully!")
            createdFiles.forEach { echo("   + $it") }
        }
    }
}
