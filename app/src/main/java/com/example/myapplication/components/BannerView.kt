package com.example.myapplication.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator
import com.tbuonomo.viewpagerdotsindicator.compose.model.DotGraphic
import com.tbuonomo.viewpagerdotsindicator.compose.type.ShiftIndicatorType

private const val BANNER_PREFS = "banner_cache"
private const val BANNER_CACHE_KEY = "banner_urls"

@Composable
fun BannerView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(BANNER_PREFS, 0) }
    val cachedBanners = remember { prefs.getStringSet(BANNER_CACHE_KEY, emptySet())?.toList() ?: emptyList() }
    var bannerlist by remember { mutableStateOf(cachedBanners) }

    LaunchedEffect(Unit) {
        Firebase.firestore.collection("data")
            .document("banners")
            .get()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    // Permission/network issue: keep cached list; do not crash app.
                    return@addOnCompleteListener
                }

                val urls = task.result?.get("urls") as? List<*>
                val firestoreBanners = urls
                    ?.filterIsInstance<String>()
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()

                if (firestoreBanners.isNotEmpty()) {
                    bannerlist = firestoreBanners
                    prefs.edit().putStringSet(BANNER_CACHE_KEY, firestoreBanners.toSet()).apply()
                }
            }
    }
    // to display image on ui used any library suc as coil
    Column (){
        if (bannerlist.isEmpty()) {
            Spacer(modifier = Modifier.height(1.dp))
            return@Column
        }

        val pagerState = rememberPagerState(0) { bannerlist.size }
        HorizontalPager(state = pagerState,
            pageSpacing = 24.dp,
            modifier = Modifier.height(200.dp)) {

            AsyncImage(
                model = bannerlist.get(it),
                contentDescription = "banner",
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        com.tbuonomo.viewpagerdotsindicator.compose.DotsIndicator(dotCount = bannerlist.size,
            pagerState= pagerState,
            type = ShiftIndicatorType(DotGraphic(color = MaterialTheme.colorScheme.primary, size = 6.dp)))

    }

}