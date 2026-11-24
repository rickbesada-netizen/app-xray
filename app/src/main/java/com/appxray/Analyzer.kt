package com.appxray

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.DateFormat
import java.util.Date
import java.util.zip.ZipFile

data class Perm(val name: String, val label: String, val sensitive: Boolean)

data class Report(
    val label: String,
    val pkg: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val firstInstall: Long,
    val lastUpdate: Long,
    val installer: String?,
    val apkFiles: List<String>,
    val totalBytes: Long,
    val frameworks: List<String>,
    val sdks: Map<String, List<String>>,
    val obfuscation: String,
    val appPackages: List<Pair<String, Int>>,
    val classCount: Int,
    val dexCount: Int,
    val permissions: List<Perm>,
    val launcher: String?,
    val activities: List<String>,
    val services: List<String>,
    val receivers: List<String>,
    val providers: List<String>,
    val nativeLibs: List<String>,
    val abis: List<String>,
    val domains: List<Pair<String, Int>>,
    val urls: List<String>,
    val endpoints: List<String>,
    val firebase: Map<String, String>,
    val deepLinks: List<String>,
) {
    /** Plain-text report. Section headings start with "## ". */
    fun toText(): String = buildString {
        val df = DateFormat.getDateInstance(DateFormat.MEDIUM)
        appendLine("App X-Ray report: $label")
        appendLine()
        appendLine("## Basics")
        appendLine("Package: $pkg")
        appendLine("Version: $versionName (build $versionCode)")
        appendLine("Works on: Android ${androidName(minSdk)} and newer (targets ${androidName(targetSdk)})")
        appendLine("Installed: ${df.format(Date(firstInstall))}, last updated ${df.format(Date(lastUpdate))}")
        appendLine("Installed from: ${installerName(installer)}")
        appendLine("Size: ${mb(totalBytes)} in ${apkFiles.size} file(s)")
        appendLine()

        appendLine("## How it was built")
        frameworks.forEach { appendLine("• $it") }
        appendLine("Code: $classCount classes in $dexCount code file(s)")
        appendLine("Scrambled code names: $obfuscation")
        appendLine()

        appendLine("## Services and libraries it uses")
        if (sdks.isEmpty()) appendLine("None of the common ones were found.")
        sdks.forEach { (cat, names) ->
            appendLine("$cat:")
            names.forEach { appendLine("  • $it") }
        }
        appendLine()

        if (firebase.isNotEmpty()) {
            appendLine("## Firebase project")
            firebase.forEach { (k, v) -> appendLine("$k: $v") }
            appendLine()
        }

        appendLine("## Servers it talks to (${domains.size})")
        if (domains.isEmpty()) appendLine("No web addresses found in the code.")
        domains.forEach { (host, n) -> appendLine("• $host  ($n mention${if (n == 1) "" else "s"})") }
        appendLine()

        if (endpoints.isNotEmpty()) {
            appendLine("## Possible API paths (${endpoints.size})")
            endpoints.forEach { appendLine("• $it") }
            appendLine()
        }

        appendLine("## Screens (${activities.size})")
        launcher?.let { appendLine("Opens on: ${short(it)}") }
        activities.forEach { appendLine("• ${short(it)}") }
        appendLine()

        if (deepLinks.isNotEmpty()) {
            appendLine("## Links that open the app (${deepLinks.size})")
            deepLinks.forEach { appendLine("• $it") }
            appendLine()
        }

        appendLine("## Permissions (${permissions.size})")
        val sens = permissions.filter { it.sensitive }
        if (sens.isNotEmpty()) {
            appendLine("Sensitive (asks the user):")
            sens.forEach { appendLine("  • ${it.label}  [${it.name.substringAfterLast('.')}]") }
        }
        val normal = permissions.filter { !it.sensitive }
        if (normal.isNotEmpty()) {
            appendLine("Normal:")
            normal.forEach { appendLine("  • ${it.label}  [${it.name.substringAfterLast('.')}]") }
        }
        appendLine()

        appendLine("## Background services (${services.size})")
        services.forEach { appendLine("• ${short(it)}") }
        appendLine()
        appendLine("## Event listeners (${receivers.size})")
        receivers.forEach { appendLine("• ${short(it)}") }
        appendLine()
        appendLine("## Data providers (${providers.size})")
        providers.forEach { appendLine("• ${short(it)}") }
        appendLine()

        appendLine("## The app's own code packages")
        if (appPackages.isEmpty()) appendLine("Could not tell them apart from libraries.")
        appPackages.forEach { (p, n) -> appendLine("• $p  ($n classes)") }
        appendLine()

        appendLine("## Native libraries (${nativeLibs.size})")
        if (abis.isNotEmpty()) appendLine("Processor types: ${abis.joinToString()}")
        nativeLibs.forEach { appendLine("• $it") }
        appendLine()

        appendLine("## All web addresses found (${urls.size})")
        urls.forEach { appendLine(it) }
    }

    private fun short(name: String) = if (name.startsWith("$pkg.")) name.removePrefix(pkg) else name
}

