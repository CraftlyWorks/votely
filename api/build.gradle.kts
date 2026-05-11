plugins {
    id("maven-publish")
    id("signing")
    id("com.gradleup.nmcp")
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "votely-api"
            from(components["java"])

            pom {
                name.set("votely-api")
                description.set("Votely API - shared Vote model and channel constants for Minecraft plugin integration.")
                url.set("https://github.com/CraftlyWorks/votely")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("CraftlyWorks")
                        name.set("CraftlyWorks")
                        email.set("contact@craftlyworks.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/CraftlyWorks/votely.git")
                    developerConnection.set("scm:git:ssh://github.com/CraftlyWorks/votely.git")
                    url.set("https://github.com/CraftlyWorks/votely")
                }
            }
        }
    }
}

signing {
    val signingKey = System.getenv("GPG_SIGNING_KEY")
    val signingPassword = System.getenv("GPG_SIGNING_PASSWORD")

    if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    } else {
        logger.warn("GPG key or password not set — artifacts will not be signed.")
    }
}

nmcp {
    publishAllPublicationsToCentralPortal {
        username.set(findProperty("centralUsername") as String)
        password.set(findProperty("centralPassword") as String)
    }
}
