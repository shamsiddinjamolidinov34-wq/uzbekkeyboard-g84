package uz.accessibility.keyboard

import android.content.Context
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout

class UzbekKeyboardService : InputMethodService() {

    private var isCaps = false
    private var isSymbols = false
    private var lastHoveredKey: Pair<String, String>? = null
    private lateinit var rootLayout: LinearLayout
    private lateinit var accessibilityManager: AccessibilityManager

    override fun onCreate() {
        super.onCreate()
        accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }

    override fun onCreateInputView(): View {
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(6, 10, 6, 14)
        }
        buildKeyboard()
        return rootLayout
    }

    override fun onWindowShown() {
        super.onWindowShown()
        if (::rootLayout.isInitialized) {
            buildKeyboard()
        }
    }

    private fun buildKeyboard() {
        rootLayout.removeAllViews()
        val prefs = getSharedPreferences("uz_kb_prefs", Context.MODE_PRIVATE)
        val showNumbers = prefs.getBoolean("show_number_row", true)
        val useTildeO = prefs.getBoolean("use_tilde_o", false)
        val keyHeightDp = prefs.getInt("key_height_dp", 58)

        val rows = if (isSymbols) {
            getSymbolRows()
        } else {
            getLetterRows(showNumbers, useTildeO)
        }

        for (row in rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(keyHeightDp)
                ).apply {
                    setMargins(0, 3, 0, 3)
                }
            }

            for (keyLabel in row) {
                val btn = createKeyButton(keyLabel)
                rowLayout.addView(btn)
            }
            rootLayout.addView(rowLayout)
        }
    }

    private fun getLetterRows(showNumbers: Boolean, useTildeO: Boolean): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        if (showNumbers) {
            rows.add(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"))
        }

        val oLetter = if (useTildeO) "õ" else "ō"

        rows.add(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p", oLetter, "ğ"))
        rows.add(listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "ş", "ç"))
        rows.add(listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "ʼ", "DEL"))
        rows.add(listOf("?123", ",", "◄", "SPACE", "►", ".", "ENTER"))

        return rows
    }

    private fun getSymbolRows(): List<List<String>> {
        return listOf(
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
            listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/"),
            listOf("*", "\"", "'", ":", ";", "!", "?", "_", "DEL"),
            listOf("ABC", ",", "◄", "SPACE", "►", ".", "ENTER")
        )
    }

    private fun createKeyButton(key: String): Button {
        val prefs = getSharedPreferences("uz_kb_prefs", Context.MODE_PRIVATE)
        val liftToType = prefs.getBoolean("lift_to_type", true)

        val displayKey = when (key) {
            "SHIFT" -> if (isCaps) "⬆" else "⇧"
            "DEL" -> "⌫"
            "SPACE" -> "Boʻsh joy"
            "ENTER" -> "↵"
            "?123", "ABC", "◄", "►" -> key
            else -> if (isCaps) key.uppercase() else key
        }

        val isSpecial = key in listOf("SHIFT", "DEL", "?123", "ABC", "ENTER", "◄", "►")
        val defaultBgColor = if (isSpecial) Color.parseColor("#2E3440") else Color.parseColor("#242933")

        val weight = when (key) {
            "SPACE" -> 3.2f
            "SHIFT", "DEL", "?123", "ABC", "ENTER" -> 1.5f
            else -> 1.0f
        }

        return Button(this).apply {
            text = displayKey
            textSize = 18f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(defaultBgColor)
            gravity = Gravity.CENTER
            setPadding(2, 2, 2, 2)

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                weight
            ).apply {
                setMargins(2, 0, 2, 0)
            }

            contentDescription = getAccessibilityDescription(displayKey, key)

            setOnHoverListener { v, event ->
                if (liftToType && accessibilityManager.isTouchExplorationEnabled) {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_HOVER_ENTER -> {
                            v.setBackgroundColor(Color.parseColor("#4C566A"))
                            lastHoveredKey = Pair(displayKey, key)
                        }
                        MotionEvent.ACTION_HOVER_MOVE -> {
                            lastHoveredKey = Pair(displayKey, key)
                        }
                        MotionEvent.ACTION_HOVER_EXIT -> {
                            v.setBackgroundColor(defaultBgColor)
                            val insideX = event.x >= 0 && event.x <= v.width
                            val insideY = event.y >= 0 && event.y <= v.height
                            if (insideX && insideY && lastHoveredKey?.second == key) {
                                processKeyPress(displayKey, key)
                                lastHoveredKey = null
                            }
                        }
                    }
                }
                false
            }

            setOnClickListener {
                processKeyPress(displayKey, key)
            }

            if (key == "DEL") {
                setOnLongClickListener {
                    currentInputConnection?.deleteSurroundingText(15, 0)
                    announceKey("Soʻz oʻchirildi")
                    true
                }
            }
        }
    }

    private fun processKeyPress(displayKey: String, rawKey: String) {
        triggerFeedback()
        val ic = currentInputConnection ?: return

        when (rawKey) {
            "SHIFT" -> {
                isCaps = !isCaps
                announceKey(if (isCaps) "Katta harflar yoqildi" else "Katta harflar oʻchirildi")
                buildKeyboard()
            }
            "DEL" -> {
                val deleted = ic.getSelectedText(0)
                if (!deleted.isNullOrEmpty()) {
                    ic.commitText("", 1)
                } else {
                    ic.deleteSurroundingText(1, 0)
                }
                announceKey("Oʻchirildi")
            }
            "SPACE" -> {
                ic.commitText(" ", 1)
                announceKey("Boʻsh joy")
            }
            "ENTER" -> {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                announceKey("Kiritish")
            }
            "◄" -> {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT)
                announceKey("Kursor chapga")
            }
            "►" -> {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT)
                announceKey("Kursor oʻngga")
            }
            "?123" -> {
                isSymbols = true
                announceKey("Belgilar rejimi")
                buildKeyboard()
            }
            "ABC" -> {
                isSymbols = false
                announceKey("Harflar rejimi")
                buildKeyboard()
            }
            else -> {
                ic.commitText(displayKey, 1)
                announceKey(getAccessibilityDescription(displayKey, rawKey))
                if (isCaps) {
                    isCaps = false
                    buildKeyboard()
                }
            }
        }
    }

    private fun getAccessibilityDescription(displayKey: String, rawKey: String): String {
        val isUpper = isCaps && rawKey.length == 1 && rawKey[0].isLetter()
        val prefix = if (isUpper) "Katta " else ""

        return when (rawKey.lowercase()) {
            "shift" -> if (isCaps) "Katta harf rejimi yoqilgan" else "Katta harf"
            "del" -> "Oʻchirish"
            "space" -> "Boʻsh joy"
            "enter" -> "Yangi qator yoki kiritish"
            "◄" -> "Kursorni chapga surish"
            "►" -> "Kursorni oʻngga surish"
            "?123" -> "Raqamlar va belgilar"
            "abc" -> "Harflar klaviaturasi"
            "ō", "õ" -> "${prefix}Oʻ harfi"
            "ğ" -> "${prefix}Gʻ harfi"
            "ş" -> "${prefix}Sh harfi"
            "ç" -> "${prefix}Ch harfi"
            "ʼ" -> "Tutuq belgisi"
            "." -> "Nuqta"
            "," -> "Vergul"
            else -> "$prefix$displayKey"
        }
    }

    @Suppress("DEPRECATION")
    private fun announceKey(text: String) {
        val prefs = getSharedPreferences("uz_kb_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("speak_keys", true) || !accessibilityManager.isEnabled) return

        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT).apply {
            className = javaClass.name
            packageName = applicationContext.packageName
            this.text.add(text)
        }
        accessibilityManager.sendAccessibilityEvent(event)
    }

    private fun triggerFeedback() {
        val prefs = getSharedPreferences("uz_kb_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("vibrate_on_keypress", true)) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
            }
        }

        if (prefs.getBoolean("sound_on_keypress", true)) {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
