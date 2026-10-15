package com.example.safekeyboard

import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.media.MediaPlayer
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout

class MyKeyboardService : InputMethodService() {

    private var mediaPlayer: MediaPlayer? = null

    // Список запрещенных слов для мгновенной очистки и воспроизведения звука
    private val triggerWords = listOf("трахает", "гей", "поза", "рака", "блять", "сука", "fuck")

    private enum class Language { RU, HY, EN }
    private var currentLanguage = Language.RU

    private lateinit var row1: LinearLayout
    private lateinit var row2: LinearLayout
    private lateinit var row3: LinearLayout
    private lateinit var btnLanguage: Button

    // Раскладки клавиатуры по рядам для каждого языка
    private val ruRows = listOf(
        listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х"),
        listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"),
        listOf("я", "ч", "с", "м", "и", "т", "ь", "б", "ю")
    )

    private val hyRows = listOf(
        listOf("ք", "ա", "տ", "և", "ր", "չ", "ճ", "օ", "պ"),
        listOf("ու", "ի", "ե", "ը", "ա", "ս", "դ", "ֆ", "գ", "հ", "յ"),
        listOf("զ", "ղ", "ց", "վ", "բ", "ն", "մ", "լ")
    )

    private val enRows = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("z", "x", "c", "v", "b", "n", "m")
    )

    override fun onCreate() {
        super.onCreate()
        // Загружаем аудио из res/raw/secret_sound.mp3
        mediaPlayer = MediaPlayer.create(this, R.raw.secret_sound)
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)

        row1 = view.findViewById(R.id.row_1)
        row2 = view.findViewById(R.id.row_2)
        row3 = view.findViewById(R.id.row_3)
        btnLanguage = view.findViewById(R.id.btn_language)

        val btnDelete = view.findViewById<Button>(R.id.btn_delete)
        val btnSpace = view.findViewById<Button>(R.id.btn_space)
        val btnEnter = view.findViewById<Button>(R.id.btn_enter)
        val btnComma = view.findViewById<Button>(R.id.btn_comma)
        val btnDot = view.findViewById<Button>(R.id.btn_dot)

        // Обработчики служебных клавиш
        btnDelete.setOnClickListener {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }

        btnSpace.setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
        }

        btnComma.setOnClickListener {
            currentInputConnection?.commitText(",", 1)
        }

        btnDot.setOnClickListener {
            currentInputConnection?.commitText(".", 1)
        }

        btnEnter.setOnClickListener {
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }

        // Кнопка глобуса для переключения языков
        btnLanguage.setOnClickListener {
            toggleLanguage()
        }

        // Первоначальное построение раскладки
        updateKeyboardLayout()

        return view
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

        val activeRows = when (currentLanguage) {
            Language.RU -> ruRows
            Language.HY -> hyRows
            Language.EN -> enRows
        }

        populateRow(row1, activeRows[0])
        populateRow(row2, activeRows[1])
        populateRow(row3, activeRows[2])
    }

    private fun populateRow(rowLayout: LinearLayout, keys: List<String>) {
        for (keyText in keys) {
            val button = Button(this).apply {
                text = keyText
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1.0f
                ).apply {
                    setMargins(2, 2, 2, 2)
                }
                setBackgroundColor(Color.parseColor("#333333"))
                setTextColor(Color.WHITE)
                textSize = 16f
                setOnClickListener {
                    currentInputConnection?.commitText(keyText, 1)
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
                // Включаем аудио-предупреждение
                mediaPlayer?.seekTo(0)
                mediaPlayer?.start()

                // Мгновенно стираем весь набранный текст
                ic.deleteSurroundingText(textBeforeCursor.length, 0)
                break
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}