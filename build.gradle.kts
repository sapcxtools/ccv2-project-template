plugins {
    id("com.diffplug.spotless") version("7.0.2")
}

repositories {
    mavenCentral()
}

spotless {
    val importOrderConfigFile = project.file("core-customize/conventions/eclipse-formatter.importorder")
    val javaFormatterConfigFile = project.file("core-customize/conventions/eclipse-formatter-settings.xml")
    val frontendFormatterConfigFile = project.file("js-storefront/spartacus/.prettierrc")

    java {
        target("core-customize/hybris/bin/custom/project/**/*.java")
        targetExclude("core-customize/hybris/bin/custom/project/**/gensrc/**")
        importOrderFile(importOrderConfigFile)
        eclipse().configFile(javaFormatterConfigFile)
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("frontend") {
        target(
            "js-storefront/*/src/**/*.scss",
            "js-storefront/*/src/**/*.ts",
            "js-storefront/*/src/**/*.html"
        )
        prettier("2.5.1").configFile(frontendFormatterConfigFile)
    }
}
