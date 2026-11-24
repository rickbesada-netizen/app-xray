package com.appxray

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

data class AppItem(val label: String, val pkg: String, val system: Boolean, val sizeBytes: Long)

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

class MainActivity : Activity() {

    private var all: List<AppItem> = emptyList()
    private var shown: List<AppItem> = emptyList()
    private val icons = HashMap<String, Drawable?>()
    private lateinit var adapter: AppAdapter
    private lateinit var search: EditText
    private lateinit var showSystem: CheckBox
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "App X-Ray"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), 0)
        }
        search = EditText(this).apply {
            hint = "Search apps"
            setSingleLine()
        }
        showSystem = CheckBox(this).apply { text = "Show built-in system apps" }
        status = TextView(this).apply {
            text = "Loading your apps…"
            setPadding(0, dp(4), 0, dp(4))
        }
        val list = ListView(this)
        adapter = AppAdapter()
        list.adapter = adapter
        list.setOnItemClickListener { _, _, pos, _ ->
            startActivity(Intent(this, DetailActivity::class.java).putExtra("pkg", shown[pos].pkg))
        }

        root.addView(search)
        root.addView(showSystem)
        root.addView(status)
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = filter()
        })
        showSystem.setOnCheckedChangeListener { _, _ -> filter() }

        Thread {
            val pm = packageManager
            val items = pm.getInstalledApplications(0)
                .filter { it.packageName != packageName }
                .map {
                    val sys = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                        (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
                    val size = (listOf(it.sourceDir) + (it.splitSourceDirs?.toList() ?: emptyList()))
                        .sumOf { p -> java.io.File(p).length() }
                    AppItem(it.loadLabel(pm).toString(), it.packageName, sys, size)
                }
                .sortedBy { it.label.lowercase() }
            runOnUiThread {
                all = items
                filter()
            }
        }.start()
    }

    private fun filter() {
        val q = search.text.toString().trim().lowercase()
        shown = all.filter { (showSystem.isChecked || !it.system) &&
            (q.isEmpty() || it.label.lowercase().contains(q) || it.pkg.contains(q)) }
        status.text = "${shown.size} apps. Tap one to X-ray it."
        adapter.notifyDataSetChanged()
    }

    private inner class AppAdapter : BaseAdapter() {
        override fun getCount() = shown.size
        override fun getItem(position: Int) = shown[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val row = (convertView as? LinearLayout) ?: LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
                addView(ImageView(context), LinearLayout.LayoutParams(dp(44), dp(44)))
                val texts = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), 0, 0, 0)
                    addView(TextView(context).apply { textSize = 16f; setTypeface(typeface, Typeface.BOLD) })
                    addView(TextView(context).apply { textSize = 12f; alpha = 0.7f })
                }
                addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }
            val item = shown[position]
            val icon = row.getChildAt(0) as ImageView
            val texts = row.getChildAt(1) as LinearLayout
            (texts.getChildAt(0) as TextView).text = item.label
            (texts.getChildAt(1) as TextView).text = "${item.pkg} · ${mb(item.sizeBytes)}"
            val d = icons.getOrPut(item.pkg) {
                try { packageManager.getApplicationIcon(item.pkg) } catch (e: Exception) { null }
            }
            icon.setImageDrawable(d)
            return row
        }
    }
}
