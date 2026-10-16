package com.example.safekeyboard

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.inputmethodservice.InputMethodService
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import java.util.Locale

class MyKeyboardService : InputMethodService() {

    private var mediaPlayer: MediaPlayer? = null

    // Список запрещенных слов для мгновенной очистки и воспроизведения звука
    private val triggerWords = listOf("трахает", "гей", "поза", "рака", "блять", "сука", "fuck", "бля", "traxaet")

    private enum class Language { RU, HY, EN }
    private var currentLanguage = Language.RU
    private var isSymbols = false
    private var isUppercase = false // Флаг режима заглавных букв
    private var isRippleOrange = true // Флаг для чередования цветов анимации (оранжевый / синий)

    private lateinit var keyboardMainLayout: LinearLayout
    private lateinit var alarmLayout: LinearLayout
    private lateinit var btnStopAlarm: Button

    private lateinit var row1: LinearLayout
    private lateinit var row2: LinearLayout
    private lateinit var row3: LinearLayout
    private lateinit var btnLanguage: Button
    private lateinit var btnSymbolToggle: Button
    private lateinit var btnShift: Button

    private val handler = Handler(Looper.getMainLooper())
    private var enableButtonRunnable: Runnable? = null

    // Полные и аккуратные раскладки клавиатуры для каждого языка
// Полная русская раскладка (33 буквы)
    private val ruRows = listOf(
        listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х", "ъ"),
        listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"),
        listOf("я", "ч", "с", "м", "и", "т", "ь", "б", "ю", "ё")
    )

    // Полная армянская раскладка (все 39 букв, включая добавленные «է», «թ», «ժ», «ձ», «փ», а также «ր» и «և»)
    private val hyRows = listOf(
        listOf("ք", "ո", "ե", "ռ", "տ", "ը", "ւ", "ի", "օ", "պ", "է", "թ", "ժ"),
        listOf("ա", "ս", "դ", "ֆ", "գ", "հ", "յ", "կ", "լ", "խ", "շ", "և"),
        listOf("զ", "ղ", "ց", "վ", "բ", "ն", "մ", "չ", "ճ", "ջ", "ծ", "ր", "ձ", "փ")
    )

