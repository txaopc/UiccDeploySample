package bcy.ca.vgcacrypto;

import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.signatures.ExternalBlankSignatureContainer;

import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

public class ExternalPrecalculatedSignatureContainer extends ExternalBlankSignatureContainer
{
    byte[] cmsSignatureContents;

    public ExternalPrecalculatedSignatureContainer(byte[] cmsSignatureContents) {
        super(new PdfDictionary());
        this.cmsSignatureContents = cmsSignatureContents;
    }

    @Override
    public  byte[] sign(InputStream data) throws CertificateException, InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException {
        return cmsSignatureContents;
    }
}
