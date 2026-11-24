package com.appxray

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DetailActivity : Activity() {

    private lateinit var pkg: String
    private lateinit var status: TextView
    private lateinit var body: TextView
    private val buttons = mutableListOf<Button>()
    private var report: Report? = null
    private var pendingSave: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pkg = intent.getStringExtra("pkg") ?: run { finish(); return }
        val pm = packageManager
        val label = try { pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString() } catch (e: Exception) { pkg }
        title = label

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(24))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(context).apply {
                setImageDrawable(try { pm.getApplicationIcon(pkg) } catch (e: Exception) { null })
            }, LinearLayout.LayoutParams(dp(56), dp(56)))
            addView(TextView(context).apply {
                text = "$label\n$pkg"
                textSize = 16f
                setPadding(dp(12), 0, 0, 0)
            })
        }
        col.addView(header)

        fun button(text: String, action: () -> Unit) = Button(this).apply {
            this.text = text
            isAllCaps = false
            isEnabled = false
            setOnClickListener { action() }
            buttons.add(this)
            col.addView(this, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        button("Send app + report to Claude") { exportZip { shareFile(it) } }
        button("Save app + report to phone") { exportZip { saveFile(it) } }
        button("Share report as text") { shareText() }

        status = TextView(this).apply {
            setPadding(0, dp(12), 0, dp(8))
            text = "Starting…"
        }
        col.addView(status)
        body = TextView(this).apply {
            setTextIsSelectable(true)
            textSize = 14f
            setLineSpacing(0f, 1.15f)
        }
        col.addView(body)

        setContentView(ScrollView(this).apply { addView(col) })

        Thread {
            try {
                val r = Analyzer.analyze(this, pkg) { msg -> runOnUiThread { status.text = msg } }
                runOnUiThread {
                    report = r
                    status.text = "Done. Long-press any text to copy it."
                    body.text = styled(r.toText())
                    buttons.forEach { it.isEnabled = true }
                }
            } catch (e: Throwable) {
                runOnUiThread { status.text = "Could not X-ray this app: ${e.message}" }
            }
        }.start()
    }

    /** Makes the "## " lines into bold headings. */
    private fun styled(text: String): CharSequence {
        val sb = SpannableStringBuilder()
        text.lines().forEachIndexed { i, line ->
            if (line.startsWith("## ")) {
                val start = sb.length
                sb.append("\n").append(line.removePrefix("## "))
                sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(RelativeSizeSpan(1.2f), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (i == 0) {
                val start = sb.length
                sb.append(line)
                sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                sb.append(line)
            }
            sb.append("\n")
        }
        return sb
    }

    /** Packs the app's APK file(s) and the report into one .zip, ready to send. */
    private fun exportZip(then: (File) -> Unit) {
        val r = report ?: return
        val dialog = AlertDialog.Builder(this).setMessage("Packing ${r.label}…").setCancelable(false).show()
        buttons.forEach { it.isEnabled = false }
        Thread {
            try {
                val dir = File(cacheDir, "exports").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
                val safe = r.label.replace(Regex("[^A-Za-z0-9]+"), "-").trim('-').ifEmpty { "app" }
                val out = File(dir, "$safe-${r.versionName.replace(Regex("[^A-Za-z0-9.]+"), "")}-xray.zip")
                ZipOutputStream(out.outputStream().buffered()).use { zip ->
                    zip.setLevel(Deflater.BEST_SPEED)
                    zip.putNextEntry(ZipEntry("xray-report.txt"))
                    zip.write(r.toText().toByteArray())
                    zip.closeEntry()
                    r.apkFiles.forEachIndexed { i, path ->
                        val f = File(path)
                        val name = if (i == 0) "base.apk" else f.name
                        runOnUiThread { dialog.setMessage("Packing ${r.label}…\n$name (${mb(f.length())})") }
                        zip.putNextEntry(ZipEntry("apk/$name"))
                        FileInputStream(f).use { it.copyTo(zip, 256 * 1024) }
                        zip.closeEntry()
                    }
                }
                runOnUiThread {
                    dialog.dismiss()
                    buttons.forEach { it.isEnabled = true }
                    then(out)
                }
            } catch (e: Throwable) {
                runOnUiThread {
                    dialog.dismiss()
                    buttons.forEach { it.isEnabled = true }
                    Toast.makeText(this, "Could not pack the app: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "com.appxray.files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            putExtra(Intent.EXTRA_TEXT, "Please reverse engineer this app with REA and give me a plan to build my own version.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, "Send ${file.name} (${mb(file.length())})"))
    }

    private fun saveFile(file: File) {
        pendingSave = file
        val pick = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, file.name)
        }
        startActivityForResult(pick, SAVE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != SAVE_REQUEST || resultCode != RESULT_OK) return
        val target: Uri = data?.data ?: return
        val src = pendingSave ?: return
        Thread {
            val ok = try {
                contentResolver.openOutputStream(target)?.use { out -> src.inputStream().use { it.copyTo(out) } }
                true
            } catch (e: Exception) { false }
            runOnUiThread {
                Toast.makeText(this, if (ok) "Saved ${src.name}" else "Could not save the file", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun shareText() {
        val text = report?.toText() ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "App X-Ray: ${report?.label}")
            putExtra(Intent.EXTRA_TEXT, if (text.length > 90_000) text.take(90_000) + "\n…(cut short)" else text)
        }
        startActivity(Intent.createChooser(send, "Share report"))
    }

    companion object {
        private const val SAVE_REQUEST = 41
    }
}
