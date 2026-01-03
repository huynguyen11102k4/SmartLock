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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.example.smartlock.data.AppDatabase
import com.example.smartlock.databinding.ActivityMainBinding
import com.example.smartlock.utils.AuthManager
import com.example.smartlock.utils.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var navController: NavController

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        if (tokenManager.getAccessToken() != null) {
            navController.navigate(R.id.doorListFragment)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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