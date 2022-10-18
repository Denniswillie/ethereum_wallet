package com.example.cryptoproto2;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.web3j.crypto.Bip39Wallet;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.lang.Math;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    Web3j web3;
    Credentials credentials;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Create walletFilePath file if not exist. This file stores the file name that contains the
        // wallet's credentials. In other words, this file is a pointer to the wallet's file.
        File file = new File(getApplicationContext().getFilesDir(), "walletFilePath.txt");
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        setSendFundsFeature();
        setPasswordSubmitButtonOnClickListener(findViewById(R.id.submit_password));
        setEthereumConnection();
    }

    private void setSendFundsFeature() {
        toggleSendFundsFeatureVisibility(false);
        EditText destinationAddress = findViewById(R.id.destination_address);
        EditText fundsToSend = findViewById(R.id.funds_to_send);
        Button fundsToSendButton = findViewById(R.id.send_funds_button);
        fundsToSendButton.setOnClickListener(view -> {
            if (web3 == null || credentials == null) {
                showToast("Connection to Ethereum has not been established. Please wait.", false);

            }
            try {
                TransactionReceipt receipt = Transfer.sendFunds(web3, credentials, destinationAddress.getText().toString(), BigDecimal.valueOf(Double.parseDouble(fundsToSend.getText().toString())), Convert.Unit.ETHER).send();
                showToast("TRANSACTION SUCCESSFUL: " + receipt.getTransactionHash(), true);

                // update the balance displayed.
                updateBalance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Add an onclick listener to the password submit button. Create wallet
     * if not yet exist.
     */
    @SuppressLint("SetTextI18n")
    private void setPasswordSubmitButtonOnClickListener(Button button) {
        button.setOnClickListener(view -> {
            if (web3 == null) {
                showToast("Connection to Ethereum has not been established. Please wait.", false);
                return;
            }

            String password = ((EditText)findViewById(R.id.password)).getText().toString();
            if (password.isEmpty()) {
                showToast("password cannot be empty.", true);
                return;
            }

            try {
                // check if walletFilePath.txt has any contents in it.
                String walletFilePath =
                        readTextData(new File(getApplicationContext().getFilesDir(), "walletFilePath.txt"));

                // create a new wallet if there's no existing wallet.
                if (walletFilePath.isEmpty()) {
                    // create wallet.
                    Bip39Wallet wallet = WalletUtils.generateBip39Wallet(password, getApplicationContext().getFilesDir());
                    TextView mnemonicTextView = findViewById(R.id.mnemonic);
                    mnemonicTextView.setText("Mnemonic: " + wallet.getMnemonic());
                    walletFilePath = getApplicationContext().getFilesDir().getAbsolutePath()+ "/" + wallet.getFilename();
                    writeTextData(
                            new File(getApplicationContext().getFilesDir(),
                                    "walletFilePath.txt"),
                            walletFilePath
                    );
                }
                credentials = WalletUtils.loadCredentials(password, new File(walletFilePath));
                TextView addressTextView = findViewById(R.id.address);
                addressTextView.setText("Address: " + credentials.getAddress());
                Log.d("ADDRESS", credentials.getAddress());

                // display the balance now that we have credentials and access.
                updateBalance();

                toggleSendFundsFeatureVisibility(true);
            } catch (CipherException e) {
                showToast("Wrong password", true);
                e.printStackTrace();
            } catch (IOException | ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateBalance() throws ExecutionException, InterruptedException {
        EthGetBalance balanceWei = web3.ethGetBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST).sendAsync().get();
        TextView balanceTextView = findViewById(R.id.balance);
        balanceTextView.setText("balance: " + balanceWei.getBalance().doubleValue() / Math.pow(10, 18) + " ETH");
    }

    private void setEthereumConnection() {
        web3 = Web3j.build(new HttpService("https://goerli.infura.io/v3/10529f878e4645329d47f4d8514b17e0"));
        try {
            //if the client version has an error the user will not gain access if successful the user will get connnected
            Web3ClientVersion clientVersion = web3.web3ClientVersion().sendAsync().get();
            if (!clientVersion.hasError()) {
                showToast("Connected to the Ethereum network", true);
            } else {
                showToast(clientVersion.getError().getMessage(), true);
            }
        } catch (Exception e) {
            showToast(e.getMessage(), true);
        }
    }

    // writeTextData() method save the data into the file in byte format
    private void writeTextData(File file, String data) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(data.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // readTextData() is the method which reads the data
    // the data that is saved in byte format in the file
    private String readTextData(File myfile) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(myfile);
            int i = -1;
            StringBuffer buffer = new StringBuffer();
            while ((i = fileInputStream.read()) != -1) {
                buffer.append((char) i);
            }
            return buffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private void showToast(String msg, boolean isLong) {
        Toast.makeText(getApplicationContext(), msg, isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }

    private void toggleSendFundsFeatureVisibility(boolean show) {
        EditText destinationAddress = findViewById(R.id.destination_address);
        EditText fundsToSend = findViewById(R.id.funds_to_send);
        Button fundsToSendButton = findViewById(R.id.send_funds_button);
        destinationAddress.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        fundsToSend.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        fundsToSendButton.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }
}