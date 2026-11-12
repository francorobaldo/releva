package app.releva;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Releva como app de Android: la página vive dentro de la app y los datos quedan en el teléfono. */
public class MainActivity extends Activity {
    static final String HOST = "appassets.androidplatform.net";
    static final String START = "https://" + HOST + "/assets/www/index.html";
    private static final int REQ_FILE = 41;

    private WebView web;
    private ValueCallback<Uri[]> fileCb;
    private boolean pageReady;
    private String pendingImport;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        FrameLayout root = new FrameLayout(this);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        root.setBackgroundColor(0xFFFFFFFF);
        // que la app no quede debajo de la barra de estado ni de la de navegación
        root.setOnApplyWindowInsetsListener((v, ins) -> {
            int il, it, ir, ib;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets i = ins.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                android.graphics.Insets ime = ins.getInsets(WindowInsets.Type.ime());
                il = i.left; it = i.top; ir = i.right; ib = Math.max(i.bottom, ime.bottom);
            } else {
                il = ins.getSystemWindowInsetLeft(); it = ins.getSystemWindowInsetTop(); ir = ins.getSystemWindowInsetRight(); ib = ins.getSystemWindowInsetBottom();
            }
            v.setPadding(il, it, ir, ib);
            return Build.VERSION.SDK_INT >= 30 ? WindowInsets.CONSUMED : ins.consumeSystemWindowInsets();
        });

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setTextZoom(100);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .setDomain(HOST)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                return loader.shouldInterceptRequest(req.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if (HOST.equals(u.getHost())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (ActivityNotFoundException ignored) {}
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                runImport();
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                try {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(Intent.createChooser(i, "Abrir relevamiento"), REQ_FILE);
                } catch (ActivityNotFoundException e) {
                    fileCb = null;
                    return false;
                }
                return true;
            }
        });

        web.addJavascriptInterface(new Bridge(), "RelevaNative");
        readIncoming(getIntent());
        web.loadUrl(START);
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        readIncoming(i);
        runImport();
    }

    /** Si la app se abrió con un archivo .releva (desde WhatsApp, mail o archivos), lo lee. */
    private void readIncoming(Intent i) {
        if (i == null) return;
        Uri u = null;
        if (Intent.ACTION_VIEW.equals(i.getAction())) u = i.getData();
        else if (Intent.ACTION_SEND.equals(i.getAction())) {
            Object o = i.getParcelableExtra(Intent.EXTRA_STREAM);
            if (o instanceof Uri) u = (Uri) o;
        }
        if (u == null) return;
        try (InputStream in = getContentResolver().openInputStream(u)) {
            if (in == null) return;
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            pendingImport = new String(bo.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo leer el archivo", Toast.LENGTH_LONG).show();
        }
    }

    private void runImport() {
        if (!pageReady || pendingImport == null) return;
        String txt = pendingImport;
        pendingImport = null;
        String js = "window.relevaImportText&&relevaImportText(" + org.json.JSONObject.quote(txt) + ")";
        web.evaluateJavascript(js, null);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_FILE && fileCb != null) {
            Uri u = (res == RESULT_OK && data != null) ? data.getData() : null;
            fileCb.onReceiveValue(u != null ? new Uri[]{u} : null);
            fileCb = null;
            return;
        }
        super.onActivityResult(req, res, data);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.relevaBack?relevaBack():false", r -> {
            if (!"true".equals(r)) moveTaskToBack(true);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // que lo último quede guardado aunque Android cierre la app
        if (pageReady) web.evaluateJavascript("window.relevaFlush&&relevaFlush()", null);
    }

    private static String safeName(String name) {
        String n = name == null ? "relevamiento.releva" : name.replaceAll("[^A-Za-z0-9._-]", "_");
        return n.isEmpty() ? "relevamiento.releva" : n;
    }

    /** Lo que la página puede pedirle al teléfono. */
    class Bridge {
        /** Comparte el archivo del relevamiento (WhatsApp, mail, Drive…). */
        @JavascriptInterface
        public void shareFile(String text, String name) {
            runOnUiThread(() -> {
                try {
                    File dir = new File(getCacheDir(), "share");
                    if (!dir.exists()) dir.mkdirs();
                    File f = new File(dir, safeName(name));
                    try (FileOutputStream os = new FileOutputStream(f)) { os.write(text.getBytes(StandardCharsets.UTF_8)); }
                    Uri u = androidx.core.content.FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".files", f);
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("application/octet-stream");
                    i.putExtra(Intent.EXTRA_STREAM, u);
                    i.putExtra(Intent.EXTRA_SUBJECT, "Relevamiento · " + f.getName());
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(i, "Compartir relevamiento"));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "No se pudo compartir", Toast.LENGTH_LONG).show();
                }
            });
        }

        /** Guarda el archivo en la carpeta Descargas del teléfono. */
        @JavascriptInterface
        public boolean saveFile(String text, String name) {
            try {
                String n = safeName(name);
                byte[] data = text.getBytes(StandardCharsets.UTF_8);
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.MediaColumns.DISPLAY_NAME, n);
                    cv.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
                    cv.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Releva");
                    Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (u == null) return false;
                    try (OutputStream os = getContentResolver().openOutputStream(u)) {
                        if (os == null) return false;
                        os.write(data);
                    }
                    return true;
                } else {
                    File dir = getExternalFilesDir("Releva");
                    if (dir == null) return false;
                    if (!dir.exists()) dir.mkdirs();
                    try (FileOutputStream os = new FileOutputStream(new File(dir, n))) { os.write(data); }
                    return true;
                }
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public void vibrate(int ms) {
            try {
                android.os.Vibrator v = getSystemService(android.os.Vibrator.class);
                if (v != null) v.vibrate(android.os.VibrationEffect.createOneShot(Math.max(5, Math.min(ms, 200)), android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            } catch (Exception ignored) {}
        }
    }
}
