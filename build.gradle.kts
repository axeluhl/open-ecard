subprojects {
    group = "org.openecard"
    version = "2.1.12-SNAPSHOT"
}
plugins {
    id("com.github.ben-manes.versions") version "0.51.0"
}
allprojects {
    apply {
	plugin("com.github.ben-manes.versions")
    }
}
