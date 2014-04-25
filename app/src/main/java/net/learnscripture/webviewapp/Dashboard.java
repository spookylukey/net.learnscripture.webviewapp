package net.learnscripture.webviewapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.MailTo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import static android.content.res.Configuration.*;

public class Dashboard extends Activity {

	public String BASE_URL = "http://learnscripture.net/";
	public String DASHBOARD_URL = BASE_URL + "dashboard/";
	public String CONTACT_URL = BASE_URL + "contact/";

	private JavascriptInterface jsInterface;
	private View activeInput = null;

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dashboard);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		WebView engine = (WebView) findViewById(R.id.web_engine);

		// Progress bar.
		// With full screen app, window progress bar (FEATURE_PROGRESS) doesn't seem to show,
		// so we use an explicitly created one.
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
				activeInput = null;
				jsInterface.enablePreferencesMenu  = false;
				jsInterface.modalIsVisible = false;
				jsInterface.urlForSharing = null;
				progressBar.setVisibility(View.VISIBLE);
			}

			public void onPageFinished(WebView view, String url)
			{
				progressBar.setVisibility(View.GONE);
			}
		});
		engine.getSettings().setJavaScriptEnabled(true);
		jsInterface = new JavascriptInterface(this);
		try {
			ComponentName comp = new ComponentName(this, Dashboard.class);
			PackageInfo pinfo = getPackageManager().getPackageInfo(comp.getPackageName(), 0);
			jsInterface.versionCode = pinfo.versionCode;
		} catch(android.content.pm.PackageManager.NameNotFoundException e) {
		}

		engine.addJavascriptInterface(jsInterface, "androidlearnscripture");
		engine.loadUrl(BASE_URL);
	}

	private WebView getEngine() {
		return (WebView) findViewById(R.id.web_engine);
	}

	private boolean isHardwareKeyboardAvailable() {
		return getResources().getConfiguration().keyboard == KEYBOARD_QWERTY;
	}

	public void showKeyboard() {
		if (isHardwareKeyboardAvailable()) {
			return;
		}

		if (activeInput != null) {
			InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			imm.showSoftInput(activeInput, 0);
		}
	}

	public void showMenu() {
		this.openOptionsMenu();
	}

	public void registerInputFocused() {
		activeInput = getCurrentFocus();
	}

	public void onBackPressed() {
		WebView engine = getEngine();
		String url = engine.getUrl(); 
		if (jsInterface.modalIsVisible) {
			engine.loadUrl("javascript: learnscripture.hideModal();");
		} else if (url != null && (
				url.equals(BASE_URL) ||
				url.equals(DASHBOARD_URL) ||
				!engine.canGoBack())) {
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
			prefs.setVisible(jsInterface.enablePreferencesMenu);
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
			return true;
		case R.id.contact_menuitem:
			getEngine().loadUrl(CONTACT_URL);
			return true;
		case R.id.share_url_menuitem:
			final String url = (jsInterface.urlForSharing != null
								? jsInterface.urlForSharing
								: getEngine().getUrl());
			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType("text/plain");
			i.putExtra(Intent.EXTRA_SUBJECT, "LearnScripture URL");
			i.putExtra(Intent.EXTRA_TEXT, url);
			startActivity(Intent.createChooser(i, "Share URL"));
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private class FixedWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (url.startsWith(BASE_URL) || url.startsWith("javascript:")) {
				// handle by the WebView
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
				// Use external browser for anything not on this site
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				view.getContext().startActivity(i);
				return true;
			}
		}
	}

	// The methods of JavascriptInterface are called from javascript.
	// The attributes are accessed from the Dashboard class.
	// This is deliberately a dumb container class to stop possible
	// security issues of javascript controlling Java app.
	final class JavascriptInterface {
		public boolean enablePreferencesMenu = false;
		public boolean modalIsVisible = false;
		public int versionCode = 0;
		public String urlForSharing = null;
		private Dashboard dashboard;

		public JavascriptInterface(Dashboard dashboard) {
			this.dashboard = dashboard;
		}

		public void setEnablePreferencesMenu() {
			enablePreferencesMenu = true;
		}

		public void setModalIsVisible(boolean visible) {
			modalIsVisible = visible;
		}

		// This is useful for allowing the web site to be able to detect
		// old app versions and prompt the user to upgrade.
		public int getVersionCode() {
			return versionCode;
		}
		
		public void setUrlForSharing(String url) {
			urlForSharing = url;
		}

		public void showKeyboard() {
			this.dashboard.showKeyboard();
		}

		public void showMenu() {
			this.dashboard.showMenu();
		}

		public void registerInputFocused() {
			this.dashboard.registerInputFocused();
		}
	}

}
