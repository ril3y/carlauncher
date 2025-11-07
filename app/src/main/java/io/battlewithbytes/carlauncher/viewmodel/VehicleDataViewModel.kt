package io.battlewithbytes.carlauncher.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.battlewithbytes.carlauncher.model.*
import io.battlewithbytes.carlauncher.service.CanBusService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for accessing vehicle data from CAN bus service
 * Provides reactive streams of vehicle data to UI components
 */
class VehicleDataViewModel(application: Application) : AndroidViewModel(application) {

    private var canBusService: CanBusService? = null
    private var bound = false

    private val _batteryData = MutableStateFlow<BatteryData?>(null)
    val batteryData: StateFlow<BatteryData?> = _batteryData.asStateFlow()

    private val _motorData = MutableStateFlow<MotorData?>(null)
    val motorData: StateFlow<MotorData?> = _motorData.asStateFlow()

    private val _vehicleStatus = MutableStateFlow<VehicleStatus?>(null)
    val vehicleStatus: StateFlow<VehicleStatus?> = _vehicleStatus.asStateFlow()

    private val _connectionState = MutableStateFlow(CanBusService.ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<CanBusService.ConnectionState> = _connectionState.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CanBusService.CanBusServiceBinder
            canBusService = binder.getService()
            bound = true
            observeServiceData()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            canBusService = null
            bound = false
        }
    }

    init {
        startAndBindService()
    }

    private fun startAndBindService() {
        val context = getApplication<Application>()
        val intent = Intent(context, CanBusService::class.java)

        // Start service as foreground service
        context.startForegroundService(intent)

        // Bind to service
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun observeServiceData() {
        val service = canBusService ?: return

        viewModelScope.launch {
            service.batteryData.collect { data ->
                _batteryData.value = data
            }
        }

        viewModelScope.launch {
            service.motorData.collect { data ->
                _motorData.value = data
            }
        }

        viewModelScope.launch {
            service.vehicleStatus.collect { data ->
                _vehicleStatus.value = data
            }
        }

        viewModelScope.launch {
            service.connectionState.collect { state ->
                _connectionState.value = state
            }
        }
    }

    /**
     * Send a CAN message to the bus
     */
    suspend fun sendCanMessage(id: Int, data: ByteArray): Boolean {
        return canBusService?.sendCanMessage(id, data) ?: false
    }

    override fun onCleared() {
        super.onCleared()
        if (bound) {
            getApplication<Application>().unbindService(serviceConnection)
            bound = false
        }
    }
}
