package net.granoeste.creador.GoogleServiceAuthExample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class GoogleServiceAuthenticator {
    private static final String TAG = GoogleServiceAuthenticator.class.getSimpleName();

    public static final String ACCOUNT_TYPE = "com.google";

    public enum GOOGLE_ACCOUNT_TYPE {
        APPENGINE   { public String toString() { return "ah";         } },
//        CALENDAR    { public String toString() { return "cl";         } },
//        GMAIL       { public String toString() { return "mail";       } },
//        READER      { public String toString() { return "reader";     } },
//        TALK        { public String toString() { return "talk";       } },
//        YOUTUBE     { public String toString() { return "youtube";    } },
    }

    public interface PostExecuteCallback {
        void run(String acsid);
    }

    private Context context;
    private AccountManager accountManager;
    private String hostname;
    private String appPath;
    private PostExecuteCallback postExecuteCallback;
    private DefaultHttpClient httpClient = new DefaultHttpClient();

    public GoogleServiceAuthenticator(Context context) {
        this.context = context;
    }

    public Account[] getGoogleAccounts() {
        if (accountManager == null) {
            accountManager = AccountManager.get(context);
        }
        return accountManager.getAccountsByType(ACCOUNT_TYPE);
    }

    public void execute(Account account, GOOGLE_ACCOUNT_TYPE type, PostExecuteCallback postExecuteCallback) throws Exception {
        if (hostname == null) {
            throw new Exception("hostname must not be null");
        }
        if (appPath == null) {
            throw new Exception("appPath must not be null");
        }
        this.postExecuteCallback = postExecuteCallback;

        // Gets an auth token of the specified type.
        accountManager.getAuthToken(
                account,						// The account to fetch an auth token for
                type.toString(),				// The auth token type, an authenticator-dependent string token, must not be null
                false,							// True to add a notification to prompt the user for a password if necessary, false to leave that to the caller
                new GetAuthTokenCallback(),		// Callback to invoke when the request completes, null for no callback
                null							// Handler identifying the callback thread, null for the main thread
                );
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setAppPath(String appPath) {
        this.appPath = appPath;
    }

    private String getLoginUrl(String hostname, String appPath, String authToken) {
        return "https://" + hostname + "/_ah/login?continue=" + appPath + "&auth=" + authToken;
    }

    private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {

        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;
            try {
                bundle = result.getResult();
                Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
                if(intent != null) {
                    // User input required
                    context.startActivity(intent);

                } else {
                    String acsid = getAuthToken(bundle);

                    // If authentication succeeds and gets the SACSID/ACSID, the post-process is called.
                    if (acsid != null) {
                        if (postExecuteCallback != null) {
                            postExecuteCallback.run(acsid);
                        }
                    }
                }
            } catch (OperationCanceledException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };

    private String getAuthToken(Bundle bundle) {

        boolean validated = false;
        int count = 3;
        try {
            while (!validated) {
                String authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);

                String uri = getLoginUrl(hostname, appPath, authToken);
                HttpGet httpGet = new HttpGet(uri);
                HttpResponse httpResponse = httpClient.execute(httpGet);

                int status = httpResponse.getStatusLine().getStatusCode();

                if (status == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                    // Authenticate error (500)
                    try {
                        StringBuilder buf = new StringBuilder();
                        buf.append(String.format("Status:%d ", status));
                        InputStream in = httpResponse.getEntity().getContent();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        String l = null;
                        while ((l = reader.readLine()) != null) {
                            buf.append(l + "\n");
                        }
                        Log.w(TAG, buf.toString());
                    } catch (Exception e) {
                    }

                    // Removes an auth token from the AccountManager's cache.
                    String accountType = bundle.getString(AccountManager.KEY_ACCOUNT_TYPE);
                    accountManager.invalidateAuthToken(accountType, authToken);

                } else {
                    // Authenticate success
                    validated = true;
                    break;
                }
                // retry count down
                if (0 < count--) {
                    break;
                }
            }

            // If authentication succeeds, get the SACSID/ACSID from the Cookie
            if (validated) {
                for (Cookie cookie : httpClient.getCookieStore().getCookies()) {
                  if ("SACSID".equals(cookie.getName()) || "ACSID".equals(cookie.getName())) {
                        return cookie.getName() + "=" + cookie.getValue();
                    }
                }
            }

        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}
