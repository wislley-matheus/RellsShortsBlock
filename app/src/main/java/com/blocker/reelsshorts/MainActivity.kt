package com.blocker.reelsshorts

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import com.blocker.reelsshorts.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.switchInstagram.isChecked = PrefsManager.isInstagramBlockEnabled(this)
        binding.switchYoutube.isChecked = PrefsManager.isYoutubeBlockEnabled(this)

        binding.switchInstagram.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.setInstagramBlockEnabled(this, isChecked)
        }
        binding.switchYoutube.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.setYoutubeBlockEnabled(this, isChecked)
        }

        binding.btnOpenSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnResetStats.setOnClickListener {
            PrefsManager.resetStats(this)
            refreshStats()
        }

        refreshStats()
    }

    override fun onResume() {
        super.onResume()
        updateStatusText()
        refreshStats()
    }

    private fun refreshStats() {
        binding.tvStatsReels.text =
            getString(R.string.stats_reels, PrefsManager.getReelsBlockedCount(this))
        binding.tvStatsShorts.text =
            getString(R.string.stats_shorts, PrefsManager.getShortsBlockedCount(this))
    }

    private fun updateStatusText() {
        val enabled = isAccessibilityServiceEnabled()
        binding.tvStatus.text = if (enabled) {
            getString(R.string.status_enabled)
        } else {
            getString(R.string.status_disabled)
        }
        binding.tvStatus.setTextColor(
            if (enabled) getColor(R.color.green_ok) else getColor(R.color.red_off)
        )
    }

    /**
     * Verifica, via Settings.Secure, se o nosso AccessibilityService está
     * na lista de serviços de acessibilidade habilitados pelo usuário.
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "$packageName/${BlockerAccessibilityService::class.java.name}"
        val enabledServicesSetting = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
