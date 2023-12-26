description = "http"

plugins {
	id("openecard.lib-conventions")
}

dependencies {
//	api(libs.jaxb.api)
//	api(libs.jaxb.ws.api)
//	api(libs.slf4j.api)
	api(project(":addon"))
//	api(project(":wsdef:wsdef-common"))
//	api(project(":wsdef:wsdef-client"))
//	implementation(project(":i18n"))
//	api(libs.bc.prov)
//	api(libs.bc.tls)
//	api(libs.httpcore)

	testImplementation(libs.bundles.test.basics)
	testImplementation(project(":gui:swing"))
	testImplementation(project(":ifd:ifd-core"))
	testImplementation(project(":management"))
	testImplementation(project(":sal:tiny-sal"))
}

tasks.named("processResources", ProcessResources::class) {
	doLast {
		// build file listing of www files in resources dir, separated with :
		val files = destinationDir.resolve("www").walk()
			.filter { it.isFile }
			.map { it.relativeTo(destinationDir).path }
			.joinToString(":")
		destinationDir.resolve("www-files").writeText(files)
	}
}
