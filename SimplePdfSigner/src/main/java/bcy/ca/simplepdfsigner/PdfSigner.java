package bcy.ca.vgcacrypto;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.IExternalDigest;
import com.itextpdf.signatures.ITSAClient;
import com.itextpdf.signatures.PdfPKCS7;
import com.itextpdf.signatures.PdfSignatureAppearance;
import com.itextpdf.kernel.geom.Rectangle;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Base64;

public class PdfSigner {

    static {
        BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
    }
    int estimatedSize = 8192;
    String hashAlgorithm = "SHA-256";
    String sigName;

    public PdfSigner(){

    }

    public String getSigName(){
        return sigName;
    }
    /**
     * Tính giá trị hash nội dung file PDF để ký số
     *
     * @param in       InputStream file PDF cần ký
     * @param preparedArrayStream OutputStream để ghi nội dung PDF sau khi đã thiết lập hiển thị chữ ký số
     * @param signerCert         Certificate chứng thư số người ký
     * @param page   int trang đặt chữ ký số
     * @param rect   Rectangle khung kích thước chữ ký số
     * @param sigImage   ImageData hình ảnh chữ ký số
     * @throws IOException if some I/O problem occurs
     * @throws GeneralSecurityException if some problem during apply security algorithms occurs
     */
    public byte[] prepareSignature(InputStream in, OutputStream preparedArrayStream,
                                   Certificate signerCert,
                                   int page, Rectangle rect, ImageData sigImage) throws IOException, GeneralSecurityException {
        PdfReader reader = new PdfReader(in);
        com.itextpdf.signatures.PdfSigner signer = new com.itextpdf.signatures.PdfSigner(reader, preparedArrayStream,  new StampingProperties().useAppendMode());
        sigName = signer.getNewSigFieldName();
        signer.setFieldName(sigName);
        signer.setCertificationLevel(com.itextpdf.signatures.PdfSigner.CERTIFIED_FORM_FILLING);

        //Viêc thiết lập hiển thị CKS thì đơn vị phát triển tùy biến theo yêu cầu tại đây
        PdfSignatureAppearance appearance = signer.getSignatureAppearance();
        appearance.setPageRect(rect)
                .setPageNumber(page)
                .setLocation("Hà Nội")
                .setReason("Ký số văn bản điện tử")
                .setCertificate(signerCert)
                .setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC);
        appearance = appearance.setSignatureGraphic(sigImage);

        PreSignatureContainer external = new PreSignatureContainer(PdfName.Adobe_PPKLite, PdfName.ETSI_CAdES_DETACHED);
        signer.signExternalContainer(external, estimatedSize);
        //return Base64.getEncoder().encodeToString(external.getHash());
        return external.getHash();
    }

    public byte[] getDataTobeSign(byte[] hash, Certificate signerCert) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        IExternalDigest externalDigest = new BouncyCastleDigest();
        Certificate[] chain = new Certificate[] { signerCert };
        PdfPKCS7 sgn = new PdfPKCS7((PrivateKey) null, chain, hashAlgorithm, null, externalDigest, false);
        byte[] sh = sgn.getAuthenticatedAttributeBytes(hash, com.itextpdf.signatures.PdfSigner.CryptoStandard.CADES, null, null);
        //return Base64.getEncoder().encodeToString(sh);
        return sh;
    }

    public String getEncodedPKCS7(byte[] hash, Certificate signerCert, byte[] extSignature, ITSAClient tsc) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        IExternalDigest externalDigest = new BouncyCastleDigest();
        Certificate[] chain = new Certificate[] { signerCert };
        String signatureAlg = signerCert.getPublicKey().getAlgorithm();

        PdfPKCS7 sgn = new PdfPKCS7((PrivateKey) null, chain, hashAlgorithm, null, externalDigest, false);
        sgn.setExternalDigest(extSignature, null, signatureAlg);
        byte[] signedContent = sgn.getEncodedPKCS7(hash, com.itextpdf.signatures.PdfSigner.CryptoStandard.CADES, tsc, null, null);
        return Base64.getEncoder().encodeToString(signedContent);
    }
    public void encapsulateSignature(InputStream in,String path, String encodedPKCS7) throws Exception {
        byte[] decodeSignature = Base64.getDecoder().decode(encodedPKCS7);
        PdfReader reader = new PdfReader(in);
        FileOutputStream outputStream = new FileOutputStream(path);
        com.itextpdf.signatures.PdfSigner.signDeferred(new PdfDocument(reader), sigName, outputStream, new ExternalPrecalculatedSignatureContainer(decodeSignature));
        outputStream.close();
    }
}
