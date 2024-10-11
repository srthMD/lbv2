import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jetbrains.kotlin.jvm")
    id("idea")
    id("application")
}

group = "ro.srth.lbv2"

version = "v1.0.1"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
    maven(url = "https://repo.spring.io/milestone")
}

application { mainClass.set("ro.srth.lbv2.Bot") }

dependencies {
    implementation("net.dv8tion:JDA:5.1.2") { exclude(module = "opus-java") }

    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("org.json:json:20240303")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // https://github.com/bramp/ffmpeg-cli-wrapper/issues/291
    implementation("net.bramp.ffmpeg:ffmpeg:0.8.0")

    // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
    implementation("club.minnced:jda-ktx:0.11.0-beta.20")
    implementation("com.github.minndevelopment:jda-reactor:1.6.0")

    compileOnly("io.projectreactor:reactor-core:3.6.7")
    testCompileOnly("io.projectreactor:reactor-test:3.6.7")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.3.0-M1")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin { jvmToolchain(21) }

val shadowJar by
tasks.getting(ShadowJar::class) {
    exclude("ro/srth/lbv2/JSONGenerator*")

    val jarName = project.name + "-" + version + "-all.jar"
    file("build/libs/start.bat").writeText("java -jar $jarName")
    file("build/libs/startRegister.bat").writeText("java -jar $jarName --register")
}

// internal use
task<Copy>("copyCommands") {
    from("build/libs/cmds")
    include("*.json")
    into("commands")
}

task<Copy>("prepareDeploy") {
    dependsOn("shadowJar")
    val jarName = project.name + "-" + version + "-all.jar"

    from("build/libs")
    include("cmds/**")
    exclude("cmds/randomquestion/*")
    include(jarName)
    include("private.json")
    include("start*.bat")
    include("token.txt")

    from(projectDir)
    include("README.MD")

    into("build/distributions/prepare")
}

task<Zip>("deploy") {
    dependsOn("prepareDeploy")
    includeEmptyDirs = true

    from("build/distributions/prepare")
    include("*")
    include("cmds/**")

    eachFile {
        if (file.name == "private.json") {
            file.writeText("{\n\n}")
        }

        if (file.name == "token.txt") {
            file.writeText("PUT YOUR DISCORD TOKEN HERE (DO NOT SHARE THIS FILE)")
        }
    }
}

