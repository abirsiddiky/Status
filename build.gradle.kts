// Top-level build file where you can add configuration options common to all sub-projects/modules.
val debugKeystore = file("${rootDir}/debug.keystore")
if (!debugKeystore.exists()) {
  val base64Keystore = file("${rootDir}/debug.keystore.base64")
  if (base64Keystore.exists()) {
    debugKeystore.writeBytes(java.util.Base64.getDecoder().decode(base64Keystore.readText().trim()))
  }
}

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}
