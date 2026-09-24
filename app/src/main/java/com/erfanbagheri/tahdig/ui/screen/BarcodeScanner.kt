package com.erfanbagheri.tahdig.ui.screen

import android.Manifest
import java.util.concurrent.Executors
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.content.ContextCompat
import com.erfanbagheri.tahdig.ui.theme.YekanBakh
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * CameraX + ML Kit preview (#116).
 *
 * The ML Kit model is the BUNDLED one, so detection works with no download on
 * first use — an offline-first app must not need a network round trip to open
 * a camera.
 *
 * Permission is requested here, at the point of use, rather than at launch:
 * the AC asks for priming, and asking for the camera the moment the app opens
 * is the pattern Android users deny out of habit.
 */
@Composable
fun BarcodeCameraPreview(
    onBarcode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    // Re-read on every RESUME, not just at first composition: the permission
    // can be granted from system settings while this screen sits in the
    // back stack, and a remembered initial value would keep showing the
    // priming copy forever with a camera the app already has.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Priming copy first, so the OS dialog is never the first thing the user
    // reads about why we want the camera.
    if (!granted) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "برای خواندن بارکد، دسترسی به دوربین لازم است.",
                fontFamily = YekanBakh,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp),
            )
            androidx.compose.material3.TextButton(
                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("اجازه می‌دهم", fontFamily = YekanBakh)
            }
        }
        return
    }

    val scanner = remember { BarcodeScanning.getClient() }
    // Guards the analyzer: ML Kit delivers frames faster than a lookup can
    // finish, and without this the same barcode fires a dozen lookups.
    var busy by remember { mutableStateOf(false) }
    // Set from the analyzer thread; only ever flipped false -> true, so a plain
    // flag is enough and a racy double-log is harmless.
    var framesLogged by remember { mutableStateOf(false) }
    // Created in factory(), so it cannot be a remember{} above (the factory
    // closes over this composable's state). Set before AndroidView composes.
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            // COMPATIBLE (TextureView) rather than the default PERFORMANCE
            // (SurfaceView): a SurfaceView nested in a scrolling/overlapping
            // Compose container renders as a BLACK rectangle, because the
            // surface is composited in a separate window that does not follow
            // the scroll. This preview lives inside a LazyColumn item.
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                // A dedicated single thread, NOT the main executor: ML Kit
                // inference is CPU work and blocking it on the main thread
                // stalls Compose. Single-threaded also gives the
                // STRATEGY_KEEP_ONLY_LATEST backpressure somewhere to land.
                analysis.setAnalyzer(analysisExecutor) { proxy ->
                    val media = proxy.image
                    if (media != null && !framesLogged) {
                        // One line proving the pipeline is live. Without it a
                        // black preview is indistinguishable from a preview
                        // whose frames never arrive at all.
                        framesLogged = true
                        android.util.Log.i(
                            "TahdigScanner",
                            "first frame " + media.width + "x" + media.height +
                                " rotation=" + proxy.imageInfo.rotationDegrees,
                        )
                    }
                    if (media == null || busy) {
                        proxy.close()
                    } else {
                        busy = true
                        scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                            .addOnSuccessListener { codes ->
                                val value = codes.firstNotNullOfOrNull { code ->
                                    code.rawValue?.takeIf { code.format == Barcode.FORMAT_EAN_13 ||
                                        code.format == Barcode.FORMAT_EAN_8 ||
                                        code.format == Barcode.FORMAT_UPC_A ||
                                        code.format == Barcode.FORMAT_UPC_E }
                                }
                                if (value != null) onBarcode(value)
                            }
                            .addOnCompleteListener {
                                // Release the frame and reopen the gate either way:
                                // a failed frame must not wedge the scanner shut.
                                busy = false
                                proxy.close()
                            }
                    }
                }
                provider.unbindAll()
                // NOT wrapped in runCatching: a swallowed bind failure is a
                // black rectangle with no clue why, which is exactly what this
                // bug was. Let it throw so logcat names the cause.
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}
