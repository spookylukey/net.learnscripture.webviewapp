package net.learnscripture.webviewapp;

import java.net.URISyntaxException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.MailTo;
import android.net.Uri;
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

	public String BASE_URL = "http://learnscripture.net/";
	public String DASHBOARD_URL = BASE_URL + "dashboard/";

	public boolean enablePreferencesMenu = false;
	public boolean modalIsVisible = false;

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
				enablePreferencesMenu  = false;
				modalIsVisible = false;
				progressBar.setVisibility(View.VISIBLE);
			}

			public void onPageFinished(WebView view, String url)
			{
				progressBar.setVisibility(View.GONE);
			}
		});
		engine.getSettings().setJavaScriptEnabled(true);
		engine.addJavascriptInterface(new IJavascriptHandler(this), "androidlearnscripture");
		engine.loadUrl(BASE_URL);
	}

	private WebView getEngine() {
		return (WebView) findViewById(R.id.web_engine);
	}

	public void onBackPressed() {
		WebView engine = getEngine();
		String url = engine.getUrl(); 
		if (modalIsVisible) {
			engine.loadUrl("javascript: learnscripture.hideModal();");
		} else if (url.equals(BASE_URL) ||
				url.equals(DASHBOARD_URL) ||
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
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem prefs = menu.findItem(R.id.preferences_menuitem);
		if (prefs != null) {
			prefs.setVisible(enablePreferencesMenu);
		}
		super.onPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.dashboard_menuitem:
			getEngine().loadUrl(DASHBOARD_URL);
			return true;
		case R.id.refresh_menuitem:
			getEngine().reload();
			return true;
		case R.id.preferences_menuitem:
			getEngine().loadUrl("javascript: learnscripture.showPreferences()");
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private class FixedWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (url.startsWith(BASE_URL) || url.startsWith("javascript:")) {
				return false;
			} else if (url.startsWith("mailto:")) {
				MailTo mt = MailTo.parse(url);
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("message/rfc822");
				i.putExtra(Intent.EXTRA_EMAIL, new String[]{mt.getTo()});
				i.putExtra(Intent.EXTRA_SUBJECT, mt.getSubject());
				i.putExtra(Intent.EXTRA_CC, mt.getCc());
				i.putExtra(Intent.EXTRA_TEXT, mt.getBody());
				view.getContext().startActivity(i);
				view.reload();
				return true;
			} else {
				// We want to give user the choice of which browser, if appropriate
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				view.getContext().startActivity(i);
				return true;
			}
		}
	}

	// The method of IJavascriptHandler are called from javascript
	final class IJavascriptHandler {
		private Dashboard activity;

		IJavascriptHandler(Dashboard activity) {
			this.activity = activity;
		}

		public void setEnablePreferencesMenu() {
			activity.enablePreferencesMenu = true;
		}

		public void setModalIsVisible(boolean visible) {
			activity.modalIsVisible  = visible;
		}
	}

}
