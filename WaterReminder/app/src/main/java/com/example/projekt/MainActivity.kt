package com.example.projekt

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.preference.PreferenceManager
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import java.util.*
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.*
import android.text.InputType

class MainActivity : AppCompatActivity() {

    private lateinit var alarmManager: AlarmManager
    private lateinit var vibrator: Vibrator
    private lateinit var editTextWaterAmount: EditText
    private lateinit var buttonAddWater: Button
    private lateinit var textViewWaterProgress: TextView

    private var waterConsumed: Int = 0
    private var progresso: Int = 0
    private var targetWaterAmount: Int = 0
    val notificationChannelId = "WaterReminderChannel"


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main);
        supportActionBar?.title = "Water Reminder"
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isTargetWaterAmountSet = sharedPreferences.contains("targetWaterAmount")
        if (!isTargetWaterAmountSet) {
           waterChange()
        }
        editTextWaterAmount = findViewById(R.id.editTextWaterAmount)
        buttonAddWater = findViewById(R.id.buttonAddWater)
        val mainLayout = findViewById<View>(R.id.mainLayout)
        editTextWaterAmount.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                addWater()
                hideKeyboard()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        mainLayout.setOnTouchListener { _, _ ->
            hideKeyboard()
            false
        }
        buttonAddWater.setOnClickListener {
            if (vibrator.hasVibrator()) {
                val vibrationEffect =
                    VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(vibrationEffect)
            }
            addWater()
        }
        createNotificationChannel()
        updateCircle()
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        midNightReset()
        val isZeroWaterAction = intent.getBooleanExtra("ACTION_ZERO_WATER", false)
        if (isZeroWaterAction) {
            zeroWater()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        vibrator.cancel()
    }
    private fun setWaterReminderAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, WaterReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Ustaw alarm na godzine
        val alarmTime = System.currentTimeMillis() + (1000*60*60)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                alarmTime,
                pendingIntent
            )
        }
    }

    private fun midNightReset() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, WaterReminderBroadcastReceiver::class.java)
        intent.action = "ACTION_ZERO_WATER"
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, 1)
        }
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

private fun updateCircle(){
    val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    waterConsumed = sharedPreferences.getInt("waterConsumed", 0)
    progresso = sharedPreferences.getInt("progress",0)
    targetWaterAmount = sharedPreferences.getInt("targetWaterAmount",0)
    textViewWaterProgress = findViewById(R.id.textViewProgres)
    val progressText = String.format("%d%%\n%d ml / %d ml", progresso, waterConsumed, targetWaterAmount)
    textViewWaterProgress.text = progressText
    val circularProgressBar = findViewById<CircularProgressBar>(R.id.circularProgressBar)
    circularProgressBar.apply {
        progressMax = 100f
        setProgressWithAnimation(progresso.toFloat(), 800)
        progressBarWidth = 10f
        progressBarColorStart = Color.BLUE
        progressBarColorEnd = Color.CYAN
        progressBarColorDirection = CircularProgressBar.GradientDirection.TOP_TO_BOTTOM
    }
}
    private fun updateWaterProgress() {
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        targetWaterAmount = sharedPreferences.getInt("targetWaterAmount",0)
        waterConsumed = sharedPreferences.getInt("waterConsumed",0)

        val progress = (waterConsumed.toDouble() / targetWaterAmount.toDouble() * 100).toInt()
        if(progress>=100&&over100==0){
            over100++
            playSound("noice")
        }
        editor.putInt("progress", progress)
        editor.apply()
        editor.putInt("targetWaterAmount", targetWaterAmount)
        editor.apply()
        updateCircle()
        setWaterReminderAlarm()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editTextWaterAmount.windowToken, 0)
    }
    private fun addWater() {
        if (editTextWaterAmount.text.toString().isEmpty()) {
            editTextWaterAmount.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editTextWaterAmount, InputMethodManager.SHOW_IMPLICIT)
        }

        val input = editTextWaterAmount.text.toString().toIntOrNull()
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()

        if (input != null && input > 0) {
            playSound("shield")
            waterConsumed += input
            editor.putInt("waterConsumed", waterConsumed)
            editor.apply()
            updateWaterProgress()
        }
        editTextWaterAmount.text.clear()

        if (vibrator.hasVibrator()) {
            val vibrationEffect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(vibrationEffect)
        }
    }
    private var over100=0;
    private fun createNotificationChannel() {
        val channelName = "Water Reminder"
        val channelDescription = "Wypij prosze wode"
        val importance = NotificationManager.IMPORTANCE_DEFAULT

        val channel =
            NotificationChannel(notificationChannelId, channelName, importance).apply {
                description = channelDescription
            }

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private var mMediaPlayer: MediaPlayer? = null
   private fun playSound(soundName: String) {
        val soundResourceId = resources.getIdentifier(soundName, "raw", packageName)
        val mediaPlayer = MediaPlayer.create(this, soundResourceId)
        mediaPlayer.start()
    }

    private fun waterChange() {
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val currentTargetWaterAmount = sharedPreferences.getInt("targetWaterAmount", 0)
        val editTextTargetWaterAmount = EditText(this)
        editTextTargetWaterAmount.inputType = InputType.TYPE_CLASS_NUMBER 
        editTextTargetWaterAmount.hint = "Zmień swój cel"
        AlertDialog.Builder(this)
            .setTitle("Zmień swój cel")
            .setView(editTextTargetWaterAmount)
            .setPositiveButton("OK") { _, _ ->
                val newTargetWaterAmountText = editTextTargetWaterAmount.text.toString()
                val newTargetWaterAmount = newTargetWaterAmountText.toIntOrNull()

                if (newTargetWaterAmount != null) {
                    sharedPreferences.edit()
                        .putInt("targetWaterAmount", newTargetWaterAmount)
                        .apply()
                    updateWaterProgress()
                } else {
                    Toast.makeText(this, "Nieprawidłowa wartość", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }
    override fun onStop() {
        super.onStop()
        if (mMediaPlayer != null) {
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_change_target_water_amount -> {
               waterChange()
                return true
            }
            R.id.clear_water -> {
                zeroWater()
                return true
            }
            R.id.notify -> {
                showNotification(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
   private fun zeroWater(){
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPreferences.edit()
        editor.putInt("waterConsumed", 0)
        editor.apply()
        updateWaterProgress()
    }
    companion object {
        const val notificationChannelId = "WaterReminderChannel"
        private const val NOTIFICATION_INTERVAL_MINUTES = 1
        fun showNotification(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
            val notifications = listOf(
                "Walnij se gula mordo",
                "Ze mną sie nie napijesz?",
                "Weź cos wypij bo zasłabniesz",
                "Prosze wypij coś",
                "Brat nie pije wody"
            )
            val random = Random()
            val randomIndex = random.nextInt(notifications.size)

            // Pobieranie losowego powiadomienia z listy
            val randomNotification = notifications[randomIndex]

            // Tworzenie budowniczego powiadomienia
            val notificationBuilder = NotificationCompat.Builder(context, MainActivity.notificationChannelId)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("We se gulnij")
                .setContentText(randomNotification)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            // Pobieranie obiektu NotificationManagerCompat
            val notificationManager = NotificationManagerCompat.from(context)

            // Generowanie losowego identyfikatora powiadomienia
            val notificationId = random.nextInt()

            // Wywołanie metody notify() z wygenerowanym identyfikatorem powiadomienia
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }
}



