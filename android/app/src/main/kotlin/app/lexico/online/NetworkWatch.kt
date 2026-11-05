package app.lexico.online

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network

internal class NetworkWatch(context: Context) {
  private val manager = context.getSystemService(ConnectivityManager::class.java)

  fun watch(onLost: () -> Unit, onBack: (switched: Boolean) -> Unit): () -> Unit {
    val callback = callback(onLost, onBack)
    runCatching { manager.registerDefaultNetworkCallback(callback) }
    return { runCatching { manager.unregisterNetworkCallback(callback) } }
  }

  private fun callback(onLost: () -> Unit, onBack: (Boolean) -> Unit) = object : ConnectivityManager.NetworkCallback() {
    @Volatile private var current: Network? = null

    override fun onAvailable(network: Network) {
      val switched = current != null && current != network
      current = network
      onBack(switched)
    }

    override fun onLost(network: Network) {
      if (network != current) return
      current = null
      onLost()
    }
  }
}
