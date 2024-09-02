plugins {
	java
	antlr
	war
	jacoco
	id("org.springframework.boot") version "3.3.2"
	id("io.spring.dependency-management") version "1.1.3"
	id("com.github.jk1.dependency-license-report") version "2.5"
}

group = "us.calubrecht"
version = "0.1.9.44"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

java.sourceSets["main"].java {
	srcDir("build/generated-src/antlr/main/")
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")

	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation ("org.springframework.boot:spring-boot-starter-security")
	implementation("org.antlr:antlr4-runtime:4.13.1")
	implementation("org.apache.commons:commons-text:1.10.0")
	implementation("commons-io:commons-io:2.15.0")
	implementation ("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.jsoup:jsoup:1.16.2")
	implementation("io.github.java-diff-utils:java-diff-utils:4.12")


	providedRuntime("mysql:mysql-connector-java:8.0.33")
	implementation("com.github.usefulness:webp-imageio:0.5.1")
	// Gradle will pull in an older version of this if not explicit.
	providedRuntime("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.10")
	providedRuntime("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.10")
	providedRuntime("org.jetbrains.kotlin:kotlin-stdlib-common:1.9.10")
	providedRuntime("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")

	testImplementation ("org.junit.jupiter:junit-jupiter")
	testImplementation("org.mockito:mockito-core")
	testImplementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
	testRuntimeOnly ("com.h2database:h2")

	antlr("org.antlr:antlr4:4.13.1")
}

tasks.withType<Test> {
	useJUnitPlatform()
	finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}
tasks.processResources {
	filesMatching("application.properties") {
		expand (project.properties)
	}
}

tasks.war {
	manifest {
		attributes("Implementation-Version" to archiveVersion)
	}
	archiveClassifier.set("")
}

tasks.register<Jar>("macroApiJar") {
	manifest {
		attributes("Implementation-Version" to archiveVersion)
	}
	group="build"
	archiveBaseName="lazerwiki-macro-api"
	from(sourceSets.main.get().output).include("us/calubrecht/lazerwiki/macro/**","us/calubrecht/lazerwiki/plugin/**")
}

tasks.processResources {
	from ("src/main/java") {
		include("**/*.css")
	}
}

tasks.register<Jar>("localMacroJar") {
	manifest {
		attributes("Implementation-Version" to archiveVersion)
	}
	group="build"
	archiveBaseName="localMacros"
	from(sourceSets.main.get().output).include("localMacros/**")
}


tasks.build {
	dependsOn(tasks.getByName("macroApiJar"))
}

tasks.bootWar {
	enabled = false
}

tasks.jacocoTestReport {
	reports {
		xml.required.set(true)
	}
	classDirectories.setFrom(
			files(classDirectories.files.map {
				fileTree(it) {
					exclude("us/calubrecht/lazerwiki/LazerWikiApplication*.class",
							"us/calubrecht/lazerwiki/LazerWikiAuth*.class",
							"us/calubrecht/lazerwiki/WebSecurity*.class",
							"us/calubrecht/lazerwiki/ServletInitializer.class",
							"us/calubrecht/lazerwiki/model/*.class",
							"us/calubrecht/lazerwiki/requests/*.class",
							"us/calubrecht/lazerwiki/controller/GlobalControllerAdvice.class",
							"us/calubrecht/lazerwiki/repository/EntityManagerProxy.class",
							"us/calubrecht/lazerwiki/repository/SiteRepository.class",
							"us/calubrecht/lazerwiki/repository/UserRepository.class",
							"us/calubrecht/lazerwiki/repository/NamespaceRepository.class",
							"us/calubrecht/lazerwiki/repository/PageCacheRepository.class",
							"us/calubrecht/lazerwiki/repository/LinkRepository.class",
							"us/calubrecht/lazerwiki/repository/TagRepository.class",
							"us/calubrecht/lazerwiki/repository/MediaRecordRepository.class",
							"us/calubrecht/lazerwiki/service/IMarkupRenderer.class",
							"us/calubrecht/lazerwiki/service/parser/doku/*",
							"us/calubrecht/lazerwiki/plugin/WikiPlugin.class",
							"us/calubrecht/lazerwiki/macro/CustomMacro.class",
							"us/calubrecht/lazerwiki/util/ImageUtil.class",
							"us/calubrecht/lazerwiki/util/IOSupplier.class",
							"us/calubrecht/lazerwiki/examplePlugins/ClearFloats.class",
							"localMacros/*")
				}
			})
	)
	dependsOn(tasks.test) // tests are required to run before generating the report
}
