package com.universopinol.store.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.universopinol.store.MainActivity
import com.universopinol.store.R
import com.universopinol.store.data.Repository
import com.universopinol.store.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Revisa el catálogo cada pocas horas y avisa si hay apps para actualizar. */
class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repo = Repository(applicationContext)
        val catalog = runCatching { repo.fetch() }.getOrElse { return@withContext Result.retry() }
        val updates = Store.updates(catalog, repo.installed(catalog))
        val key = updates.joinToString(",") { "${it.id}:${it.versionCode}" }
        val prefs = applicationContext.getSharedPreferences("store", Context.MODE_PRIVATE)
        if (updates.isNotEmpty() && key != prefs.getString("notified", "")) {
            notify(applicationContext, updates.joinToString(", ") { "${it.name} ${it.versionName}" }, updates.size)
            prefs.edit().putString("notified", key).apply()
        }
        Result.success()
    }

    companion object {
        private const val CHANNEL = "updates"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<UpdateWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("revisar-actualizaciones", ExistingPeriodicWorkPolicy.KEEP, req)
        }

        private fun notify(context: Context, names: String, count: Int) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Actualizaciones", NotificationManager.IMPORTANCE_DEFAULT))
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val n = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(if (count == 1) "Hay una actualización" else "Hay $count actualizaciones")
                .setContentText(names)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(1, n)
        }
    }
}
