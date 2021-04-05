package com.ciremun.kapp

import android.app.Activity
import android.opengl.Visibility
import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
    private var et: EditText? = null
    private var log: TextView? = null
    private var socket: Socket? = null
    private var bwriter: BufferedWriter? = null
    private var breader: BufferedReader? = null

    private val host = "irc.chat.twitch.tv"
    private val port = 6667
    private val channel = "#tsoding"
    private val nickname = "shtcd"
    private var password = ""

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val passwordField = findViewById<View>(R.id.passwordField) as EditText
        passwordField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int)
            {
                password = passwordField.text.toString()
                passwordField.visibility = View.GONE
            }
        })
        log = findViewById<View>(R.id.log) as TextView
        et = findViewById<View>(R.id.mainEditText) as EditText
        (findViewById<View>(R.id.connectToChannelButton) as Button).text = "Подключиться к $channel"
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
        if (et!!.text.toString().isNotEmpty()) {
            MessageSender(et!!.text.toString()).execute()
            et!!.setText("")
        }
    }

    // Вывести сообщение на экран
    fun print(text: String?) {
        log!!.text = "${log!!.text}\n>>> $text\n"
    }

    // Вызывается нажатием кнопки в приложении
    fun connectToChannel(v: View) {
        Connect().execute()
    }

    // Отправить сообщение на сервер при помощи Асинхронной задачи
    inner class MessageSender(private var msg: String) : AsyncTask<String?, Void?, Void?>() {

        override fun doInBackground(vararg params: String?): Void? {
            bwriter?.let { sendMessage(it, "PRIVMSG $channel :$msg") }
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
                    bwriter = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))
                    breader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                }

                if (password.isEmpty())
                {
                    pprint("Ошибка: Введите пароль")
                    return null
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