// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

// Ensure debug.keystore exists from base64 if running in CI or external environments
val debugKeystore = file("${rootDir}/debug.keystore")
val base64Keystore = file("${rootDir}/debug.keystore.base64")
if (!debugKeystore.exists() && base64Keystore.exists()) {
  try {
    val decoded = java.util.Base64.getDecoder().decode(base64Keystore.readText().trim())
    debugKeystore.writeBytes(decoded)
  } catch (_: Exception) {}
}

