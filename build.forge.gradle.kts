import org.gradle.jvm.tasks.Jar

plugins {
    id("net.neoforged.moddev.legacyforge")
    id("dev.kikugie.postprocess.jsonlang")
    id("me.modmuss50.mod-publish-plugin")
    id("maven-publish")
}

val minecraft = stonecutter.current.version
val mcVersion = stonecutter.current.project.substringBeforeLast('-')

val modId = property("mod.id") as String
val refmap = "$modId.refmap.json"
val mixinConfigs = listOf("$modId.mixins.json", "$modId.forge.mixins.json")

tasks.named<ProcessResources>("processResources") {
    fun prop(name: String) = project.property(name) as String

    val props = HashMap<String, String>().apply {
        this["version"] = prop("mod.version") + "+" + prop("deps.minecraft")
        this["minecraft_version_range"] = prop("mod.mc_dep_forgelike")
        this["mod_id"] = prop("mod.id")
        this["pack_format"] = prop("mod.pack_format")
        this["mod_name"] = prop("mod.name")
        this["description"] = prop("mod.description")
        this["mod_author"] = prop("mod.author")
        this["credits"] = prop("mod.credits")
        this["license"] = prop("mod.license")
        this["forge_loader_version_range"] = prop("deps.forge_loader_version_range")
        this["forge_version"] = prop("deps.forge")
        this["java_version"] = prop("deps.java_version")
        this["yacl_version"] = prop("deps.yacl").substringBefore('+')
    }
    inputs.properties(props)

    filesMatching(listOf("pack.mcmeta", "META-INF/mods.toml", "*.mixins.json")) {
        expand(props)
    }

    val refmapEntry = "\"refmap\": \"$refmap\",\n  \"package\":"
    filesMatching(mixinConfigs) {
        filter { line -> line.replace("\"package\":", refmapEntry) }
    }
}

version = "${property("mod.version")}+${property("deps.minecraft")}-forge"
base.archivesName = modId

jsonlang {
    languageDirectories = listOf("assets/$modId/lang")
    prettyPrint = true
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "Xander Maven (YACL)"
        url = uri("https://maven.isxander.dev/releases")
        content {
            includeGroupAndSubgroups("dev.isxander")
            includeGroupAndSubgroups("org.quiltmc.parsers")
        }
    }
    maven {
        name = "Quilt Maven"
        url = uri("https://maven.quiltmc.org/repository/release/")
        content {
            includeGroupAndSubgroups("org.quiltmc.parsers")
        }
    }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content {
            includeGroupAndSubgroups("maven.modrinth")
        }
    }
    maven {
        name = "ParchmentMC"
        url = uri("https://maven.parchmentmc.org")
        content {
            includeGroupAndSubgroups("org.parchmentmc")
        }
    }
}

mixin {
    add(sourceSets["main"], refmap)
    mixinConfigs.forEach(::config)
}

legacyForge {
    version = "${property("deps.minecraft")}-${property("deps.forge")}"
    validateAccessTransformers = true

    parchment {
        minecraftVersion = property("deps.minecraft") as String
        mappingsVersion = property("deps.parchment") as String
    }

    runs {
        configureEach {
            systemProperty("kotlinx.coroutines.debug", "off")
        }
        register("client") {
            gameDirectory = file("run/")
            client()
        }
        register("server") {
            gameDirectory = file("run/")
            server()
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }
}

tasks {
    processResources {
        exclude("**/fabric.mod.json", "**/*.accesswidener", "**/neoforge.mods.toml", "**/*.fabric.mixins.json", "**/*.neoforge.mixins.json")
    }

    jar {
        dependsOn("postProcessMainResources")
        finalizedBy("reobfJar")
        manifest.attributes("MixinConfigs" to mixinConfigs.joinToString(","))
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(named<Jar>("reobfJar").map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }

    withType<JavaExec>().configureEach {
        jvmArgs("-Dkotlinx.coroutines.debug=off")
    }
}

dependencies {
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")

    // YACL
    modCompileOnly("dev.isxander:yet-another-config-lib:${property("deps.yacl")}")
    modRuntimeOnly("dev.isxander:yet-another-config-lib:${property("deps.yacl")}")

    // Mixin Constraints
    compileOnly("com.moulberry:mixinconstraints:${property("deps.mixin_constraints")}")
    val mixinConstraints = implementation("com.moulberry:mixinconstraints") {
        version {
            strictly("[${property("deps.mixin_constraints")},)")
            prefer(property("deps.mixin_constraints") as String)
        }
    }
    "jarJar"(mixinConstraints!!)

    // MixinExtras
    annotationProcessor("io.github.llamalad7:mixinextras-common:${property("deps.mixinextras")}")
    compileOnly("io.github.llamalad7:mixinextras-common:${property("deps.mixinextras")}")
    implementation("io.github.llamalad7:mixinextras-forge:${property("deps.mixinextras")}")
    "jarJar"("io.github.llamalad7:mixinextras-forge:${property("deps.mixinextras")}")
}

val javaVer = (property("deps.java_version") as String).toInt()
java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVer)
    sourceCompatibility = JavaVersion.toVersion(javaVer)
    targetCompatibility = JavaVersion.toVersion(javaVer)
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = javaVer
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = property("mod.group") as String
            artifactId = "$modId-forge"
            version = "${property("mod.version")}+${property("deps.minecraft")}"

            from(components["java"])
        }
    }
}

val additionalVersionsStr = findProperty("publish.additionalVersions") as String?
val additionalVersions: List<String> = additionalVersionsStr
    ?.split(",")
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() }
    ?: emptyList()

publishMods {
    file = tasks.named<Jar>("reobfJar").flatMap { it.archiveFile }
    additionalFiles.from(tasks.named<Jar>("sourcesJar").map { it.archiveFile.get() })

    type = STABLE
    displayName = "${property("mod.name")} Forge $mcVersion - ${property("mod.version")}"
    version = "${property("mod.version")}+${property("deps.minecraft")}-forge"
    changelog = provider { rootProject.file("CHANGELOG-LATEST.md").readText() }
    modLoaders.add("forge")

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = providers.environmentVariable("MODRINTH_TOKEN").orElse(providers.environmentVariable("MODRINTH_API_KEY"))
        minecraftVersions.add(property("deps.minecraft") as String)
        minecraftVersions.addAll(additionalVersions)
        optional("yacl")
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN").orElse(providers.environmentVariable("CURSEFORGE_API_KEY"))
        minecraftVersions.add(property("deps.minecraft") as String)
        minecraftVersions.addAll(additionalVersions)
        optional("yacl")
        client = false
        server = true
    }
}
