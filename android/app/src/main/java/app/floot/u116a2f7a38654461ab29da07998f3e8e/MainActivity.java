package app.floot.u116a2f7a38654461ab29da07998f3e8e;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    // FLOOT_EDGE_TO_EDGE_INSETS
    // Android 15 (API 35) forces edge-to-edge and target SDK 36 ignores the
    // opt-out, so the WebView is laid out behind the status and navigation
    // bars. Capacitor cannot fix it for us here: StatusBar.overlaysWebView is
    // a no-op on API 35+, and the SystemBars inset pipeline only arms when the
    // page ships viewport-fit=cover, which the Floot shell does not emit in
    // inset mode (an app may still inject its own such meta — see below).
    // Without this the top strip of the app is hidden AND unpressable, because
    // touches over the status bar go to the system window.
    //
    // Inset the content view instead — the same layout iOS gives us by
    // default. Guarded to API 35+: below that overlaysWebView still works, and
    // padding here as well would inset the app twice.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < 35) {
            return;
        }

        final View content = findViewById(android.R.id.content);
        if (content == null) {
            return;
        }

        // The padding exposes a strip of window background on each inset edge.
        // Paint it the project's configured system-bar color (black unless
        // set) so it reads as part of the system bars whatever the app theme
        // is; the bar icon style is set to contrast with it via the SystemBars
        // plugin config. iOS paints the same color above its status bar.
        content.setBackgroundColor(Color.parseColor("#000000"));

        // The androidx Compat wrappers rather than the framework WindowInsets
        // API: the latter is all API 30+ against a minSdk of 26, and lint's
        // NewApi check does not reliably follow the SDK_INT guard above into a
        // lambda body — a false positive there is fatal, because
        // lintVitalRelease runs as part of every release build. The Compat
        // calls are safe on every API level, so the check never applies.
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            // The keyboard overlays an edge-to-edge window too (adjustResize is
            // ignored once the decor stops fitting system windows), so pad by
            // whichever bottom inset is larger.
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            final int bottomInset = Math.max(bars.bottom, ime.bottom);
            view.setPadding(bars.left, bars.top, bars.right, bottomInset);
            // Hand descendants the insets we already consumed — EXACTLY what
            // was padded above, bottomInset included. Capacitor's SystemBars
            // plugin puts its own listener on the WebView's parent, and once
            // the page carries viewport-fit=cover (Floot's shell does not emit
            // it in inset mode, but an app that injects its own viewport meta
            // for safe-area CSS does) that listener margins the parent by the
            // FULL ime inset. Leaving the ime inset unconsumed here therefore
            // subtracts the keyboard height twice and collapses the WebView to
            // a sliver over the window background.
            return windowInsets.inset(bars.left, bars.top, bars.right, bottomInset);
        });
        ViewCompat.requestApplyInsets(content);
    }
}
