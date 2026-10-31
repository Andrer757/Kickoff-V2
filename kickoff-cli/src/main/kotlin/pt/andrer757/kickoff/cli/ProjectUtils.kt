package pt.andrer757.kickoff.cli

import java.io.File

object ProjectUtils {

    fun findProjectRoot(dir: File = File(System.getProperty("user.dir"))): File? {
        var current: File? = dir.absoluteFile
        while (current != null) {
            if (File(current, "settings.gradle.kts").exists()) {
                return current
            }
            current = current.parentFile
        }
        return null
    }

    fun detectPackageName(projectRoot: File): String? {
        val appBuild = File(projectRoot, "app/build.gradle.kts")
        if (appBuild.exists()) {
            val content = appBuild.readText()
            val match = Regex("""namespace\s*=\s*"([^"]+)"""").find(content)
            if (match != null) {
                return match.groupValues[1]
            }
        }

        val sampleBuild = File(projectRoot, "sample/build.gradle.kts")
        if (sampleBuild.exists()) {
            val content = sampleBuild.readText()
            val match = Regex("""namespace\s*=\s*"([^"]+)\.sample"""").find(content)
            if (match != null) {
                return match.groupValues[1]
            }
        }

        val coreModel = File(projectRoot, "core/model/build.gradle.kts")
        if (coreModel.exists()) {
            val content = coreModel.readText()
            val match = Regex("""namespace\s*=\s*"([^"]+)\.core\.model"""").find(content)
            if (match != null) {
                return match.groupValues[1]
            }
        }

        return null
    }

    fun isSdkProject(projectRoot: File): Boolean {
        val settings = File(projectRoot, "settings.gradle.kts")
        if (settings.exists()) {
            val text = settings.readText()
            return text.contains(":sdk-core") || text.contains("sdk-core")
        }
        return false
    }
}
