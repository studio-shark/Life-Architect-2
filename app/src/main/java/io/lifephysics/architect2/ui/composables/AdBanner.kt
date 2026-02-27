package io.lifephysics.architect2.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdmobBanner(modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                // TODO: Replace with your real Ad Unit ID before publishing:
                // ca-app-pub-7210557644472819/3041816755
                adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test ID
                setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        context,
                        screenWidthDp
                    )
                )
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