fun mb(bytes: Long) = String.format("%.1f MB", bytes / 1_048_576.0)

fun androidName(sdk: Int): String = when {
    sdk >= 36 -> "16"
    sdk >= 35 -> "15"
    sdk >= 34 -> "14"
    sdk >= 33 -> "13"
    sdk >= 31 -> "12"
    sdk >= 30 -> "11"
    sdk >= 29 -> "10"
    sdk >= 28 -> "9"
    sdk >= 26 -> "8"
    sdk >= 24 -> "7"
    sdk >= 23 -> "6"
    sdk >= 21 -> "5"
    else -> "4 or older"
} + " (API $sdk)"

fun installerName(pkg: String?): String = when (pkg) {
    null -> "Unknown / installed by hand"
    "com.android.vending" -> "Google Play Store"
    "com.sec.android.app.samsungapps" -> "Samsung Galaxy Store"
    "com.amazon.venezia" -> "Amazon Appstore"
    "com.huawei.appmarket" -> "Huawei AppGallery"
    "com.google.android.packageinstaller", "com.android.packageinstaller" -> "Installed from a file"
    else -> pkg
}

object Analyzer {

    private const val MAX_FILE_BYTES = 80L * 1024 * 1024
    private val urlRegex = Regex("""https?://[A-Za-z0-9._~:/?#\[\]@!$&'()*+,;=%-]{3,300}""")
    private val endpointRegex = Regex("""^/?[A-Za-z0-9_\-]+(/[A-Za-z0-9_\-{}.:]+){1,8}/?$""")
    private val versionSegment = Regex("""(^|/)v[0-9]+(/|$)""")

