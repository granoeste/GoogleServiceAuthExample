package net.granoeste.creador.GoogleServiceAuthExample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.granoeste.creador.GoogleServiceAuthExample.GoogleServiceAuthenticator.GOOGLE_ACCOUNT_TYPE;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.accounts.Account;
import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class GoogleServiceAuthExampleActivity extends ListActivity {
    private static final String TAG = GoogleServiceAuthExampleActivity.class.getName();

    GoogleServiceAuthenticator authenticator;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authenticator = new GoogleServiceAuthenticator(this);
        Account[] accounts = authenticator.getGoogleAccounts();
        this.setListAdapter(new ArrayAdapter<Account>(this, android.R.layout.simple_list_item_1, accounts));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
            Account account = (Account)getListView().getItemAtPosition(position);

            authenticator.setHostname("yourappl.appspot.com");
            authenticator.setAppPath("http://localhost/");

            try {
                authenticator.execute(account, GOOGLE_ACCOUNT_TYPE.APPENGINE,
                        new GoogleServiceAuthenticator.PostExecuteCallback() {

                            @Override
                            public void run(String acsid) {

                                DefaultHttpClient httpClient = new DefaultHttpClient();
                                HttpPost httpPost = new HttpPost("http://yourappl.appspot.com/sign");
                                HttpResponse httpResponse = null;

                                try {
                                    List<BasicNameValuePair> parms = new ArrayList<BasicNameValuePair>();
                                    parms.add(new BasicNameValuePair("content", "InputData=" + new SimpleDateFormat().format(new Date()) ));
                                    httpPost.setEntity(new UrlEncodedFormEntity(parms, HTTP.UTF_8));

                                    httpPost.setHeader("Cookie", acsid);

                                    httpResponse = httpClient.execute(httpPost);

                                } catch (UnsupportedEncodingException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                } catch (ClientProtocolException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }

                                if (httpResponse != null) {
                                    int status = httpResponse.getStatusLine().getStatusCode();
                                    StringBuilder buf = new StringBuilder();
                                    buf.append(String.format("status:%d", status));
                                    try {
                                        InputStream in = httpResponse.getEntity().getContent();
                                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                                        String l = null;
                                        while((l = reader.readLine()) != null) {
                                            buf.append(l + "\n");
                                        }
                                        if (status != HttpStatus.SC_OK) {
                                            Log.e(TAG, buf.toString());
                                        }

                                    } catch(Exception e) {
                                        e.printStackTrace();
                                    }
                                    (Toast.makeText(
                                            GoogleServiceAuthExampleActivity.this,
                                            buf.toString(),
                                            Toast.LENGTH_LONG)).show();
                                    Log.d(TAG, buf.toString());
                                }
                            }

                });
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
    }

}