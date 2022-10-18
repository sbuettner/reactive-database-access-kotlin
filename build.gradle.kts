import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.0.0-SNAPSHOT"
    id("io.spring.dependency-management") version "1.0.14.RELEASE"
    id("org.flywaydb.flyway") version "9.4.0"
    id("dev.monosoul.jooq-docker") version "1.3.11"
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.spring") version "1.7.20"
}

group = "sbuettner.demo"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.postgresql:r2dbc-postgresql:1.0.0.RC1")
    implementation("org.jooq:jooq:3.17.4")
    implementation(platform("io.arrow-kt:arrow-stack:1.1.3"))
    implementation("io.arrow-kt:arrow-core")
    implementation("io.arrow-kt:arrow-fx-coroutines")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.5.1")
    testImplementation("io.kotest.extensions:kotest-assertions-arrow:1.2.5")
    jooqCodegen("org.postgresql:postgresql:42.5.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xcontext-receivers")
        jvmTarget = "17"
    }
}

tasks.withType(dev.monosoul.jooq.GenerateJooqClassesTask::class) {
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
