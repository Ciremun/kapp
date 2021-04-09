package com.ciremun.kapp

import android.app.Activity
import android.os.AsyncTask
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket


class MainActivity : Activity() {
    private var messageField: EditText? = null
    private var passwordField: EditText? = null
    private var nicknameField: EditText? = null
    private var channelField: EditText? = null
    private var connectToChannelButton: Button? = null
    private var disconnectFromChannelButton: Button? = null
    private var log: TextView? = null
    private var socket: Socket? = null
    private var bwriter: BufferedWriter? = null
    private var breader: BufferedReader? = null
    private var channel: String? = null
    private var password: String? = null
    private var nickname: String? = null

    private val host = "irc.chat.twitch.tv"
    private val port = 6667

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        nicknameField = findViewById<View>(R.id.nicknameField) as EditText
        channelField = findViewById<View>(R.id.channelField) as EditText
        passwordField = findViewById<View>(R.id.passwordField) as EditText
        log = findViewById<View>(R.id.log) as TextView
        log?.movementMethod = ScrollingMovementMethod()
        messageField = findViewById<View>(R.id.mainEditText) as EditText
        connectToChannelButton = findViewById<View>(R.id.connectToChannelButton) as Button
        connectToChannelButton?.text = "Подключиться"
        disconnectFromChannelButton = findViewById<View>(R.id.disconnectFromChannelButton) as Button
        disconnectFromChannelButton?.text = "Отключиться"
        disconnectFromChannelButton?.visibility = View.GONE
    }

    // Перенаправить сообщение на поток принадлежащий интерфейсу
    private fun pprint(s: String?) {
        runOnUiThread { print(s) }
    }

    // Отправить сообщение на сервер
    private fun sendMessage(bw: BufferedWriter, s: String) {
        try {
            bw.write("$s\n")
            bw.flush()
        } catch (e: Exception) {
            pprint(e.toString())
        }
    }

    // Отправить сообщение на сервер из поля ввода
    fun sendText(v: View) {
        if (messageField!!.text.toString().isNotEmpty()) {
            MessageSender("PRIVMSG $channel :${messageField!!.text}").execute()
            messageField!!.setText("")
        }
    }

    // Вывести сообщение на экран
    fun print(text: String?) {
        log!!.append("\n$text")
    }

    // Вызывается нажатием кнопки в приложении
    fun connectToChannel(v: View) {
        if (passwordField?.text?.isEmpty() == true)
        {
            pprint("Ошибка: Введите пароль")
            return
        }
        if (nicknameField?.text?.isEmpty() == true)
        {
            pprint("Ошибка: Введите имя пользователя")
            return
        }
        if (channelField?.text?.isEmpty() == true)
        {
            pprint("Ошибка: Введите канал")
            return
        }
        password = passwordField?.text.toString()
        nickname = nicknameField?.text.toString()
        channel = channelField?.text.toString()

        channelField?.text?.clear()

        passwordField?.visibility = View.GONE
        nicknameField?.visibility = View.GONE
        channelField?.visibility = View.GONE
        connectToChannelButton?.visibility = View.GONE
        disconnectFromChannelButton?.visibility = View.VISIBLE
        Connect().execute()
    }

    fun disconnectFromChannel(v: View)
    {
        passwordField?.visibility = View.VISIBLE
        nicknameField?.visibility = View.VISIBLE
        channelField?.visibility = View.VISIBLE
        connectToChannelButton?.visibility = View.VISIBLE
        disconnectFromChannelButton?.visibility = View.GONE
        MessageSender("PART $channel").execute()
    }

    // Отправить сообщение на сервер при помощи Асинхронной задачи
    inner class MessageSender(private var msg: String) : AsyncTask<String?, Void?, Void?>() {

        override fun doInBackground(vararg params: String?): Void? {
            bwriter?.let { sendMessage(it, msg) }
            return null
        }

    }

    // Подключиться к серверу
    inner class Connect : AsyncTask<Void?, Void?, Void?>() {

        override fun doInBackground(p1: Array<Void?>): Void? {
            try {

                if (socket == null)
                {
                    socket = Socket(host, port)
                    breader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                    bwriter = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))
                }

                bwriter?.let { sendMessage(it, "PASS $password") }
                pprint("Пароль отправлен")
                bwriter?.let { sendMessage(it, "NICK $nickname") }
                pprint("Имя пользователя отправлено")
                bwriter?.let { sendMessage(it, "JOIN $channel") }
                pprint("Подключение к $channel")
                breader?.let { bwriter?.let { it1 -> Listen(it, it1).start() } }
            } catch (e: Exception) {
                pprint(e.toString())
            }
            return null
        }

    }

    // Ответить на пинг от сервера или показать полученное сообщение
    inner class Listen(private var `in`: BufferedReader, private var out: BufferedWriter) :
        Thread() {
        private var line: String? = null

        override fun run() {
            try {
                while (`in`.readLine().also { line = it } != null) {
                    if (line!!.startsWith("PING")) {
                        sendMessage(out, "PONG ${line!!.substring(5)}")
                    } else {
                        pprint(line)
                    }
                }
            } catch (e: Exception) {
                pprint(e.toString())
            }
        }

    }
}