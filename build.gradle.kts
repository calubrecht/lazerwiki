plugins {
	java
	war
	jacoco
	checkstyle
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.github.jk1.dependency-license-report") version "2.5"
	id("com.github.spotbugs") version "6.5.8"
	id("com.diffplug.spotless") version "7.0.4"
    `maven-publish`
}

group = "us.calubrecht"
version = project.properties["version"]!!

java {
	sourceCompatibility = JavaVersion.VERSION_25
}

checkstyle {
	toolVersion = "10.21.4"
	configFile = file("config/checkstyle/checkstyle.xml")
}

tasks.checkstyleTest {
	configFile = file("config/checkstyle/checkstyle-test.xml")
}

spotless {
	java {
		googleJavaFormat("1.35.0")
	}
}

spotbugs {
	ignoreFailures.set(true)
	showStackTraces.set(true)
	showProgress.set(true)
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
	auxClassPaths = configurations.compileClasspath.get()
	reports.create("html") {
		required.set(true)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")

	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation ("org.springframework.boot:spring-boot-starter-security")
	implementation("org.apache.commons:commons-text:1.15.0")
	implementation("org.apache.commons:commons-compress:1.28.0")
	implementation("org.apache.commons:commons-lang3:3.20.0")
	implementation("commons-io:commons-io:2.18.0")
	implementation ("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("io.github.java-diff-utils:java-diff-utils:4.12")
	implementation("org.xerial:sqlite-jdbc:3.49.1.0")
	implementation("org.hibernate.orm:hibernate-community-dialects")
	implementation("org.hibernate.orm:hibernate-core:7.3.5.Final")

	runtimeOnly("org.springframework.boot:spring-boot-properties-migrator")
	implementation("mysql:mysql-connector-java:8.0.33")
	implementation("com.github.usefulness:webp-imageio:0.5.1")
	implementation("io.gdcc:sitemapgen4j:2.1.2")
	// Gradle will pull in an older version of this if not explicit.
	providedRuntime("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.24")
	providedRuntime("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.24")
	providedRuntime("org.jetbrains.kotlin:kotlin-stdlib-common:1.9.24")
	providedRuntime("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")

	testImplementation ("org.junit.jupiter:junit-jupiter")
	testImplementation("org.mockito:mockito-core")
	testImplementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
	testRuntimeOnly ("com.h2database:h2")

	// Needed for SpotBugs analysis — transitively referenced by Spring Security but not on classpath
	compileOnly("io.projectreactor:reactor-core")
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
	description = "Build API jar for Custom Macro plugins"
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
	description = "Build a plugin jar from locally defined macros"
    manifest {
		attributes("Implementation-Version" to archiveVersion)
	}
	group="build"
	archiveBaseName="localMacros"
	from(sourceSets.main.get().output).include("localMacros/**")
}

tasks.register<Jar>("macroApiSourceJar") {
    description = "Sources for Macro API Jar"
    manifest {
        attributes("Implementation-Version" to archiveVersion)
    }
    group="build"
    archiveBaseName="lazerwiki-macro-api"
    archiveClassifier = "sources"
    from(sourceSets.main.get().java).include("us/calubrecht/lazerwiki/macro/**","us/calubrecht/lazerwiki/plugin/**")
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
						    "us/calubrecht/lazerwiki/config/LazerwikiDataSourceConfiguration.class",
						    "us/calubrecht/lazerwiki/adminCommandLine/AdminCommandLine.class",
							"localMacros/*")
				}
			})
	)
	dependsOn(tasks.test) // tests are required to run before generating the report
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.getByName("macroApiJar"))
            artifact(tasks.getByName("macroApiSourceJar"))
            pom {
                artifactId = "lazerwiki-macro-api"
                name.set("Lazerwiki Macro API")
                description.set("A library to allow creations of macros to modify behavior of Lazerwiki")
                developers {
                    developer {
                        name.set("Chad Lubrecht")
                        email.set("chad.lubrecht@gmail.com")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            url = uri("https://repo.repsy.io/mvn/ca_lazerdwarf/default")
            credentials {
                username = if (hasProperty("repsyUser")) findProperty("repsyUser") as String else ""
				password = if (hasProperty("repsyPassword")) findProperty("repsyPassword") as String else ""
            }
        }
    }
}
        

