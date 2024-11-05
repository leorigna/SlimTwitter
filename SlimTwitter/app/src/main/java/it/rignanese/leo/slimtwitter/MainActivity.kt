/*
SlimSocial for Twitter is an Open Source app realized by Leonardo Rignanese
 GNU GENERAL PUBLIC LICENSE  Version 2, June 1991
*/

package it.rignanese.leo.slimtwitter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.widget.FrameLayout

import im.delight.android.webview.AdvancedWebView

class MainActivity: Activity(), AdvancedWebView.Listener {
    //the main webView where is shown twitter
    private var webViewTwitter: AdvancedWebView? = null

    //object to show full screen videos
    private var myWebChromeClient: WebChromeClient? = null
    private var mTargetView: FrameLayout? = null
    private var mContentView: FrameLayout? = null
    private var mCustomViewCallback: WebChromeClient.CustomViewCallback? = null
    private var mCustomView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // setup the webView
        webViewTwitter = findViewById(R.id.webView)

        webViewTwitter?.setListener(this, this)
        webViewTwitter?.addPermittedHostname("mobile.x.com")
        webViewTwitter?.addPermittedHostname("x.com")

        //full screen video
        myWebChromeClient = object : WebChromeClient() {
            //this custom WebChromeClient allow to show video on full screen

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                mCustomViewCallback = callback
                mTargetView?.addView(view)
                mCustomView = view
                mContentView?.visibility = View.GONE
                mTargetView?.visibility = View.VISIBLE
                mTargetView?.bringToFront()
            }

            override fun onHideCustomView() {
                if (mCustomView == null)
                    return

                mCustomView?.visibility = View.GONE
                mTargetView?.removeView(mCustomView)
                mCustomView = null
                mTargetView?.visibility = View.GONE
                mCustomViewCallback?.onCustomViewHidden()
                mContentView?.visibility = View.VISIBLE
            }
        }
        webViewTwitter?.webChromeClient = myWebChromeClient
        mContentView = findViewById(R.id.main_content)
        mTargetView = findViewById(R.id.target_view)

        //get the external shared link (if it exists)
        val urlSharer = ExternalLinkListener()
        if (urlSharer != null) {
            //if is a share request, load the sharer url
            webViewTwitter?.loadUrl(urlSharer)
        } else {
            //load homepage
            webViewTwitter?.loadUrl(getString(R.string.urlTwitterMobile))
        }
    }

    // app is already running and gets a new intent (used to share link without open another activity
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // If the ExternalLinkListener fails, go back to twitter base url as Fallback
        webViewTwitter?.loadUrl(ExternalLinkListener() ?: getString(R.string.urlTwitterMobile))
    }

    private fun ExternalLinkListener(): String? {
        val intent = intent
        val intentAction = intent.action ?: return null

        // If this Activity was launched because the user clicked on a supported URL just use that URL.
        if (intentAction == Intent.ACTION_VIEW && intent.dataString != null) {
            return intent.dataString
        }

        // Extract text and/or a URL when text was shared to this app.
        if (intentAction == Intent.ACTION_SEND) {
            val sharedSubject: String? = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            val sharedText: String? = intent.getStringExtra(Intent.EXTRA_TEXT)

            var text: String? = null
            var url: String? = null

            if (sharedText != null) {
                if (sharedText.startsWith("http://") || sharedText.startsWith("https://")) {
                    // If the text starts with http[s]:// we just assume it's a URL and use it as the 'url' argument.
                    url = sharedText
                } else {
                    // Otherwise we'll use the value as text for the tweet.
                    text = sharedText
                }
            }

            if (text == null && sharedSubject != null) {
                // If we don't have a value for the text of the tweet yet, use the subject value.
                text = sharedSubject
            }

            val uriBuilder: Uri.Builder = Uri.parse("https://x.com/intent/tweet").buildUpon()
            if (text != null) {
                uriBuilder.appendQueryParameter("text", text)
            }
            if (url != null) {
                uriBuilder.appendQueryParameter("url", url)
            }

            return uriBuilder.build().toString()
        }

        return null
    }

    @SuppressLint("NewApi")
    override fun onResume() {
        super.onResume()
        webViewTwitter?.onResume()
    }

    @SuppressLint("NewApi")
    override fun onPause() {
        webViewTwitter?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        Log.e("Info", "onDestroy()")
        webViewTwitter?.onDestroy()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
        super.onActivityResult(requestCode, resultCode, intent)
        webViewTwitter?.onActivityResult(requestCode, resultCode, intent)
    }

    //*********************** WebView methods ****************************

    override fun onPageStarted(url: String, favicon: Bitmap?) { }

    override fun onPageFinished(url: String) { }

    override fun onPageError(errorCode: Int, description: String, failingUrl: String) {
        val summary =
        "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html charset=UTF-8\" /></head><body><h1 style='text-align:center padding-top:15%'> " +
        getString(R.string.titleNoConnection) +
        "</h1> <h3 style='text-align:center padding-top:1% font-style: italic'>" +
        getString(R.string.descriptionNoConnection) +
        "</h3>  <h5 style='text-align:center padding-top:80% opacity: 0.3'>" +
        getString(R.string.awards) +
        "</h5></body></html>"

        webViewTwitter?.loadData(summary, "text/html charset=utf-8", "utf-8")
        //load a custom html page
    }

    override fun onDownloadRequested(url: String, suggestedFilename: String, mimeType: String,
                                     contentLength: Long, contentDisposition: String, userAgent: String) { }

    override fun onExternalPageRequest(url: String) {
        webViewTwitter?.loadUrl(url)
        //startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }


    //*********************** KEY ****************************
    // handling the back button
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (mCustomView != null) {
            myWebChromeClient?.onHideCustomView()//hide video player
        } else {
            if (webViewTwitter?.canGoBack() == true) {
                webViewTwitter?.goBack()
            } else {
                // close app
                finish()
            }
        }
    }
}