    @Suppress("DEPRECATION")
    fun analyze(ctx: Context, pkg: String, progress: (String) -> Unit): Report {
        val pm = ctx.packageManager
        val ai = pm.getApplicationInfo(pkg, 0)
        val label = ai.loadLabel(pm).toString()

        progress("Reading app details…")
        val pi: PackageInfo = pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
        val versionCode = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()

        val apkFiles = listOf(ai.sourceDir) + (ai.splitSourceDirs?.toList() ?: emptyList())
        val totalBytes = apkFiles.sumOf { File(it).length() }

        // Screens etc. are read from the APK file itself, which avoids Android's size limit
        // when asking the system about apps with hundreds of components.
        progress("Listing screens…")
        val compFlags = PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS
        val archive: PackageInfo? = try {
            pm.getPackageArchiveInfo(ai.sourceDir, compFlags)
        } catch (e: Exception) { null }
        val activities = archive?.activities?.map { it.name }?.sorted() ?: emptyList()
        val services = archive?.services?.map { it.name }?.sorted() ?: emptyList()
        val receivers = archive?.receivers?.map { it.name }?.sorted() ?: emptyList()
        val providers = archive?.providers?.map { it.name }?.sorted() ?: emptyList()
        val launcher = pm.getLaunchIntentForPackage(pkg)?.component?.className

        val permissions = (pi.requestedPermissions ?: emptyArray<String>()).map { name ->
            try {
                val info = pm.getPermissionInfo(name, 0)
                val base = info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
                Perm(name, info.loadLabel(pm).toString(), base == PermissionInfo.PROTECTION_DANGEROUS)
            } catch (e: Exception) {
                Perm(name, name.substringAfterLast('.').lowercase().replace('_', ' '), false)
            }
        }.sortedWith(compareBy({ !it.sensitive }, { it.label.lowercase() }))

        val installer = try {
            if (Build.VERSION.SDK_INT >= 30) pm.getInstallSourceInfo(pkg).installingPackageName
            else pm.getInstallerPackageName(pkg)
        } catch (e: Exception) { null }

        // ---- Look inside the APK files ----
        val entryNames = HashSet<String>()
        val libs = sortedSetOf<String>()
        val abis = sortedSetOf<String>()
        val foundSdks = LinkedHashSet<Sdk>()
        val pkgCounts = HashMap<String, Int>()
        val urlSet = LinkedHashSet<String>()
        val endpoints = sortedSetOf<String>()
        var classCount = 0
        var shortNames = 0
        var dexCount = 0
        var hasKotlin = false
        var jsFilesScanned = 0
        var flutterScanned = false

        fun takeUrls(text: String) {
            if (urlSet.size >= 3000) return
            urlRegex.findAll(text).forEach { m ->
                val u = m.value.trimEnd('.', ',', ';', ')', '\'', '"', '\\', ']', '}')
                if (u.length > 10) urlSet.add(u)
            }
        }

        fun onDexString(s: String) {
            if (s.length > 2 && s[0] == 'L' && s[s.length - 1] == ';') {
                classCount++
                if (s == "Lkotlin/Metadata;") hasKotlin = true
                for (sdk in Signatures.sdks) if (s.startsWith(sdk.prefix)) { foundSdks.add(sdk); break }
                val slash = s.lastIndexOf('/')
                if (slash > 1) {
                    val simple = s.substring(slash + 1, s.length - 1)
                    if (simple.length <= 2) shortNames++
                    val parts = s.substring(1, slash).split('/')
                    val key = parts.take(3).joinToString(".")
                    pkgCounts[key] = (pkgCounts[key] ?: 0) + 1
                } else if (s.length <= 4) {
                    shortNames++
                }
            } else if (s.contains("://")) {
                if (s.contains("http")) takeUrls(s)
            } else if (s.length in 5..120 && s.indexOf('/') >= 0 && endpoints.size < 400 &&
                endpointRegex.matches(s) && looksLikeApi(s)
            ) {
                endpoints.add(s)
            }
        }

        apkFiles.forEachIndexed { i, path ->
            try {
                ZipFile(path).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val e = entries.nextElement()
                        val name = e.name
                        entryNames.add(name)
                        when {
                            Regex("""classes\d*\.dex""").matches(name) && e.size < MAX_FILE_BYTES -> {
                                dexCount++
                                progress("Reading code file $dexCount (${File(path).name})…")
                                val bytes = zip.getInputStream(e).use { it.readBytes() }
                                readDexStrings(bytes, ::onDexString)
                            }
                            name.startsWith("lib/") && name.endsWith(".so") -> {
                                val parts = name.split('/')
                                if (parts.size >= 3) abis.add(parts[1])
                                libs.add(name.substringAfterLast('/'))
                                if (name.endsWith("/libapp.so") && !flutterScanned && e.size < MAX_FILE_BYTES) {
                                    flutterScanned = true
                                    progress("Reading Flutter code…")
                                    val bytes = zip.getInputStream(e).use { it.readBytes() }
                                    takeUrls(String(bytes, Charsets.ISO_8859_1))
                                }
                            }
                            name == "assets/index.android.bundle" && e.size < MAX_FILE_BYTES -> {
                                progress("Reading React Native code…")
                                val bytes = zip.getInputStream(e).use { it.readBytes() }
                                takeUrls(String(bytes, Charsets.ISO_8859_1))
                            }
                            name.startsWith("assets/") && jsFilesScanned < 300 && e.size < 8L * 1024 * 1024 &&
                                (name.endsWith(".js") || name.endsWith(".json") || name.endsWith(".html")) -> {
                                jsFilesScanned++
                                val bytes = zip.getInputStream(e).use { it.readBytes() }
                                takeUrls(String(bytes, Charsets.ISO_8859_1))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                progress("Could not fully read file ${i + 1}: ${e.message}")
            }
        }

        progress("Putting the report together…")

        // Frameworks
        val frameworks = mutableListOf<String>()
        fun hasLib(n: String) = libs.contains(n)
        if (hasLib("libflutter.so") || entryNames.any { it.startsWith("assets/flutter_assets/") })
            frameworks.add("Flutter (Dart). Code is compiled, so it is hard to read back.")
        if (entryNames.contains("assets/index.android.bundle") || hasLib("libreactnativejni.so") || hasLib("libreactnative.so"))
            frameworks.add("React Native (JavaScript)" + if (hasLib("libhermes.so")) ", using the Hermes engine" else "")
        if (entryNames.any { it.startsWith("assets/expo") } || foundSdksHasPrefix(entryNames, "expo"))
            frameworks.add("Expo (React Native toolkit)")
        if (hasLib("libunity.so")) frameworks.add("Unity (game engine, C#)")
        if (hasLib("libmonodroid.so") || entryNames.any { it.startsWith("assemblies/") })
            frameworks.add("Xamarin / .NET MAUI (C#)")
        if (entryNames.contains("assets/capacitor.config.json")) frameworks.add("Capacitor / Ionic (web app in a shell)")
        else if (entryNames.contains("assets/www/cordova.js")) frameworks.add("Cordova (web app in a shell)")
        if (hasLib("libgodot_android.so")) frameworks.add("Godot (game engine)")
        if (libs.any { it.contains("cocos") }) frameworks.add("Cocos2d (game engine)")
        if (libs.any { it.contains("NativeScript") }) frameworks.add("NativeScript (JavaScript)")
        if (libs.any { it.startsWith("libUE4") || it.startsWith("libUnreal") }) frameworks.add("Unreal Engine")
        if (frameworks.isEmpty()) {
            frameworks.add(if (hasKotlin) "Native Android, written in Kotlin" else "Native Android, written in Java")
        } else if (hasKotlin) {
            frameworks.add("Kotlin is also used for the Android parts")
        }
        if (foundSdks.any { it.prefix == "Landroidx/compose/" }) frameworks.add("Screens built with Jetpack Compose")

        val sdkMap = LinkedHashMap<String, MutableList<String>>()
        foundSdks.sortedBy { it.category }.forEach { sdk ->
            val list = sdkMap.getOrPut(sdk.category) { mutableListOf() }
            if (!list.contains(sdk.name)) list.add(sdk.name)
        }

        val ratio = if (classCount == 0) 0.0 else shortNames.toDouble() / classCount
        val obfuscation = when {
            classCount == 0 -> "Unknown (no Java/Kotlin code found)"
            ratio > 0.35 -> "Heavy. Most names are scrambled, like a.b.c"
            ratio > 0.12 -> "Some. Part of the code has scrambled names"
            else -> "Little or none. Names are mostly readable"
        }

        val appPackages = pkgCounts.entries
            .filter { (p, _) -> Signatures.libraryPackages.none { p.startsWith(it.trimEnd('.')) } }
            .filter { (p, _) -> p.length > 2 }
            .sortedByDescending { it.value }
            .take(15)
            .map { it.key to it.value }

        val urls = urlSet.filter { u ->
            val h = hostOf(u)
            h.contains('.') && !h.contains('%') && !h.contains('$') && !h.contains('{') &&
                Signatures.noiseHosts.none { n -> h == n || h.endsWith(".$n") || u.contains("://$n") }
        }.sorted()
        val domains = urls.groupingBy { hostOf(it) }.eachCount()
            .entries.sortedByDescending { it.value }.map { it.key to it.value }

        return Report(
            label = label, pkg = pkg, versionName = pi.versionName ?: "?", versionCode = versionCode,
            minSdk = ai.minSdkVersion, targetSdk = ai.targetSdkVersion,
            firstInstall = pi.firstInstallTime, lastUpdate = pi.lastUpdateTime,
            installer = installer, apkFiles = apkFiles, totalBytes = totalBytes,
            frameworks = frameworks, sdks = sdkMap, obfuscation = obfuscation,
            appPackages = appPackages, classCount = classCount, dexCount = dexCount,
            permissions = permissions, launcher = launcher, activities = activities,
            services = services, receivers = receivers, providers = providers,
            nativeLibs = libs.toList(), abis = abis.toList(), domains = domains,
            urls = urls.take(1500), endpoints = endpoints.toList(),
            firebase = firebaseInfo(pm, ai), deepLinks = deepLinks(ctx, pkg),
        )
    }

    private fun foundSdksHasPrefix(entries: Set<String>, p: String) = entries.any { it.startsWith("assets/$p") }

    private fun looksLikeApi(s: String): Boolean {
        val l = s.lowercase()
        if (l.startsWith("res/") || l.startsWith("meta-inf") || l.startsWith("kotlin/") ||
            l.startsWith("java/") || l.startsWith("android/") || l.startsWith("androidx/") ||
            l.startsWith("com/") || l.startsWith("org/") || l.startsWith("lib/") || l.startsWith("assets/")
        ) return false
        if (l.endsWith(".png") || l.endsWith(".xml") || l.endsWith(".so") || l.endsWith(".class") ||
            l.endsWith(".java") || l.endsWith(".kt") || l.endsWith(".json")
        ) return false
        return l.contains("api") || l.contains("{") || versionSegment.containsMatchIn(l) ||
            l.contains("graphql") || l.contains("auth") || l.contains("/users") || l.contains("login")
    }

    fun hostOf(url: String): String =
        url.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
            .substringAfter('@').substringBefore(':').lowercase()

    /** Reads every string stored in a .dex code file: class names, text, web addresses. */
    fun readDexStrings(bytes: ByteArray, onString: (String) -> Unit) {
        if (bytes.size < 0x70 || bytes[0] != 'd'.code.toByte() || bytes[1] != 'e'.code.toByte()) return
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val count = bb.getInt(0x38)
        val idsOff = bb.getInt(0x3C)
        if (count <= 0 || idsOff <= 0 || idsOff.toLong() + count.toLong() * 4 > bytes.size) return
        for (i in 0 until count) {
            try {
                var p = bb.getInt(idsOff + i * 4)
                if (p <= 0 || p >= bytes.size) continue
                while (p < bytes.size && (bytes[p].toInt() and 0x80) != 0) p++ // skip length (uleb128)
                p++
                var end = p
                while (end < bytes.size && bytes[end] != 0.toByte()) end++
                if (end > p) onString(String(bytes, p, end - p, Charsets.UTF_8))
            } catch (e: Exception) {
                // skip a damaged entry
            }
        }
    }

    private fun firebaseInfo(pm: PackageManager, ai: ApplicationInfo): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        try {
            val res = pm.getResourcesForApplication(ai)
            val keys = listOf(
                "project_id" to "Project ID",
                "firebase_database_url" to "Database URL",
                "google_storage_bucket" to "Storage bucket",
            )
            for ((key, title) in keys) {
                val id = res.getIdentifier(key, "string", ai.packageName)
                if (id != 0) {
                    val v = res.getString(id)
                    if (v.isNotBlank()) out[title] = v
                }
            }
        } catch (e: Exception) {
            // not available
        }
        return out
    }

    /** Web links and custom app links (myapp://...) that open this app, read from its manifest. */
    private fun deepLinks(ctx: Context, pkg: String): List<String> {
        val out = sortedSetOf<String>()
        val ns = "http://schemas.android.com/apk/res/android"
        try {
            val other = ctx.createPackageContext(pkg, 0)
            val parser = other.assets.openXmlResourceParser("AndroidManifest.xml")
            var inFilter = false
            val schemes = mutableListOf<String>()
            val hosts = mutableListOf<String>()
            var event = parser.next()
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "intent-filter" -> { inFilter = true; schemes.clear(); hosts.clear() }
                        "data" -> if (inFilter) {
                            parser.getAttributeValue(ns, "scheme")?.let { schemes.add(it) }
                            parser.getAttributeValue(ns, "host")?.let { hosts.add(it) }
                        }
                    }
                } else if (event == XmlPullParser.END_TAG && parser.name == "intent-filter") {
                    inFilter = false
                    for (sc in schemes) {
                        if (sc == "content" || sc == "file" || sc == "package") continue
                        if (hosts.isEmpty()) out.add("$sc://") else hosts.forEach { out.add("$sc://$it") }
                    }
                }
                event = parser.next()
                if (out.size > 200) break
            }
            parser.close()
        } catch (e: Exception) {
            // manifest not readable
        }
        return out.toList()
    }
}
