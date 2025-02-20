import org.apache.tools.ant.taskdefs.condition.Os

import java.time.Instant
import java.util.Base64

plugins {
    id("sap.commerce.build") version("4.0.0")
    id("sap.commerce.build.ccv2") version("4.0.0")
}

val DEPENDENCY_FOLDER = "../dependencies"
repositories {
    flatDir { dirs(DEPENDENCY_FOLDER) }
    mavenCentral()
}

tasks.register<WriteProperties>("generateLocalProperties") {
    comment = "FILE WAS GENERATED AT " + Instant.now()
    outputFile = project.file("hybris/config/local.properties")
    property("hybris.optional.config.dir", project.file("hybris/config/local-config").absolutePath)
    doLast {
        mkdir(project.file("hybris/config/local-config/"))
    }
}

val symlinkConfigTask = tasks.register("symlinkConfig")
val hybrisConfig = file("hybris/config")
val localConfig = file("hybris/config/local-config")
val homeDirectory = file(project.gradle.gradleUserHomeDir.parent)
mapOf(
    "10-local.properties" to "cloud/common.properties",
    "20-local.properties" to "cloud/persona/development.properties",
    "50-local.properties" to "cloud/local-dev.properties",
).forEach{
    val link = it.key
    var path = file(hybrisConfig.absolutePath + "/" + it.value)
    if (!path.exists()) {
        path = file(homeDirectory.absolutePath + "/.sap-commerce/local-config/" + it.key)
    }
    
    if (path.exists()) {
        val symlinkTask = tasks.register<Exec>("symlink-${link}") {
            val relPath = path.relativeTo(localConfig)
            println("rel path: " + relPath)

            if (Os.isFamily(Os.FAMILY_UNIX)) {
                commandLine("sh", "-c", "ln -sfn ${relPath} ${link}")
            } else {
                // https://blogs.windows.com/windowsdeveloper/2016/12/02/symlinks-windows-10/
                val windowsPath = relPath.toString().replace("[/]".toRegex(), "\\")
                commandLine("cmd", "/c", """mklink "${link}" "${windowsPath}" """)
            }
            workingDir(localConfig)
            dependsOn("generateLocalProperties")
        }
        symlinkConfigTask.configure {
            dependsOn(symlinkTask)
        }
    } else {
        // Unlink if no longer existing
        val unlinkTask = tasks.register<Exec>("unlink-${link}") {
            if (Os.isFamily(Os.FAMILY_UNIX)) {
                commandLine("sh", "-c", "rm -f ${link}")
            } else {
                commandLine("cmd", "/c", "del /q ${link}")
            }
            workingDir(localConfig)
            dependsOn("generateLocalProperties")
        }
        symlinkConfigTask.configure {
            dependsOn(unlinkTask)
        }
    }
}

tasks.register<WriteProperties>("generateLocalDeveloperProperties") {
    dependsOn(symlinkConfigTask)
    comment = "my.properties - add your own local development configuration parameters here"
    outputFile = project.file("hybris/config/local-config/99-local.properties")
    onlyIf {
        !project.file("hybris/config/local-config/99-local.properties").exists()
    }
}

tasks.named("installManifestAddons") {
    mustRunAfter("generateLocalProperties")
}

tasks.register("setupLocalDevelopment") {
    group = "SAP Commerce"
    description = "Setup local development"
    dependsOn("bootstrapPlatform", "generateLocalDeveloperProperties", "installManifestAddons")
}
