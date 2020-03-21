/*
 * Copyright 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("com.android.application")
}

apply(from = "../shortcutsGenerator.gradle")

val fileBuilder: groovy.lang.Closure<Any?> by extra

object TWAManifest {
    const val applicationId = "app.ryss.android.stockbroud"
    const val hostName = "staging.ryss.app" // The domain being opened in the TWA.
    const val launchUrl = "/" // The start path for the TWA. Must be relative to the domain.
    const val name = "Ryss" // The name shown on the Android Launcher.
    const val themeColor = "#70d156" // The color used for the status bar.
    const val navigationColor = "#469c30" // The color used for the navigation bar.
    const val backgroundColor = "#afe4a0" // The color used for the splash screen background.
    const val enableNotifications = false// Set to true to enable notification delegation.

    // Add shortcuts for your app here. Every shortcut must include the following fields:
    // - name=String that will show up in the shortcut.
    // - short_name=Shorter string used if |name| is too long.
    // - url=Absolute path of the URL to launch the app with (e.g "/create").
    // - icon=Name of the resource in the drawable folder to use as an icon.
    val shortcuts = arrayOf<ShortCut>()

    // The duration of fade out animation in milliseconds to be played when removing splash screen.
    const val splashScreenFadeOutDuration = 300

    class ShortCut(val name: String?, val shortName: String?, val url: String?, val icon: String?)
}

android {
    compileSdkVersion(29)
    defaultConfig {
        applicationId = TWAManifest.applicationId
        minSdkVersion(23)
        targetSdkVersion(29)
        versionCode = 2
        versionName = "0.0.2"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"

        // The name for the application on the Android Launcher
        resValue("string", "appName", TWAManifest.name)

        // The URL that will be used when launching the TWA from the Android Launcher
        val launchUrl = "https://${TWAManifest.hostName}${TWAManifest.launchUrl}"
        resValue("string", "launchUrl", launchUrl)

        // The URL that will be opened as a Desktop PWA when the TWA is installed and
        // run on ChromeOS. This will probably give a better user experience for non-mobile
        // devices, but will not include any native Android interaction.
        resValue("string", "crosLaunchUrl", launchUrl)

        // The hostname is used when building the intent-filter, so the TWA is able to
        // handle Intents to open https://svgomg.firebaseapp.com.
        resValue("string", "hostName", TWAManifest.hostName)

        // This variable below expresses the relationship between the app and the site,
        // as documented in the TWA documentation at
        // https://developers.google.com/web/updates/2017/10/using-twa#set_up_digital_asset_links_in_an_android_app
        // and is injected into the AndroidManifest.xml
        resValue("string", "assetStatements", """
            [{
                "relation"=["delegate_permission/common.handle_all_urls"],
                "target"={
                    "namespace"="web",
                    "site"="https://${TWAManifest.hostName}"
                }
            }]""")

        // This attribute sets the status bar color for the TWA. It can be either set here or in
        // `res/values/colors.xml`. Setting in both places is an error and the app will not
        // compile. If not set, the status bar color defaults to #FFFFFF - white.
        resValue("color", "colorPrimary", TWAManifest.themeColor)

        // This attribute sets the navigation bar color for the TWA. It can be either set here or in
        // `res/values/colors.xml`. Setting in both places is an error and the app will not
        // compile. If not set, the navigation bar color defaults to #FFFFFF - white.
        resValue("color", "navigationColor", TWAManifest.navigationColor)

        // Sets the color for the background used for the splash screen when launching the
        // Trusted Web Activity.
        resValue("color", "backgroundColor", TWAManifest.backgroundColor)

        // Defines a provider authority fot the Splash Screen
        resValue("string", "providerAuthority", TWAManifest.applicationId + ".fileprovider")

        // The enableNotification resource is used to enable or disable the
        // TrustedWebActivityService, by changing the android:enabled and android:exported
        // attributes
        resValue("bool", "enableNotification", TWAManifest.enableNotifications.toString())

        TWAManifest.shortcuts.forEachIndexed { index, shortcut ->
            resValue("string", "shortcut_name_$index", "$shortcut.name")
            resValue("string", "shortcut_short_name_$index", "$shortcut.short_name")
        }

        // The splashScreenFadeOutDuration resource is used to set the duration of fade out animation in milliseconds
        // to be played when removing splash screen. The default is 0 (no animation).
        resValue("integer", "splashScreenFadeOutDuration", TWAManifest.splashScreenFadeOutDuration.toString())
    }
    buildTypes {
        get("release").apply {
            isMinifyEnabled = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks {
    val generateShortcutsFile = task("generateShorcutsFile") {
        require(TWAManifest.shortcuts.size < 5) { "You can have at most 4 shortcuts." }
        TWAManifest.shortcuts.forEachIndexed { i, s ->
            require(s.name != null) { "Missing `name` in shortcut #" + i }
            require(s.shortName != null) { "Missing `short_name` in shortcut #" + i }
            require(s.url != null) { "Missing `icon` in shortcut #" + i }
            require(s.icon != null) { "Missing `url` in shortcut #" + i }
        }

        val shortcutsFile = File("$projectDir/src/main/res/xml", "shortcuts.xml")

        // This stupid groovy.xml.MarkupBuilder is unusable in java/kotlin so I have to use interop
        fileBuilder(shortcutsFile, TWAManifest)
    }

    preBuild {
        dependsOn(generateShortcutsFile)
    }
}

//task generateShorcutsFile {
//    assert TWAManifest.shortcuts.size() < 5, "You can have at most 4 shortcuts."
//    TWAManifest.shortcuts.eachWithIndex { s, i ->
//        assert s.name != null, "Missing `name` in shortcut #" + i
//        assert s.short_name != null, "Missing `short_name` in shortcut #" + i
//        assert s.url != null, "Missing `icon` in shortcut #" + i
//        assert s.icon != null, "Missing `url` in shortcut #" + i
//    }
//
//    def shortcutsFile = new File("$projectDir/src/main/res/xml", "shortcuts.xml")
//
//    def xmlWriter = new StringWriter()
//    def xmlMarkup = new MarkupBuilder(new IndentPrinter(xmlWriter, "    ", true))
//
//    xmlMarkup
//        ."shortcuts"("xmlns:android"="http://schemas.android.com/apk/res/android") {
//            TWAManifest.shortcuts.eachWithIndex { s, i ->
//                "shortcut"(
//                        "android:shortcutId"="shortcut" + i,
//                        "android:enabled"="true",
//                        "android:icon"="@drawable/" + s.icon,
//                        "android:shortcutShortLabel"="@string/shortcut_short_name_" + i,
//                        "android:shortcutLongLabel"="@string/shortcut_name_" + i) {
//                    "intent"(
//                            "android:action"="android.intent.action.MAIN",
//                            "android:targetPackage"=TWAManifest.applicationId,
//                            "android:targetClass"="com.google.androidbrowserhelper.trusted.LauncherActivity",
//                            "android:data"="https://" + TWAManifest.hostName + s.url)
//                    "categories"("android:name"="android.intent.category.LAUNCHER")
//                }
//            }
//        }
//    shortcutsFile.text = xmlWriter.toString() + "\n"
//}

//preBuild.dependsOn(generateShorcutsFile)

dependencies {
    implementation(files("libs/"))
    implementation("com.google.androidbrowserhelper", "androidbrowserhelper", "1.2.0")
}

