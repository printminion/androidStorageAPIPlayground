package com.example.storage.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.example.storage.R;

public class HomeActivity extends Activity {
	
    private static final int DIALOG_ACCOUNTS = 0;
    private static final int REQUEST_AUTHENTICATE = 1;
	private static final String PREF = "prefs";
	protected static final String AUTH_TOKEN_TYPE = "storage"; // TODO wrong type. A list of known types is here: http://code.google.com/intl/de-DE/apis/gdata/faq.html#clientlogin
	private String authToken;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        gotAccount(false);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
      switch (id) {
        case DIALOG_ACCOUNTS:
          AlertDialog.Builder builder = new AlertDialog.Builder(this);
          builder.setTitle("Select a Google account");
          final AccountManager manager = AccountManager.get(this);
          final Account[] accounts = manager.getAccountsByType("com.google");
          final int size = accounts.length;
          String[] names = new String[size];
          for (int i = 0; i < size; i++) {
            names[i] = accounts[i].name;
          }
          builder.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              gotAccount(manager, accounts[which]);
            }
          });
          return builder.create();
      }
      return null;
    }

    private void gotAccount(boolean tokenExpired) {
      SharedPreferences settings = getSharedPreferences(PREF, 0);
      String accountName = settings.getString("accountName", null);
      if (accountName != null) {
        AccountManager manager = AccountManager.get(this);
        Account[] accounts = manager.getAccountsByType("com.google");
        int size = accounts.length;
        for (int i = 0; i < size; i++) {
          Account account = accounts[i];
          if (accountName.equals(account.name)) {
            if (tokenExpired) {
              manager.invalidateAuthToken("com.google", this.authToken);
            }
            gotAccount(manager, account);
            return;
          }
        }
      }
      showDialog(DIALOG_ACCOUNTS);
    }

    private void gotAccount(final AccountManager manager, final Account account) {
      SharedPreferences settings = getSharedPreferences(PREF, 0);
      SharedPreferences.Editor editor = settings.edit();
      editor.putString("accountName", account.name);
      editor.commit();
      new Thread() {

        @Override
        public void run() {
          try {
            final Bundle bundle =
                manager.getAuthToken(account, AUTH_TOKEN_TYPE, true, null, null)
                    .getResult();
            runOnUiThread(new Runnable() {

              public void run() {
                try {
                  if (bundle.containsKey(AccountManager.KEY_INTENT)) {
                    Intent intent =
                        bundle.getParcelable(AccountManager.KEY_INTENT);
                    int flags = intent.getFlags();
                    flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
                    intent.setFlags(flags);
                    startActivityForResult(intent, REQUEST_AUTHENTICATE);
                  } else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
                    authenticatedClientLogin(
                        bundle.getString(AccountManager.KEY_AUTHTOKEN));
                  }
                } catch (Exception e) {
                  handleException(e);
                }
              }
            });
          } catch (Exception e) {
            handleException(e);
          }
        }
      }.start();
    }

    private void handleException(Exception e) {
		throw new RuntimeException(e);
	}

	@Override
    protected void onActivityResult(
        int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      switch (requestCode) {
        case REQUEST_AUTHENTICATE:
          if (resultCode == RESULT_OK) {
            gotAccount(false);
          } else {
            showDialog(DIALOG_ACCOUNTS);
          }
          break;
      }
    }

    private void authenticatedClientLogin(String authToken) {
      this.authToken = authToken;
      new AlertDialog.Builder(this).setMessage(authToken).create().show();
      /*((GoogleHeaders) transport.defaultHeaders).setGoogleLogin(authToken);
      authenticated();*/
    }
}