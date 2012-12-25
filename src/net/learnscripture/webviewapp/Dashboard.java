package net.learnscripture.webviewapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class Dashboard extends Activity {


	@SuppressLint("SetJavaScriptEnabled") @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dashboard);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		WebView engine = (WebView) findViewById(R.id.web_engine);

		// Progress bar.
		// With full screen app, window progress bar (FEATURE_PROGRESS) doesn't seem to show,
		// so we add an explicit one.
		final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressbar);

		engine.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress)
			{
				progressBar.setProgress(progress);
			}
		});

		engine.setWebViewClient(new FixedWebViewClient() {
			public void onPageStarted(WebView view, String url, Bitmap favicon)
			{
				progressBar.setVisibility(View.VISIBLE);
			}

			public void onPageFinished(WebView view, String url)
			{
				progressBar.setVisibility(View.GONE);
			}
		});
		engine.getSettings().setJavaScriptEnabled(true);

		engine.loadUrl("http://learnscripture.net/");
	}

	private WebView getEngine() {
		return (WebView) findViewById(R.id.web_engine);
	}

	public void onBackPressed() {
		WebView engine = getEngine();
		String url = engine.getUrl(); 
		if (url.equals("http://learnscripture.net/") ||
				url.equals("http://learnscripture.net/dashboard/") ||
				!engine.canGoBack()) {
			// exit
			super.onBackPressed();
		} else {
			// go back a page, like normal browser
			engine.goBack();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.dashboard_menuitem:
			getEngine().loadUrl("http://learnscripture.net/dashboard/");
			return true;
		case R.id.refresh_menuitem:
			getEngine().reload();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private class FixedWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
	}

}
