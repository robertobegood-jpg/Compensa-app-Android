import java.security.KeyStore
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.roberto.compensa"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.roberto.compensa"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.4"
    }

    // Assinatura oficial. A chave NÃO fica no repositório:
    // - no GitHub Actions vem dos secrets KEYSTORE_BASE64 e KEYSTORE_PASSWORD;
    // - no computador, coloque o arquivo em app/assinatura/compensa.jks
    //   e a senha em keystore.properties (senha=...), ambos ignorados pelo git.
    // Sem a chave, o build usa a chave de debug padrão.
    val chave = file("assinatura/compensa.jks")
    val senhaChave: String? = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
        ?: rootProject.file("keystore.properties").takeIf { it.exists() }?.let { arq ->
            Properties().apply { arq.inputStream().use { load(it) } }.getProperty("senha")
        }
    val temAssinatura = chave.exists() && senhaChave != null
    // O apelido (alias) da chave é lido do próprio arquivo, então serve a mesma chave dos outros apps.
    val apelidoChave: String = System.getenv("KEYSTORE_ALIAS")?.takeIf { it.isNotBlank() }
        ?: if (temAssinatura) {
            runCatching {
                val ks = KeyStore.getInstance(KeyStore.getDefaultType())
                chave.inputStream().use { ks.load(it, senhaChave!!.toCharArray()) }
                ks.aliases().toList().first()
            }.getOrDefault("androiddebugkey")
        } else "androiddebugkey"

    signingConfigs {
        if (temAssinatura) {
            create("oficial") {
                storeFile = chave
                storePassword = senhaChave
                keyAlias = apelidoChave
                keyPassword = senhaChave
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (temAssinatura) signingConfig = signingConfigs.getByName("oficial")
        }
        debug {
            if (temAssinatura) signingConfig = signingConfigs.getByName("oficial")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Gravação de segurança
    val camerax = "1.4.1"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-video:$camerax")

    // Leitura do card pela imagem (roda no celular, modelo já incluso no app)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // QR Code do Pix (cafezinho)
    implementation("com.google.zxing:core:3.5.3")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
