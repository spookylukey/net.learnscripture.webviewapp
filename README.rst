This is a simple Android app for displaying the learnscripture.net site in a
full screen WebView.

It may be useful to other people to serve as a basis for similar apps, where you
have a web app that will work well on an Android device, but you can benefit
from a full screen experience, and from being available on Google Play.

The code is provided under a very liberal license (MIT, see LICENSE), with no
warranty, and no documentation apart from code comments and this README.

The app provides the following refinements/features:

* full screen - so you don't have an address bar getting in the way.

* a progress bar that appears when a page starts to load, and disappears when
  loading is done.

* handling of back button:

  * this will do the same as a normal back button in a web browser, until you
    reach the 'home' page (which in this case is the 'dashboard'), at which
    point the user will go back to the previous activity.

  * when a "modal dialog" is active within the WebView, the back button will
    dismiss it, rather than do a page back. This requires integration with
    javascript in the web app, firstly to expose a javascript method that will
    close the modal, so the Android app can do::

      engine.loadUrl("javascript: learnscripture.hideModal();");

    … and secondly to let the Android app know that a modal is active. In the
    Android app, you have this method::

      IJavascriptHandler.setModalIsVisible(boolean visible)

    and this call to expose IJavascriptHandler to the web page::

      engine.addJavascriptInterface(new IJavascriptHandler(this), "androidlearnscripture");

    In the javascript, you have code like::

      $('div.modal').bind('shown', function (ev) {
          if (window.androidlearnscripture &&
              window.androidlearnscripture.setModalIsVisible) {
              window.androidlearnscripture.setModalIsVisible(true);
          }
      });

    and similar code for when the modals disappear. (This is using jQuery and
    the modal dialogs from Twitter's Bootstrap). The code checks for the
    presence of the "androidlearnscripture" object since the site might not be
    running withing the Android app, or might be running in a different version
    of the app.

* handling of links:

  * Links internal to the LearnScripture.net site are loaded within the WebView

  * External http links are loaded in a separate activity (to show the address
    bar etc and to make use of any existing sessions the user may have on other
    sites in their normal browser)

  * mailto links handled correctly

* A simple menu:

  * Refresh button

  * Home button

  * Preferences button. This launches a modal within the WebView, and requires
    integration with web site javascript code so that the button is only
    available when the preferences modal dialog is available (the javascript
    calls the setEnablePreferencesMenu method).
