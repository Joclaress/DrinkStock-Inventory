package com.example.cokeinventory.util

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * AdMob Configuration & Ban Prevention Safeguards
 *
 * POLICY & BAN PREVENTION RULES:
 * 1. Test Ads in Debug Mode: Automatically forces official Google Test Ad Unit IDs
 *    whenever running a debug build. NEVER click real ads on your device.
 * 2. Proper Lifecycle Management: Destroys AdView on composition disposal to prevent memory leaks and invalid impressions.
 * 3. Rewarded Video Protection: Uses FullScreenContentCallback to clean up ad instances and handle dismissals.
 * 4. Production Ready: Put your real AdMob Ad Unit IDs in realBannerId and realRewardedId before publishing.
 */
object AdMobConfig {
    // Official Google Test Ad Unit IDs (Guarantees safety against AdMob bans during dev/testing)
    const val SAMPLE_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111"
    const val REWARDED_VIDEO_TEST_ID = "ca-app-pub-3940256099942544/5224354917"

    // Set your real AdMob Unit IDs here when publishing to Google Play
    var realBannerId: String? = "ca-app-pub-4394473472808077/1830570842"
    var realRewardedId: String? = "ca-app-pub-4394473472808077/6891325837"

    /**
     * Checks if the app is running in debug mode to prevent accidental real ad loads/clicks.
     */
    fun isDebugBuild(context: Context): Boolean {
        return try {
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (_: Exception) {
            true // default to safe debug mode on error
        }
    }

    /**
     * Returns the appropriate Banner Ad Unit ID based on build mode.
     */
    fun getBannerAdUnitId(context: Context): String {
        return if (isDebugBuild(context) || realBannerId.isNullOrBlank()) {
            BANNER_TEST_ID
        } else {
            realBannerId!!
        }
    }

    /**
     * Returns the appropriate Rewarded Video Ad Unit ID based on build mode.
     */
    fun getRewardedAdUnitId(context: Context): String {
        return if (isDebugBuild(context) || realRewardedId.isNullOrBlank()) {
            REWARDED_VIDEO_TEST_ID
        } else {
            realRewardedId!!
        }
    }
}

object AdMobManager {
    private var isInitialized = false

    fun initialize(context: Context) {
        if (!isInitialized) {
            try {
                // Configure test device request configuration to protect developer account
                val testDeviceIds: List<String> = listOf(AdRequest.DEVICE_ID_EMULATOR)
                val requestConfiguration = RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build()
                MobileAds.setRequestConfiguration(requestConfiguration)

                MobileAds.initialize(context) { status ->
                    Log.d("AdMobManager", "AdMob MobileAds initialized: ${status.adapterStatusMap}")
                }
                isInitialized = true
            } catch (e: Exception) {
                Log.e("AdMobManager", "AdMob init error: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Loads a Rewarded Video Ad with automatic test mode fallback.
     */
    fun loadRewardedAd(
        context: Context,
        onAdLoaded: (RewardedAd) -> Unit,
        onAdFailed: (String) -> Unit
    ) {
        initialize(context)
        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdMobConfig.getRewardedAdUnitId(context)

        Log.d("AdMobManager", "Loading Rewarded Ad with Unit ID: $adUnitId")

        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    Log.d("AdMobManager", "Rewarded ad loaded successfully.")
                    onAdLoaded(rewardedAd)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e("AdMobManager", "Rewarded ad failed to load: ${loadAdError.message}")
                    onAdFailed(loadAdError.message)
                }
            }
        )
    }

    /**
     * Shows a loaded Rewarded Video Ad with full lifecycle callbacks to prevent policy violations.
     */
    fun showRewardedAd(
        activity: Activity,
        rewardedAd: RewardedAd,
        onUserRewarded: () -> Unit,
        onAdDismissed: (() -> Unit)? = null,
        onAdFailedToShow: ((String) -> Unit)? = null
    ) {
        rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d("AdMobManager", "Rewarded ad dismissed by user.")
                onAdDismissed?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e("AdMobManager", "Rewarded ad failed to show: ${adError.message}")
                onAdFailedToShow?.invoke(adError.message)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("AdMobManager", "Rewarded ad displayed on screen.")
            }
        }

        rewardedAd.show(activity) { rewardItem ->
            Log.d("AdMobManager", "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            onUserRewarded()
        }
    }
}

/**
 * Compose Banner Ad View with strict lifecycle management.
 * Destroys the AdView on dispose to prevent memory leaks and invalid impressions.
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier
) {
    var adViewInstance by remember { mutableStateOf<AdView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            adViewInstance?.destroy()
            adViewInstance = null
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    AdMobManager.initialize(ctx)
                    AdView(ctx).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = AdMobConfig.getBannerAdUnitId(ctx)
                        adListener = object : AdListener() {
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Log.e("BannerAdView", "Banner ad failed to load: ${error.message}")
                            }

                            override fun onAdImpression() {
                                Log.d("BannerAdView", "Banner ad impression recorded.")
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                        adViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
