package co.hodlwallet.tools.threads;

import android.app.Activity;
import android.os.AsyncTask;

import co.hodlwallet.BuildConfig;
import co.hodlwallet.R;
import co.hodlwallet.presenter.customviews.BRDialogView;
import co.hodlwallet.presenter.entities.ImportPrivKeyEntity;
import co.hodlwallet.tools.animation.BRDialog;
import co.hodlwallet.tools.util.BRCurrency;
import co.hodlwallet.tools.util.BRExchange;
import co.hodlwallet.wallet.BRWalletManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/2/16.
 * Copyright (c) 2016 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class ImportPrivKeyTask extends AsyncTask<String, String, String> {
    public static final String TAG = ImportPrivKeyTask.class.getName();
    public static String UTXO_URL;
    public static String TX_URL;
    private Activity app;
    private String key;
    private ImportPrivKeyEntity importPrivKeyEntity;

    public ImportPrivKeyTask(Activity activity) {
        app = activity;

        UTXO_URL = BuildConfig.BITCOIN_TESTNET ? "https://blockstream.info/testnet/api/address/%s/utxo" : "https://blockstream.info/api/address/%s/utxo";
        TX_URL = BuildConfig.BITCOIN_TESTNET ? "https://blockstream.info/testnet/api/tx/%s" : "https://blockstream.info/api/tx/%s";
    }

    @Override
    protected String doInBackground(String... params) {
        if (params.length == 0) return null;
        key = params[0];
        if (key == null || key.isEmpty() || app == null) return null;

        String legacyAddress = BRWalletManager.getInstance().getLegacyAddressFromPrivKey(key);
        String legacyUrl = String.format(UTXO_URL, legacyAddress);
        String bech32Address = BRWalletManager.getInstance().getAddressFromPrivKey(key);
        String bech32Url = String.format(UTXO_URL, bech32Address);

        importPrivKeyEntity = createTx(legacyUrl, bech32Url);
        if (importPrivKeyEntity == null) {
            app.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    BRDialog.showCustomDialog(app, app.getString(R.string.JailbreakWarnings_title),
                            app.getString(R.string.Import_Error_empty), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismissWithAnimation();
                                }
                            }, null, null, 0);
                }
            });
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        if (importPrivKeyEntity == null) {
            return;
        }

//        String iso = BRSharedPrefs.getIso(app);

        String sentBits = BRCurrency.getFormattedCurrencyString(app, "BTC", BRExchange.getAmountFromSatoshis(app, "BTC", new BigDecimal(importPrivKeyEntity.getAmount())));
//        String sentExchange = BRCurrency.getFormattedCurrencyString(app, iso, BRExchange.getAmountFromSatoshis(app, iso, new BigDecimal(importPrivKeyEntity.getAmount())));

        String feeBits = BRCurrency.getFormattedCurrencyString(app, "BTC", BRExchange.getAmountFromSatoshis(app, "BTC", new BigDecimal(importPrivKeyEntity.getFee())));
//        String feeExchange = BRCurrency.getFormattedCurrencyString(app, iso, BRExchange.getAmountFromSatoshis(app, iso, new BigDecimal(importPrivKeyEntity.getFee())));

        if (app == null || importPrivKeyEntity == null) return;
        String message = String.format(app.getString(R.string.Import_confirm), sentBits, feeBits);
        String posButton = String.format("%s (%s)", sentBits, feeBits);
        BRDialog.showCustomDialog(app, "", message, posButton, app.getString(R.string.Button_cancel), new BRDialogView.BROnClickListener() {
            @Override
            public void onClick(BRDialogView brDialogView) {
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        boolean result = BRWalletManager.getInstance().confirmKeySweep(importPrivKeyEntity.getTx(), key);
                        if (!result) {
                            app.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    BRDialog.showCustomDialog(app, app.getString(R.string.JailbreakWarnings_title),
                                            app.getString(R.string.Import_Error_notValid), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                                                @Override
                                                public void onClick(BRDialogView brDialogView) {
                                                    brDialogView.dismissWithAnimation();
                                                }
                                            }, null, null, 0);
                                }
                            });

                        }
                        else {
                            app.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    BRDialog.showCustomDialog(app, app.getString(R.string.Import_importing),
                                            app.getString(R.string.Import_success), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                                                @Override
                                                public void onClick(BRDialogView brDialogView) {
                                                    brDialogView.dismissWithAnimation();
                                                }
                                            }, null, null, 0);
                                }
                            });

                        }
                    }
                });

                brDialogView.dismissWithAnimation();

            }
        }, new BRDialogView.BROnClickListener() {
            @Override
            public void onClick(BRDialogView brDialogView) {
                brDialogView.dismissWithAnimation();
            }
        }, null, 0);
        super.onPostExecute(s);
    }

    public static ImportPrivKeyEntity createTx(String legacyUrl, String bech32Url) {
        String[] urls = {legacyUrl, bech32Url};

        for (int i = 0; i <= 1; i++) {
            String url = urls[i];
            boolean inputArrayCreated = false;

            if (url == null || url.isEmpty()) return null;
            String jsonString = callURL(url);
            if (jsonString == null || jsonString.isEmpty()) return null;

            JSONArray jsonArray = null;
            try {
                jsonArray = new JSONArray(jsonString);
                int length = jsonArray.length();

                for (int j = 0; j < length; j++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    JSONObject status = obj.getJSONObject("status");

                    boolean confirmed = status.getBoolean("confirmed");

                    if (!confirmed) continue;
                    if (!inputArrayCreated) {
                        BRWalletManager.getInstance().createInputArray();

                        inputArrayCreated = true;
                    }

                    String txid = obj.getString("txid");
                    int vout = obj.getInt("vout");
                    long value = obj.getLong("value");

                    String txUrl = String.format(TX_URL, txid);

                    if (txUrl == null || txUrl.isEmpty()) return null;
                    String txJsonString = callURL(txUrl);
                    if (txJsonString == null || txJsonString.isEmpty()) return null;

                    JSONObject txJsonObject = new JSONObject(txJsonString);
                    JSONArray vouts = txJsonObject.getJSONArray("vout");

                    String scriptPubKey = null;
                    if (vouts.length() >= (vout + 1)) {
                        JSONObject voutItem = vouts.getJSONObject(vout);
                        scriptPubKey = voutItem.getString("scriptpubkey");
                    }
                    byte[] txidBytes = hexStringToByteArray(txid);
                    byte[] scriptPubKeyBytes = hexStringToByteArray(scriptPubKey);

                    BRWalletManager.getInstance().addInputToPrivKeyTx(txidBytes, vout, scriptPubKeyBytes, value);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return BRWalletManager.getInstance().getPrivKeyObject();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static String callURL(String myURL) {
        StringBuilder sb = new StringBuilder();
        URLConnection urlConn = null;
        InputStreamReader in = null;
        try {
            URL url = new URL(myURL);
            urlConn = url.openConnection();
            if (urlConn != null)
                urlConn.setReadTimeout(60 * 1000);
            if (urlConn != null && urlConn.getInputStream() != null) {
                in = new InputStreamReader(urlConn.getInputStream(),
                        Charset.defaultCharset());
                BufferedReader bufferedReader = new BufferedReader(in);

                int cp;
                while ((cp = bufferedReader.read()) != -1) {
                    sb.append((char) cp);
                }
                bufferedReader.close();
            }
            assert in != null;
            in.close();
        } catch (Exception e) {
            return null;
        }

        return sb.toString();
    }
}
