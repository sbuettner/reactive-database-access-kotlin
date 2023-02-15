import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.0.2"
    id("io.spring.dependency-management") version "1.1.0"
    id("org.flywaydb.flyway") version "9.8.1"
    id("dev.monosoul.jooq-docker") version "3.0.7"
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.spring") version "1.8.10"
}

group = "sbuettner.demo"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("io.r2dbc:r2dbc-pool:1.0.0.RELEASE")
    implementation("org.postgresql:r2dbc-postgresql:1.0.0.RELEASE")

    val jooqVersion = "3.17.8"
    implementation("org.jooq:jooq:$jooqVersion")
    implementation("org.jooq:jooq-kotlin:$jooqVersion")
    implementation("org.jooq:jooq-kotlin-coroutines:$jooqVersion")

    implementation("io.arrow-kt:arrow-core:1.1.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.5.5")
    testImplementation("io.kotest.extensions:kotest-assertions-arrow:1.3.0")

    jooqCodegen("org.postgresql:postgresql:42.5.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xcontext-receivers")
        jvmTarget = "17"
    }
}

tasks.withType(dev.monosoul.jooq.GenerateJooqClassesTask::class) {
    usingJavaConfig {
        this.name = "org.jooq.codegen.KotlinGenerator"
    }
    basePackageName.set("sbuettner.demo.db")
}

flyway {
    configurations = arrayOf("jooqCodegen")
    url = "jdbc:postgresql://localhost:5432/postgres"
    user = "postgres"
    password = "postgres"
    baselineOnMigrate = true
}

tasks.withType<Test> {
    useJUnitPlatform()
}
