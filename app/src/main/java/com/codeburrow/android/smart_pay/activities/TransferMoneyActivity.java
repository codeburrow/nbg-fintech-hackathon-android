package com.codeburrow.android.smart_pay.activities;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.codeburrow.android.smart_pay.JsonParser;
import com.codeburrow.android.smart_pay.R;
import com.codeburrow.android.smart_pay.tasks.AttemptToFindAccountTask;
import com.codeburrow.android.smart_pay.tasks.AttemptToFindAccountTask.AccountAsyncResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class TransferMoneyActivity extends AppCompatActivity implements AccountAsyncResponse {
    private static final String LOG_TAG = TransferMoneyActivity.class.getSimpleName();

    // QR code reading constants
    public static final String IBAN_QR_CODE_JSON_KEY = "iban";
    public static final String AMOUNT_OF_MONEY_QR_CODE_KEY = "amount-of-money";

    private EditText passEditText;
    private TextView infoTextView;

    // QR code information
    private String iban;
    private String amountOfMoney;
    private boolean validateReceiver = false;

    private String uuid = "codeburrow.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_money);

        passEditText = (EditText) findViewById(R.id.password_edittext);
        infoTextView = (TextView) findViewById(R.id.info_text_view);

        String qrCodeJsonStr = getIntent().getStringExtra(ScanQrCodeActivity.QR_INFO);
        readQrCode(qrCodeJsonStr);

        AttemptToFindAccountTask attemptToFindAccountTask = new AttemptToFindAccountTask(getApplicationContext(), this, iban);
        attemptToFindAccountTask.execute();
    }

    /**
     * @param qrCodeJsonStr the json formatted String from the QR code (intent)
     */
    private void readQrCode(String qrCodeJsonStr) {
        try {
            JSONObject qrCodeJsonObject = new JSONObject(qrCodeJsonStr);

            iban = qrCodeJsonObject.getString(IBAN_QR_CODE_JSON_KEY);
            amountOfMoney = qrCodeJsonObject.getString(AMOUNT_OF_MONEY_QR_CODE_KEY);

            Log.e(LOG_TAG, "Send to\nQR_INFO: " + iban + "\nThe amount of: " + amountOfMoney + "$\nuuid: " + uuid);
            updateInfoText();
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());

            infoTextView.setText("What are you looking for?");
        }
    }

    public void updateInfoText(){
        if (validateReceiver) {
            infoTextView.setText("Send to\nIBAN: " + iban + "\nThe amount of: " + amountOfMoney + "$\nuuid: " + uuid);
        } else {
            infoTextView.setText("Scanned an INVALID IBAN");
        }
    }

    public void verifyTransaction(View view) {
        String password = passEditText.getText().toString();

        if (validateReceiver) {
            if (validatePassword(password)) {
                AttemptToMakeTransactionTask attemptToMakeTransactionTask = new AttemptToMakeTransactionTask();
                attemptToMakeTransactionTask.execute();
            } else {
                toast("Wrong password.");
            }
        } else {
            toast("Invalid IBAN");
        }

    }

    public boolean validatePassword(String password) {
        String storedPassword = getSharedPreferences(LoginActivity.PREFERENCES, Context.MODE_PRIVATE).getString(LoginActivity.PASSWORD_PREFS_KEY, null);

        if (storedPassword != null & storedPassword.equals(password)) {
            return true;
        }

        return false;
    }

    /**
     * @param message
     */
    private void toast(String message) {
        Context context = getApplicationContext();
        CharSequence text = message;
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, ScanQrCodeActivity.class));
        finish();
    }

    @Override
    public void processFindAccountAsyncFinish() {
        validateReceiver = true;
        updateInfoText();
    }


    private class AttemptToMakeTransactionTask extends AsyncTask<Void, Void, String> {
        private final String TAG = AttemptToMakeTransactionTask.class.getSimpleName();

        @Override
        protected String doInBackground(Void... args) {
            String setTransactionUrl =
                    "https://nbgdemo.azure-api.net/nodeopenapi/api/transactions/rest";
            JsonParser jsonParser = new JsonParser();

            String nbgTrackId = UUID.randomUUID().toString();

            JSONObject jsonParams = new JSONObject();
            JSONObject jsonPayload = new JSONObject();
            JSONObject jsonInsert = new JSONObject();
            JSONObject jsonDetails = new JSONObject();
            JSONObject jsonValue = new JSONObject();

            try {
                jsonValue.put("amount", "200");
                jsonDetails.put("value", jsonValue);
                jsonDetails.put("posted_by_user_id", "571a162f95806d5414110f20");
                jsonDetails.put("approved_by_user_id", "571b6c423ddcdb580cbee7db");
                jsonInsert.put("details", jsonDetails);
                jsonInsert.put("uuid", uuid);
                jsonPayload.put("insert", jsonInsert);
                jsonParams.put("payload", jsonPayload);
                jsonParams.put("nbgtrackid", nbgTrackId);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }

            JSONObject manJson = new JSONObject();

            return jsonParser.makePutRequest(setTransactionUrl, jsonParams);
        }

        @Override
        protected void onPostExecute(String apiResponse) {
            if (null == apiResponse) {
                toast("Transaction failed.");
            } else {
                Log.e(TAG, apiResponse);
                toast("Transaction verified.");

                Intent succesfulTransactionIntent = new Intent(getApplicationContext(), SuccessfulTransactionActivity.class);
                startActivity(succesfulTransactionIntent);
            }
        }
    }

}
