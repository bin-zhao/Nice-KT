import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    id("maven-publish")
}

val vertxVersion = "4.0.0"

dependencies {
    api(project(":subProjects:nice-tools"))
    api(project(":subProjects:nice-crypto"))

    // android gradle依赖：implementation 和compile的区别
    // 参考: https://www.jianshu.com/p/f34c179bc9d0 根据需要选择使用不同的依赖设定方式

    api("io.vertx:vertx-core:$vertxVersion")
    api("io.vertx:vertx-unit:$vertxVersion")
    api("io.vertx:vertx-zookeeper:$vertxVersion")
    api("io.vertx:vertx-service-discovery:$vertxVersion")
    api("io.vertx:vertx-circuit-breaker:$vertxVersion")
    api("io.vertx:vertx-redis-client:$vertxVersion")
    api("io.vertx:vertx-mongo-client:$vertxVersion")
    api("io.vertx:vertx-dropwizard-metrics:$vertxVersion")

    api("io.vertx:vertx-lang-kotlin:$vertxVersion") {
        this.exclude(group = "org.jetbrains.kotlin")
    }
    api("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion") {
        this.exclude(group = "org.jetbrains.kotlin")
    }

    // 参考: https://vertx.io/docs/vertx-core/kotlin/#_native_transports
    // 注意, 要保持版本号和vertx依赖的 netty 的版本号一致
    api(group = "io.netty", name = "netty-transport-native-epoll", version = "4.1.49.Final", classifier = "linux-x86_64")
    api(group = "io.netty", name = "netty-transport-native-kqueue", version = "4.1.49.Final", classifier = "osx-x86_64")

    api("com.github.ben-manes.caffeine:caffeine:2.8.8")
    api("org.apache.commons:commons-pool2:2.6.2")
    api("com.google.guava:guava:28.2-jre")
    api("org.kodein.di:kodein-di-generic-jvm:6.5.1")

    api("com.google.protobuf:protobuf-java:3.11.3")

    configurations.all {
        this.exclude(group = "org.slf4j", module = "slf4j-log4j12")
    }
}

tasks.register<Jar>("sourcesJar") {
    from(sourceSets.main.get().allSource)
    archiveClassifier.set("sources")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
        }
    }

    val userHome = System.getProperty("user.home")

    repositories {
        var myRepo =  "$userHome\\Documents\\u3dWorkspace\\NICE_Framework\\Nice-KT-Repository"
        System.getProperty("myRepo")?.apply {
            myRepo = this
        }
        maven {
            name = "myRepo"
            url = uri("file://$myRepo")
        }
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

