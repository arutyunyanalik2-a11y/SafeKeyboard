package com.example.safekeyboard

import android.inputmethodservice.InputMethodService
import android.media.MediaPlayer
import android.view.View
import android.widget.Button

class MyKeyboardService : InputMethodService() {

    private var mediaPlayer: MediaPlayer? = null

    // Список запрещенных слов или частей слов для мгновенной очистки
    private val triggerWords = listOf("трахает", "гей", "поза", "рака", "блять", "сука", "fuck")

    override fun onCreate() {
        super.onCreate()
        // Загружаем аудио из res/raw/secret_sound.mp3
        mediaPlayer = MediaPlayer.create(this, R.raw.secret_sound)
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)

        val btnTest = view.findViewById<Button>(R.id.btn_test)
        val btnDelete = view.findViewById<Button>(R.id.btn_delete)

        // Кнопка для теста вставки слова
        btnTest.setOnClickListener {
            val ic = currentInputConnection
            ic?.commitText("секрет", 1)
        }

        // Ручное стирание символа
        btnDelete.setOnClickListener {
            val ic = currentInputConnection
            ic?.deleteSurroundingText(1, 0)
        }

        return view
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

                // Мгновенно стираем весь текст, чтобы поиск не успел сработать
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