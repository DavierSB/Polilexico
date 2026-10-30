package app.lexico.wifi

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class RoomKind(internal val service: String, internal val port: Int) {
  CLASSIC("_lexico._tcp.", 47474),
  DUPLICATE("_lexicodup._tcp.", 47475),
  ENDGAME("_lexicofin._tcp.", 47476),
  ANAGRAM("_lexicoana._tcp.", 47477),
  SPRINT("_lexicospr._tcp.", 47478),
  RECALL("_lexicorec._tcp.", 47479),
}

data class Room(val name: String, val address: String, val port: Int)

internal class RoomAdvert(context: Context, private val kind: RoomKind) : NsdManager.RegistrationListener {
  private val nsd = context.getSystemService(NsdManager::class.java)

  fun register(name: String) {
    val info = NsdServiceInfo().apply { serviceName = name; serviceType = kind.service; port = kind.port }
    runCatching { nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, this) }
  }

  fun unregister() {
    runCatching { nsd.unregisterService(this) }
  }

  override fun onServiceRegistered(info: NsdServiceInfo) {}
  override fun onRegistrationFailed(info: NsdServiceInfo, code: Int) {}
  override fun onServiceUnregistered(info: NsdServiceInfo) {}
  override fun onUnregistrationFailed(info: NsdServiceInfo, code: Int) {}
}

class RoomFinder(context: Context, private val scope: CoroutineScope, private val kind: RoomKind) : NsdManager.DiscoveryListener {
  private val nsd = context.getSystemService(NsdManager::class.java)
  private val lock = context.applicationContext.getSystemService(WifiManager::class.java).createMulticastLock("lexico")
  private val found = Channel<NsdServiceInfo>(Channel.UNLIMITED)
  private val _rooms = MutableStateFlow<List<Room>>(emptyList())
  val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

  fun start() {
    runCatching { lock.acquire() }
    runCatching { nsd.discoverServices(kind.service, NsdManager.PROTOCOL_DNS_SD, this) }
    scope.launch { for (info in found) resolve(info)?.let(::add) }
  }

  fun stop() {
    runCatching { nsd.stopServiceDiscovery(this) }
    runCatching { if (lock.isHeld) lock.release() }
    found.close()
  }

  override fun onServiceFound(info: NsdServiceInfo) {
    found.trySend(info)
  }

  override fun onServiceLost(info: NsdServiceInfo) {
    _rooms.value = _rooms.value.filter { it.name != info.serviceName }
  }

  override fun onDiscoveryStarted(type: String) {}
  override fun onDiscoveryStopped(type: String) {}
  override fun onStartDiscoveryFailed(type: String, code: Int) {}
  override fun onStopDiscoveryFailed(type: String, code: Int) {}

  private fun add(room: Room) {
    _rooms.value = _rooms.value.filter { it.name != room.name } + room
  }

  @Suppress("DEPRECATION")
  private suspend fun resolve(info: NsdServiceInfo): Room? = suspendCancellableCoroutine { done ->
    val listener = object : NsdManager.ResolveListener {
      override fun onServiceResolved(resolved: NsdServiceInfo) {
        done.resume(resolved.host?.hostAddress?.let { Room(resolved.serviceName, it, resolved.port) })
      }

      override fun onResolveFailed(failed: NsdServiceInfo, code: Int) {
        done.resume(null)
      }
    }
    runCatching { nsd.resolveService(info, listener) }.onFailure { done.resume(null) }
  }
}
