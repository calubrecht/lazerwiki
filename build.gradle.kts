plugins {
	java
	antlr
	war
	jacoco
	id("org.springframework.boot") version "3.1.4"
	id("io.spring.dependency-management") version "1.1.3"
}

group = "us.calubrecht"
version = "0.0.1"

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
	implementation ("org.springframework.boot:spring-boot-starter-web")
	implementation("org.antlr:antlr4-runtime:4.13.1")
	implementation("org.apache.commons:commons-text:1.10.0")
	//implementation("javax.servlet:servlet-api:2.5")

	testImplementation("org.springframework.boot:spring-boot-starter-test")

	testImplementation ("org.junit.jupiter:junit-jupiter")
	testImplementation("org.mockito:mockito-core")
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
					exclude("us/calubrecht/userAdmin/repository", "us/calubrecht/userAdmin/*.class", "us/calubrecht/userAdmin/controller/UserController$*.class", "us/calubrecht/userAdmin/config/SessionListener.class")
				}
			})
	)
	dependsOn(tasks.test) // tests are required to run before generating the report
}