package com.commit451.tinyshareurls.sample;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Activity which registers for all app deep links and routes to the right places
 */
public class RoutingActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //You will probably want to show some loading UI here while it parses the url
        Uri data = getIntent().getData();
        if (data != null) {
            Observable<Uri> observable = App.instance().getPicoUrl().parse(data);
            observable.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<Uri>() {
                        @Override
                        public void onCompleted() {
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            //Alert your user!
                        }

                        @Override
                        public void onNext(Uri url) {
                            String time = url.getQueryParameter("time");
                            Toast.makeText(RoutingActivity.this, "Original url:\n" + url + ". Generated at " + time, Toast.LENGTH_LONG)
                                    .show();
                            //normally you would route here, or do whatever else you need to with
                            //that original url
                            finish();
                        }
                    });
        }

    }
}
