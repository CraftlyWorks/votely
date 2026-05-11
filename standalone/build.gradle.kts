plugins {
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":api"))
    implementation("com.craftlyworks:configra:1.0-RELEASE")
    implementation("com.google.code.gson:gson:2.11.0")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    shadowJar {
        archiveFileName.set("votely.jar")
        manifest {
            attributes["Main-Class"] = "com.craftlyworks.votely.VotelyApplication"
        }
        relocate("com.craftlyworks.configra", "com.craftlyworks.votely.internal.configra")
        relocate("io.netty", "com.craftlyworks.votely.internal.netty")
        relocate("com.google.gson", "com.craftlyworks.votely.internal.gson")
    }
    build {
        dependsOn(shadowJar)
    }
}