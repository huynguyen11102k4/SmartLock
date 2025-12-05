package com.example.smartlock.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartlock.databinding.FragmentBleProvisionBinding
import com.example.smartlock.model.Door
import com.example.smartlock.utils.LockBleManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.util.Locale
import java.util.UUID

class BleProvisionFragment : Fragment() {
    private var _binding: FragmentBleProvisionBinding? = null
    private val binding get() = _binding!!

    private val LOCK_SERVICE_UUID =
        UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")

    private val btManager by lazy { requireContext().getSystemService(BluetoothManager::class.java) }
    private val btAdapter by lazy { btManager.adapter }
    private val bleScanner by lazy { btAdapter.bluetoothLeScanner }

    private var scanning = false
    private val devices = linkedMapOf<String, BluetoothDevice>()
    private val deviceNames = hashMapOf<String, String>()

    private val viewModel: DoorViewModel by viewModels {
        DoorViewModelFactory(requireContext().applicationContext)
    }

    private val ble by lazy { LockBleManager(requireContext().applicationContext) }

    private var selectedDevice: BluetoothDevice? = null
    private var selectedName: String? = null

    private fun hasPerm(p: String): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), p) == PackageManager.PERMISSION_GRANTED

    private fun safeDeviceAddress(dev: BluetoothDevice): String? = try {
        if (Build.VERSION.SDK_INT >= 31 && !hasPerm(Manifest.permission.BLUETOOTH_CONNECT)) null
        else dev.address
    } catch (_: SecurityException) { null }

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val okBleScan = result[Manifest.permission.BLUETOOTH_SCAN] ?: true
        val okBleConn = result[Manifest.permission.BLUETOOTH_CONNECT] ?: true
        val okFineLoc = result[Manifest.permission.ACCESS_FINE_LOCATION] ?: true

        val ok = when {
            Build.VERSION.SDK_INT >= 31 -> okBleScan && okBleConn
            else -> okFineLoc
        }

        if (ok) {
            if (Build.VERSION.SDK_INT < 31 ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED) {
                try {
                    startScan()
                } catch (e: SecurityException) {
                    toast("Thiếu quyền BLE: ${e.message}")
                }
            }
        } else toast("Thiếu quyền cần thiết cho BLE")
    }

    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT < 31 ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
            == PackageManager.PERMISSION_GRANTED) {
            try {
                startScan()
            } catch (e: SecurityException) {
                toast("Thiếu quyền BLE: ${e.message}")
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val dev = result?.device ?: return
            val advName = result.scanRecord?.deviceName ?: "Không rõ"
//            if (!advName.startsWith("SMARTLOCK")) return

            val mac = dev.address ?: return
            if (!devices.containsKey(mac)) {
                devices[mac] = dev
                deviceNames[mac] = advName
                renderList()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            toast("Scan lỗi: $errorCode")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBleProvisionBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = BleDeviceAdapter { name, mac ->
            val device = devices[mac] ?: return@BleDeviceAdapter
            stopScan()
            selectedDevice = device
            selectedName = name
            showWifiDialog()
        }
        binding.rvDevices.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDevices.adapter = adapter

        binding.btnScan.setOnClickListener { ensurePermsAndScan() }

        renderList = {
            (binding.rvDevices.adapter as BleDeviceAdapter).submitList(
                devices.values.map { dev ->
                    val mac = dev.address ?: "??:??:??:??:??:??"
                    val displayName = deviceNames[mac] ?: "SMARTLOCK"
                    displayName to mac
                }
            )
        }

        parentFragmentManager.setFragmentResultListener(
            WifiInputDialogFragment.REQ_WIFI_INPUT, viewLifecycleOwner
        ) { _, bundle ->
            val ssid = bundle.getString(WifiInputDialogFragment.KEY_SSID).orEmpty()
            val pass = bundle.getString(WifiInputDialogFragment.KEY_PASS).orEmpty()
            provisionWithWifi(ssid, pass)
        }

        ensurePermsAndScan()
    }

    private var renderList: () -> Unit = {}

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun ensurePermsAndScan() {
        if (!btAdapter.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtLauncher.launch(intent)
            return
        }

        if (Build.VERSION.SDK_INT >= 31) {
            val need = mutableListOf<String>()
            if (!hasPerm(Manifest.permission.BLUETOOTH_SCAN)) need += Manifest.permission.BLUETOOTH_SCAN
            if (!hasPerm(Manifest.permission.BLUETOOTH_CONNECT)) need += Manifest.permission.BLUETOOTH_CONNECT
            if (need.isNotEmpty()) {
                permLauncher.launch(need.toTypedArray())
                return
            }
            startScan()
        } else {
            if (!hasPerm(Manifest.permission.ACCESS_FINE_LOCATION)) {
                permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                return
            }
            startScan()
        }
    }

    @Suppress("MissingPermission")
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun startScan() {
        if (scanning) return
        if (Build.VERSION.SDK_INT >= 31 && !hasPerm(Manifest.permission.BLUETOOTH_SCAN)) return

        devices.clear()
        deviceNames.clear()
        renderList()

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(LOCK_SERVICE_UUID))
                .build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            bleScanner.startScan(null, settings, scanCallback)
            scanning = true
        } catch (_: SecurityException) {
            toast("Thiếu quyền quét BLE")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            delay(15_000)
            stopScan()
        }
    }

    @Suppress("MissingPermission")
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun stopScan() {
        if (!scanning) return
        if (Build.VERSION.SDK_INT >= 31 && !hasPerm(Manifest.permission.BLUETOOTH_SCAN)) {
            scanning = false
            return
        }
        try { bleScanner.stopScan(scanCallback) } catch (_: SecurityException) { }
        scanning = false
    }

    private fun showWifiDialog(prefillSsid: String? = null) {
        WifiInputDialogFragment.new(prefillSsid)
            .show(parentFragmentManager, "wifi_input")
    }

    private fun buildConfigJson(ssid: String, pass: String, topic: String) = """
        {
          "wifi_ssid": "$ssid",
          "wifi_pass": "$pass",
          "mqtt_broker": "d2684409b6644e89b97ca34b695085ae.s1.eu.hivemq.cloud:8883",
          "mqtt_user": "toikhonghai",
          "mqtt_pass": "Huy123456",
          "topic_prefix": "$topic",
          "version": 1
        }
        """.trimIndent()

    private fun provisionWithWifi(ssid: String, pass: String) {
        val dev = selectedDevice
        val name = selectedName ?: "SMARTLOCK"
        if (dev == null) { toast("Chưa chọn thiết bị BLE"); return }

        val mac = safeDeviceAddress(dev)
        if (mac == null) {
            toast("Thiếu quyền để lấy địa chỉ thiết bị")
            return
        }

        val topic = "door/${mac.replace(":", "").take(6).lowercase(Locale.ROOT)}"
        val json = buildConfigJson(ssid, pass, topic)

        ble.setConnectionObserver(object : ConnectionObserver {
            override fun onDeviceConnecting(device: BluetoothDevice) {
                toast("Đang kết nối...")
            }

            override fun onDeviceConnected(device: BluetoothDevice) {
                toast("Đã kết nối, đang khởi tạo...")
            }

            override fun onDeviceReady(device: BluetoothDevice) {
                toast("Thiết bị sẵn sàng, đang gửi cấu hình...")

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        // ===== CRITICAL: Đợi lâu hơn để đảm bảo BLE stack sẵn sàng =====
                        Log.d("BleProvision", "⏳ Waiting 1500ms before sending config...")
                        delay(1500)  // Tăng từ 500ms lên 1500ms

                        Log.d("BleProvision", "📤 Sending config now...")
                        val ok = ble.sendConfigAwaitAck(
                            json,
                            expect = "OK",
                            timeoutMs = 15_000  // Tăng timeout lên 15s
                        )

                        if (ok) {
                            Log.d("BleProvision", "✅ Config successful!")
                            val deviceMac = safeDeviceAddress(device) ?: mac
                            val door = Door(
                                id = deviceMac,
                                name = name,
                                permission = "Chủ sở hữu",
                                battery = 100,
                                macAddress = deviceMac,
                                mqttTopicPrefix = topic
                            )
                            viewModel.insertDoor(door)
                            ble.disconnect().enqueue()
                            toast("Cấu hình thành công!")
                            delay(1000)
                            findNavController().popBackStack()
                        } else {
                            Log.e("BleProvision", "❌ Device did not respond with ACK")
                            ble.disconnect().enqueue()
                            toast("Thiết bị không phản hồi ACK - Thử lại!")
                        }
                    } catch (se: SecurityException) {
                        Log.e("BleProvision", "❌ Security exception: ${se.message}")
                        ble.disconnect().enqueue()
                        toast("Thiếu quyền BLE: ${se.message}")
                    } catch (e: Exception) {
                        Log.e("BleProvision", "❌ Error: ${e.message}")
                        ble.disconnect().enqueue()
                        toast("Gửi cấu hình lỗi: ${e.message}")
                    }
                }
            }

            override fun onDeviceFailedToConnect(d: BluetoothDevice, reason: Int) {
                toast("Kết nối thất bại: $reason")
            }

            override fun onDeviceDisconnecting(device: BluetoothDevice) {}
            override fun onDeviceDisconnected(d: BluetoothDevice, reason: Int) {
                toast("Đã ngắt kết nối")
            }
        })

        try {
            if (Build.VERSION.SDK_INT < 31 || hasPerm(Manifest.permission.BLUETOOTH_CONNECT)) {
                ble.connectTo(dev)
                    .retry(2, 200)
                    .enqueue()
            } else {
                toast("Thiếu quyền kết nối BLE")
            }
        } catch (_: SecurityException) {
            toast("Thiếu quyền kết nối BLE")
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
