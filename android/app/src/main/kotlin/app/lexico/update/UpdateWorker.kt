package app.lexico.update

import android.app.ActivityManager
import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal const val UPDATE_WORK = "update"
private const val VERSION_KEY = "version"
private const val PROGRESS_KEY = "progress"
private const val RETRY_MS = 5_000L

internal fun enqueueDownload(ctx: Context, version: String) {
  val request = OneTimeWorkRequestBuilder<UpdateWorker>()
    .setInputData(workDataOf(VERSION_KEY to version))
    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
    .build()
  WorkManager.getInstance(ctx).enqueueUniqueWork(UPDATE_WORK, ExistingWorkPolicy.KEEP, request)
}

internal fun downloadProgress(ctx: Context, info: WorkInfo?): Float? {
  if (info == null || info.state.isFinished) return null
  return info.progress.getFloat(PROGRESS_KEY, -1f).takeIf { it >= 0f } ?: partFraction(ctx)
}

class UpdateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
  private var percent = -1

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    val version = inputData.getString(VERSION_KEY) ?: return@withContext Result.failure()
    try {
      finish(version, download(version))
      Result.success()
    } catch (e: FatalDownload) {
      Result.failure()
    }
  }

  private suspend fun download(version: String): File {
    val part = partFile(applicationContext, version)
    discardUpdates(applicationContext, part)
    while (!attempt(version, part)) delay(RETRY_MS)
    return part
  }

  private fun attempt(version: String, part: File): Boolean =
    try {
      resumeDownload(apkUrl(version), part, ::report)
      true
    } catch (e: IOException) {
      false
    }

  private fun report(done: Long, total: Long) {
    if (isStopped) throw IOException("stopped")
    if (total <= 0) return
    val now = (done * 100 / total).toInt()
    if (now == percent) return
    if (percent < 0) saveTotal(applicationContext, total)
    percent = now
    setProgressAsync(workDataOf(PROGRESS_KEY to done.toFloat() / total))
  }

  private fun finish(version: String, part: File) {
    val apk = markReady(applicationContext, version, part)
    if (inForeground()) installApk(applicationContext, apk)
  }

  private fun inForeground(): Boolean {
    val state = ActivityManager.RunningAppProcessInfo().also(ActivityManager::getMyMemoryState)
    return state.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
  }
}
