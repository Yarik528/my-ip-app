package com.example.myipapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var tvIp: TextView
    private lateinit var btnCopy: Button
    private lateinit var btnRefresh: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvIp = findViewById(R.id.tvIp)
        btnCopy = findViewById(R.id.btnCopy)
        btnRefresh = findViewById(R.id.btnRefresh)

        fetchIpAddress()
        checkForUpdate()

        btnCopy.setOnClickListener {
            val ip = tvIp.text.toString()
            if (ip != "Загрузка..." && !ip.startsWith("Ошибка") && ip != "Нет интернета") {
                copyToClipboard(ip)
            } else {
                Toast.makeText(this, "Сначала дождитесь загрузки IP", Toast.LENGTH_SHORT).show()
            }
        }

        btnRefresh.setOnClickListener {
            tvIp.text = "Обновление..."
            fetchIpAddress()
        }
    }

    private fun fetchIpAddress() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://api.ipify.org/")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val ip = connection.inputStream.bufferedReader().readText().trim()
                    withContext(Dispatchers.Main) {
                        tvIp.text = ip
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        tvIp.text = "Ошибка сервера ($responseCode)"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    tvIp.text = "Нет интернета"
                }
            }
        }
    }

    private fun checkForUpdate() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ссылка на raw-файл latest_version.txt в твоем репозитории
                val versionUrl = URL("https://raw.githubusercontent.com/ТВОЙ_НИК/my-ip-app/main/latest_version.txt")
                val connection = versionUrl.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val remoteVersion = connection.inputStream.bufferedReader().readText().trim().toIntOrNull() ?: 0
                    val currentVersion = getString(R.string.current_version).toIntOrNull() ?: 0

                    if (remoteVersion > currentVersion) {
                        withContext(Dispatchers.Main) {
                            showUpdateDialog(remoteVersion)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace() // Тихо игнорируем ошибки проверки обновлений
            }
        }
    }

    private fun showUpdateDialog(newVersion: Int) {
        AlertDialog.Builder(this)
            .setTitle("Доступно обновление!")
            .setMessage("Вышла новая версия $newVersion. Хотите скачать?")
            .setPositiveButton("Скачать") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, 
                    Uri.parse("https://github.com/ТВОЙ_НИК/my-ip-app/releases/latest"))
                startActivity(intent)
            }
            .setNegativeButton("Позже", null)
            .show()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("IP Address", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "IP скопирован!", Toast.LENGTH_SHORT).show()
    }
}
