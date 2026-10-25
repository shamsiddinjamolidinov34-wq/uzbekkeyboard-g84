package uz.accessibility.keyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("uz_kb_prefs", Context.MODE_PRIVATE)

        val scrollView = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 36, 36, 36)
        }

        val title = TextView(this).apply {
            text = "Yangi Oʻzbek Klaviaturasi Sozlamalari"
            textSize = 20f
            setPadding(0, 0, 0, 24)
        }
        container.addView(title)

        container.addView(Button(this).apply {
            text = "1. Klaviaturani tizimdan yoqish"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        })

        container.addView(Button(this).apply {
            text = "2. Klaviaturani tanlash"
            setOnClickListener {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
        })

        container.addView(Switch(this).apply {
            text = "Barmoqni uzganda yozish (Lift-to-type)"
            textSize = 16f
            setPadding(0, 32, 0, 16)
            isChecked = prefs.getBoolean("lift_to_type", true)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("lift_to_type", isChecked).apply()
            }
        })

        container.addView(Switch(this).apply {
            text = "Yozilgan harfni ovozli oʻqish"
            textSize = 16f
            setPadding(0, 16, 0, 16)
            isChecked = prefs.getBoolean("speak_keys", true)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("speak_keys", isChecked).apply()
            }
        })

        container.addView(Switch(this).apply {
            text = "Tepada raqamlar qatorini koʻrsatish"
            textSize = 16f
            setPadding(0, 16, 0, 16)
            isChecked = prefs.getBoolean("show_number_row", true)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("show_number_row", isChecked).apply()
            }
        })

        container.addView(Switch(this).apply {
            text = "Yirik tugmalar rejimi (Ekranda oson topish uchun)"
            textSize = 16f
            setPadding(0, 16, 0, 16)
            isChecked = prefs.getInt("key_height_dp", 58) == 68
            setOnCheckedChangeListener { _, isChecked ->
                val height = if (isChecked) 68 else 58
                prefs.edit().putInt("key_height_dp", height).apply()
            }
        })

        container.addView(Switch(this).apply {
            text = "Oʻ harfi uchun Õ (toʻlqinli) belgisini ishlatish (Standart: Ō)"
            textSize = 16f
            setPadding(0, 16, 0, 16)
            isChecked = prefs.getBoolean("use_tilde_o", false)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("use_tilde_o", isChecked).apply()
            }
        })

        container.addView(Switch(this).apply {
            text = "Tugma bosilganda vibratsiya"
            textSize = 16f
            setPadding(0, 16, 0, 16)
            isChecked = prefs.getBoolean("vibrate_on_keypress", true)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("vibrate_on_keypress", isChecked).apply()
            }
        })

        container.addView(Switch(this).apply {
            text = "Tugma bosilganda ovoz effekti"
            textSize = 16f
            setPadding(0, 16, 0, 16)
            isChecked = prefs.getBoolean("sound_on_keypress", true)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("sound_on_keypress", isChecked).apply()
            }
        })

        scrollView.addView(container)
        setContentView(scrollView)
    }
}
