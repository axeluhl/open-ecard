description = "android-lib"

plugins {
	id("openecard.android-conventions")
}

android {
	namespace = "org.openecard.clients.android.lib"
}

dependencies {
//	implementation(libs.android)
//	api(libs.jaxb.api)
//	api(libs.jaxb.ws.api)
//	api(libs.slf4j.api)
//	api(project(":common"))
//	api(project(":ifd:ifd-common"))
	api(project(":clients:android-common"))
//	implementation(project(":wsdef:wsdef-client"))
//	implementation(project(":i18n"))
//	api(libs.bc.prov)
//	api(libs.bc.tls)
//	api(libs.httpcore)
//	api(libs.proxyvole)
//	api(project(":gui:graphics"))
//	api(libs.pdfbox)
//	api(libs.scio)

	testImplementation(libs.bundles.test.basics)
}

configurations.api {
    exclude(group= "com.google.code.findbugs", module= "jsr305")
    exclude(group= "com.google.code.findbugs", module= "annotations")
}
