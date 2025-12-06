package com.example.smartlock

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smartlock.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        startMqttService()
//        requestIgnoreBatteryOptimizations()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun startMqttService(){
        val intent = Intent(this, MqttService::class.java)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            Log.d("MainActivity", "Starting MqttService as foreground service")
            startForegroundService(intent)
        } else {
            Log.d("MainActivity", "Starting MqttService as background service")
            startService(intent)
        }
    }

    private fun requestIgnoreBatteryOptimizations(){
        val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
        if(!pm.isIgnoringBatteryOptimizations(packageName)){
            AlertDialog.Builder(this)
                .setTitle("Bỏ qua tối ưu pin")
                .setMessage("Để đảm bảo kết nối ổn định, vui lòng bỏ qua tối ưu pin cho ứng dụng này.")
                .setPositiveButton("Đồng ý") { _, _ ->
                    val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                }
                .setNegativeButton("Hủy", null)
                .setCancelable(true)
                .show()
        }
    }
}