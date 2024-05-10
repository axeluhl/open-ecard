plugins {
	kotlin("multiplatform")
	id("openecard.android-conventions")
}

val javaToolchain: String by project
kotlin {
	jvmToolchain {
		languageVersion = JavaLanguageVersion.of(javaToolchain)
	}

	applyDefaultHierarchyTemplate()

	androidTarget {  }

	jvm {  }
}

val testHeapSize: String by project
tasks.withType<Test> {
	maxHeapSize = testHeapSize
	useTestNG()
}

// set encoding for legacy java tasks, remove once all java is migrated to kotlin
tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
}
tasks.withType<Javadoc> {
	options.encoding = "UTF-8"
}
