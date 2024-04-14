/*
SlimSocial for Twitter is an Open Source app realized by Leonardo Rignanese
 GNU GENERAL PUBLIC LICENSE  Version 2, June 1991
*/

package it.rignanese.leo.slimtwitter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;

import im.delight.android.webview.AdvancedWebView;

public class MainActivity extends Activity implements AdvancedWebView.Listener {
    //the main webView where is shown twitter
    private AdvancedWebView webViewTwitter;

    //object to show full screen videos
    private WebChromeClient myWebChromeClient;
    private FrameLayout mTargetView;
    private FrameLayout mContentView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private View mCustomView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        // setup the webView
        webViewTwitter = findViewById(R.id.webView);

        webViewTwitter.setListener(this, this);
        webViewTwitter.addPermittedHostname("mobile.twitter.com");
        webViewTwitter.addPermittedHostname("twitter.com");

        //full screen video
        myWebChromeClient = new WebChromeClient() {
            //this custom WebChromeClient allow to show video on full screen
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                mCustomViewCallback = callback;
                mTargetView.addView(view);
                mCustomView = view;
                mContentView.setVisibility(View.GONE);
                mTargetView.setVisibility(View.VISIBLE);
                mTargetView.bringToFront();
            }

            @Override
            public void onHideCustomView() {
                if (mCustomView == null)
                    return;

                mCustomView.setVisibility(View.GONE);
                mTargetView.removeView(mCustomView);
                mCustomView = null;
                mTargetView.setVisibility(View.GONE);
                mCustomViewCallback.onCustomViewHidden();
                mContentView.setVisibility(View.VISIBLE);
            }
        };
        webViewTwitter.setWebChromeClient(myWebChromeClient);
        mContentView = findViewById(R.id.main_content);
        mTargetView = findViewById(R.id.target_view);


        String urlSharer = ExternalLinkListener();//get the external shared link (if it exists)
        if (urlSharer != null) {//if is a share request
            webViewTwitter.loadUrl(urlSharer);//load the sharer url
        } else {
            webViewTwitter.loadUrl(getString(R.string.urlTwitterMobile));//load homepage
        }
    }

    // app is already running and gets a new intent (used to share link without open another activity
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        webViewTwitter.loadUrl(ExternalLinkListener());
    }

    private String ExternalLinkListener() {
        Intent intent = getIntent();
        String intentAction = intent.getAction();

        if (intentAction == null) {
            return null;
        }

        // If this Activity was launched because the user clicked on a supported URL just use that URL.
        if (intentAction.equals(Intent.ACTION_VIEW) && intent.getDataString() != null) {
            return intent.getDataString();
        }

        // Extract text and/or a URL when text was shared to this app.
        if (intentAction.equals(Intent.ACTION_SEND)) {
            String sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);

            String text = null;
            String url = null;

            if (sharedText != null) {
                if (sharedText.startsWith("http://") || sharedText.startsWith("https://")) {
                    // If the text starts with http[s]:// we just assume it's a URL and use it as the 'url' argument.
                    url = sharedText;
                } else {
                    // Otherwise we'll use the value as text for the tweet.
                    text = sharedText;
                }
            }

            if (text == null && sharedSubject != null) {
                // If we don't have a value for the text of the tweet yet, use the subject value.
                text = sharedSubject;
            }

            Uri.Builder uriBuilder = Uri.parse("https://twitter.com/intent/tweet").buildUpon();
            if (text != null) {
                uriBuilder.appendQueryParameter("text", text);
            }
            if (url != null) {
                uriBuilder.appendQueryParameter("url", url);
            }

            return uriBuilder.build().toString();
        }

        return null;
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        webViewTwitter.onResume();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        webViewTwitter.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.e("Info", "onDestroy()");
        webViewTwitter.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        webViewTwitter.onActivityResult(requestCode, resultCode, intent);
    }

    //*********************** WebView methods ****************************

    @Override
    public void onPageStarted(String url, Bitmap favicon) {
    }

    @Override
    public void onPageFinished(String url) {
        String promoCSS = "javascript:(function() {" +
			         "var node = document.createElement('style');" +
			         "node.type = 'text/css';" +
			         "node.innerHTML = '[style]>div>div>[data-testid=placementTracking] {" +
			         "display: none;" +
			         "}';" +
			         "document.head.appendChild(node);" +
			         "}) ()";
	       webViewTwitter.loadUrl(promoCSS);
    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
        String summary =
                "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" /></head><body><h1 "
                        + "style='text-align:center; padding-top:15%;'>"
                        + getString(R.string.titleNoConnection)
                        + "</h1> <h3 style='text-align:center; padding-top:1%; font-style: italic;'>"
                        + getString(R.string.descriptionNoConnection)
                        + "</h3>  <h5 style='text-align:center; padding-top:80%; opacity: 0.3;'>"
                        + getString(R.string.awards)
                        + "</h5></body></html>";
        webViewTwitter.loadData(summary, "text/html; charset=utf-8", "utf-8");
        //load a custom html page
    }

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType,
                                    long contentLength, String contentDisposition, String userAgent) {
    }

    @Override
    public void onExternalPageRequest(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }


    //*********************** KEY ****************************
    // handling the back button
    @Override
    public void onBackPressed() {
        if (mCustomView != null) {
            myWebChromeClient.onHideCustomView();//hide video player
        } else {
            if (webViewTwitter.canGoBack()) {
                webViewTwitter.goBack();
            } else {
                finish();// close app
            }
        }
    }
}