    // Полная английская раскладка (26 букв)
    private val enRows = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("z", "x", "c", "v", "b", "n", "m")
    )

    // Раскладка цифр и спецсимволов
    private val symbolRows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "<", ">"),
        listOf("*", "\"", "'", ":", ";", "!", "?", "/", "=")
    )

    override fun onCreate() {
        super.onCreate()
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)

        keyboardMainLayout = view.findViewById(R.id.keyboard_main_layout)
        alarmLayout = view.findViewById(R.id.alarm_layout)
        btnStopAlarm = view.findViewById(R.id.btn_stop_alarm)

        row1 = view.findViewById(R.id.row_1)
        row2 = view.findViewById(R.id.row_2)
        row3 = view.findViewById(R.id.row_3)
        btnLanguage = view.findViewById(R.id.btn_language)
        btnSymbolToggle = view.findViewById(R.id.btn_symbol_toggle)
        btnShift = view.findViewById(R.id.btn_shift)

        val btnDelete = view.findViewById<Button>(R.id.btn_delete)
        val btnSpace = view.findViewById<Button>(R.id.btn_space)
        val btnEnter = view.findViewById<Button>(R.id.btn_enter)
        val btnComma = view.findViewById<Button>(R.id.btn_comma)
        val btnDot = view.findViewById<Button>(R.id.btn_dot)

        // Обработчики служебных клавиш с обновлением цвета ripple-анимации
        btnDelete.setOnClickListener {
            updateRippleColor(it)
            currentInputConnection?.deleteSurroundingText(1, 0)
        }

        btnSpace.setOnClickListener {
            updateRippleColor(it)
            currentInputConnection?.commitText(" ", 1)
        }

        btnComma.setOnClickListener {
            updateRippleColor(it)
            currentInputConnection?.commitText(",", 1)
        }

        btnDot.setOnClickListener {
            updateRippleColor(it)
            currentInputConnection?.commitText(".", 1)
        }

        btnEnter.setOnClickListener {
            updateRippleColor(it)
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }

        // Кнопка переключения символов/цифр
        btnSymbolToggle.setOnClickListener {
            updateRippleColor(it)
            isSymbols = !isSymbols
            btnSymbolToggle.text = if (isSymbols) currentLanguage.name else "123"
            updateKeyboardLayout()
        }

        // Кнопка Shift для заглавных букв
        btnShift.setOnClickListener {
            updateRippleColor(it)
            if (!isSymbols) {
                isUppercase = !isUppercase
                updateKeyboardLayout()
            }
        }

        // Кнопка глобуса для переключения языков
        btnLanguage.setOnClickListener {
            updateRippleColor(it)
            if (!isSymbols) {
                toggleLanguage()
            }
        }

        // Обработчик большой кнопки остановки музыки
        btnStopAlarm.setOnClickListener {
            stopAlarmAndRestoreKeyboard()
        }

        // Первоначальное построение раскладки
        updateKeyboardLayout()

        return view
    }

    // Метод для поочередной смены цвета ripple-эффекта (оранжевый ⇄ синий)
    private fun updateRippleColor(view: View) {
        isRippleOrange = !isRippleOrange
        val colorInt = if (isRippleOrange) {
            Color.parseColor("#80FF6600") // Оранжевый
        } else {
            Color.parseColor("#800026FF") // Синий
        }

        val background = view.background
        if (background is RippleDrawable) {
            background.setColor(ColorStateList.valueOf(colorInt))
        }
    }

    private fun triggerAlarm() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, R.raw.secret_sound)
        mediaPlayer?.start()

        keyboardMainLayout.visibility = View.GONE
        alarmLayout.visibility = View.VISIBLE

        btnStopAlarm.isEnabled = false
        btnStopAlarm.text = "ОСТАНОВИТЬ МУЗЫКУ (Ждите... 15с)"

        if (enableButtonRunnable != null) {
            handler.removeCallbacks(enableButtonRunnable!!)
        }

        enableButtonRunnable = Runnable {
            btnStopAlarm.isEnabled = true
            btnStopAlarm.text = "ОСТАНОВИТЬ МУЗЫКУ"
        }
        handler.postDelayed(enableButtonRunnable!!, 15000L)
    }

    private fun stopAlarmAndRestoreKeyboard() {
        if (enableButtonRunnable != null) {
            handler.removeCallbacks(enableButtonRunnable!!)
        }
        mediaPlayer?.release()
        mediaPlayer = null

        alarmLayout.visibility = View.GONE
        keyboardMainLayout.visibility = View.VISIBLE
    }

    private fun toggleLanguage() {
        currentLanguage = when (currentLanguage) {
            Language.RU -> Language.HY
            Language.HY -> Language.EN
            Language.EN -> Language.RU
        }
        btnLanguage.text = "🌐 ${currentLanguage.name}"
        updateKeyboardLayout()
    }

    private fun updateKeyboardLayout() {
        row1.removeAllViews()
        row2.removeAllViews()
        row3.removeAllViews()

        val activeRows = if (isSymbols) {
            symbolRows
        } else {
            when (currentLanguage) {
                Language.RU -> ruRows
                Language.HY -> hyRows
                Language.EN -> enRows
            }
        }

        populateRow(row1, activeRows[0])
        populateRow(row2, activeRows[1])
        populateRow(row3, activeRows[2])
    }

    private fun populateRow(rowLayout: LinearLayout, keys: List<String>) {
        for (keyText in keys) {
            val displayChar = if (!isSymbols && isUppercase) {
                keyText.uppercase(Locale.getDefault())
            } else {
                keyText
            }

            val button = Button(this).apply {
                text = displayChar
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1.0f
                ).apply {
                    setMargins(3, 3, 3, 3)
                }
                setBackgroundResource(R.drawable.key_background)
                setTextColor(Color.WHITE)
                textSize = 17f
                setOnClickListener {
                    // Меняем цвет анимации нажатия при каждом клике
                    updateRippleColor(it)
                    currentInputConnection?.commitText(displayChar, 1)
                }
            }
            rowLayout.addView(button)
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)

        val ic = currentInputConnection ?: return
        val textBeforeCursor = ic.getTextBeforeCursor(50, 0)?.toString()?.lowercase() ?: return

        for (trigger in triggerWords) {
            if (textBeforeCursor.contains(trigger)) {
                ic.deleteSurroundingText(textBeforeCursor.length, 0)
                triggerAlarm()
                break
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (enableButtonRunnable != null) {
            handler.removeCallbacks(enableButtonRunnable!!)
        }
        mediaPlayer?.release()
    }
}