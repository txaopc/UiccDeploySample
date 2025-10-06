package vn.mobileid.uiccdeploy;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;


import com.google.android.gms.common.util.Base64Utils;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.IExternalDigest;
import com.itextpdf.signatures.ITSAClient;
import com.itextpdf.signatures.PdfPKCS7;
import com.itextpdf.signatures.TSAClientBouncyCastle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import vn.mobileid.uiccsdk.Uicc;
import vn.mobileid.uiccsdk.Uiccsdk;
import vn.mobileid.uiccsdk.plugin.APDUResult;
import vn.mobileid.uiccsdk.plugin.Callback;
import bcy.ca.vgcacrypto.PdfSigner;

public class MainActivity extends AppCompatActivity {

    PdfSigner pdfSigner;
    byte[] pdfHash;
    Certificate signerCert;
    ImageData sigImg;

    String pdfTmpFile;
    String pdfSignedFile;
    File fileTmp;

    String tsaUrl = "http://tsa.ca.gov.vn";
    String digestAlgorithm = "SHA256";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EditText pin = findViewById(R.id.pin);
        EditText signhash = findViewById(R.id.signHash);
        EditText log = findViewById(R.id.etLog);

        Uiccsdk uiccsdk = new Uiccsdk();
        pdfSigner = new PdfSigner();

        pdfTmpFile = "___pdfDefferred.pdf";
        pdfSignedFile = "TestSigned.pdf";
        //File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        fileTmp = new File(getFilesDir(), pdfTmpFile);


        findViewById(R.id.run).setOnClickListener(v -> {

            //1. Get ImageData
            AssetManager assetManager = getAssets();

            //Load test chứng thư CKS người ký
            try (InputStream inputStream = assetManager.open("hoan.cer")) {
                // Read the InputStream into a byte array
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                signerCert = cf.generateCertificate(inputStream);
            } catch (IOException | CertificateException e) {
                throw new RuntimeException(e);
            }

            byte[] secondHash;
            try (
                    //Hình ảnh chữ ký số
                    InputStream imgStream = assetManager.open("thao.png");

                    //Văn bản đầu vào cần ký
                    InputStream resource = assetManager.open("vanbanmau.pdf");
                    FileOutputStream preparedStream = new FileOutputStream(fileTmp)
            ) {
                BouncyCastleDigest digest = new BouncyCastleDigest();

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = imgStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }
                byte[] imageBytes = byteArrayOutputStream.toByteArray();
                // Create ImageData from the byte array
                sigImg = ImageDataFactory.create(imageBytes);

                // deferred signing, step one
                pdfHash = pdfSigner.prepareSignature(resource, preparedStream,
                        signerCert, 1, new Rectangle(0,0, 180, 70), sigImg);
                // prepare PdfPKCS7 with document hash
                byte[] sh = pdfSigner.getDataTobeSign(pdfHash, signerCert);
                try (ByteArrayInputStream hashStream = new ByteArrayInputStream(sh)){
                    secondHash = DigestAlgorithms.digest(hashStream, digest.getMessageDigest(digestAlgorithm));
                }

            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException(e);
            }

            uiccsdk.signHash(this, pin.getText().toString(), Base64.getEncoder().encodeToString(secondHash), new Callback<APDUResult>() {
                @Override
                public void process(APDUResult apduResult) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            //TODO your background code
                            if(apduResult.code.equals("9000")){

                                //Tích hợp TSA vào chữ ký số
                                try(FileInputStream fileInputStream = new FileInputStream(fileTmp)) {
                                    ITSAClient tsc = new TSAClientBouncyCastle(tsaUrl, null, null, 4096, digestAlgorithm);
                                    //ITSAClient tsc = null;
                                    String encodedPKCS7 = pdfSigner.getEncodedPKCS7(pdfHash, signerCert, Base64.getMimeDecoder().decode(apduResult.signature), tsc);
                                    Log.i("uiccdeploy", fileTmp.getAbsolutePath());
                                    pdfSigner.encapsulateSignature(fileInputStream, new File(getFilesDir(), pdfSignedFile).getAbsolutePath(), encodedPKCS7);

                                    Log.i("uiccdeploy", new File(getFilesDir(), pdfSignedFile).getAbsolutePath());


                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    });

                    log.setText(new File(getFilesDir(), pdfSignedFile).getAbsolutePath());

                    log.post(new Runnable() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            log.setText(log.getText().toString() + "\n" +
                                    "code: " + apduResult.code + "\n" +
                                    "message: " + apduResult.message + "\n" +
                                    "signature: " + apduResult.signature + "\n" +
                                    "remainingCounter: " + apduResult.remainingCounter + "\n"
                            );

                            Log.i("uiccdeploy", "code: " + apduResult.code + "\n" +
                                    "message: " + apduResult.message + "\n" +
                                    "signature: " + apduResult.signature + "\n" +
                                    "remainingCounter: " + apduResult.remainingCounter);


                        }
                    });



                }
            });

        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}