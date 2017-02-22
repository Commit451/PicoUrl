package com.commit451.tinyshareurls.sample;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share();
            }
        });
    }

    private void share() {
        Uri shareUrl = Uri.parse("http://" + getString(R.string.share_base_url));
        shareUrl = shareUrl.buildUpon()
                .appendQueryParameter("time", String.valueOf(System.currentTimeMillis()))
                .build();
        Single<Uri> observable = App.instance().getPicoUrl().generate(shareUrl);
        observable.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Uri>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        //Alert your user!
                    }

                    @Override
                    public void onSuccess(Uri url) {
                        Toast.makeText(MainActivity.this, "Shortened Url: " + url.toString(), Toast.LENGTH_SHORT)
                                .show();
                        //Normally you would share this link. But we will test it immediately
                        Intent intent = new Intent(Intent.ACTION_VIEW, url);
                        startActivity(intent);
                    }
                });
    }
}
