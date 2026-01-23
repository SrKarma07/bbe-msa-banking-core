plugins {
	id("org.springframework.boot") version "3.3.2"
	id("io.spring.dependency-management") version "1.1.6"
	id("java")
    id("jacoco")
	id("org.openapi.generator") version "7.6.0"
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

repositories { mavenCentral() }

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")

	// R2DBC Postgres + JDBC driver (útil para tooling)
	runtimeOnly("org.postgresql:postgresql:42.7.4")
	//runtimeOnly("io.r2dbc:r2dbc-postgresql:1.0.5.RELEASE")
	runtimeOnly("org.postgresql:r2dbc-postgresql:1.0.5.RELEASE")

	// MapStruct + Lombok
	implementation("org.mapstruct:mapstruct:1.6.2")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.2")
	compileOnly("org.projectlombok:lombok:1.18.34")
	annotationProcessor("org.projectlombok:lombok:1.18.34")
	compileOnly("org.projectlombok:lombok-mapstruct-binding:0.2.0")
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

	// OpenAPI (ui opcional para swagger y anotaciones nullable)
	implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.6.0")
	implementation("org.openapitools:jackson-databind-nullable:0.2.6")

	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage")
	}
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.testcontainers:junit-jupiter:1.20.1")
	testImplementation("org.testcontainers:postgresql:1.20.1")
	testImplementation("org.testcontainers:r2dbc:1.20.1")
}

tasks.test { useJUnitPlatform()
	systemProperty("spring.profiles.active", "test")
}

openApiGenerate {
	generatorName.set("spring")
	inputSpec.set("$projectDir/src/main/resources/bbe-msa-bs-account-movement-openapi.yml")
	outputDir.set("$buildDir/generated")
	apiPackage.set("com.business.banking.services.infrastructure.input.adapter.rest.api")
	modelPackage.set("com.business.banking.services.infrastructure.input.adapter.rest.dto")
	configOptions.set(
		mapOf(
			"interfaceOnly" to "true",
			"useSpringBoot3" to "true",
			"useJakartaEe" to "true",
			"reactive" to "true",
			"dateLibrary" to "java8",
			"openApiNullable" to "false"
		)
	)
}

sourceSets {
	main {
		java.srcDirs("src/main/java", "$buildDir/generated/src/main/java")
	}
}

tasks.compileJava { dependsOn(tasks.openApiGenerate) }
tasks.clean { doFirst { delete("$buildDir/generated") } }

jacoco {
    toolVersion = "0.8.12"
}

val jacocoExcludes = listOf(
    "**/com/business/banking/services/infrastructure/input/adapter/rest/api/**",
    "**/com/business/banking/services/infrastructure/input/adapter/rest/dto/**",
    "**/*Application*",
    "**/generated/**"
)

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport, tasks.jacocoTestCoverageVerification)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)

        xml.outputLocation.set(layout.buildDirectory.file("jacoco/test.xml"))
        html.outputLocation.set(layout.buildDirectory.dir("jacoco/html"))
        csv.outputLocation.set(layout.buildDirectory.file("jacoco/test.csv"))
    }

    classDirectories.setFrom(
        files(classDirectories.files.map { file ->
            fileTree(file) {
                exclude(jacocoExcludes)
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)

    classDirectories.setFrom(
        files(classDirectories.files.map { file ->
            fileTree(file) {
                exclude(jacocoExcludes)
            }
        })
    )

    violationRules {
        rule {
            limit {
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

val integrationTest by sourceSets.creating {
    compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
    runtimeClasspath += output + compileClasspath
}

configurations[integrationTest.implementationConfigurationName].extendsFrom(configurations["testImplementation"])
configurations[integrationTest.runtimeOnlyConfigurationName].extendsFrom(configurations["testRuntimeOnly"])

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath

    useJUnitPlatform()

    systemProperty("spring.profiles.active", "it")

    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn("integrationTest")
}
