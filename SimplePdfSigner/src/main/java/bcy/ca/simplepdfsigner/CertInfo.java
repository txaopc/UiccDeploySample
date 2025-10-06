package bcy.ca.vgcacrypto;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

public class CertInfo {
    private X509Certificate _cert = null;

    public X509Certificate getX509()
    {

        return _cert;
    }

    private byte[] getHash(X509Certificate cert) throws NoSuchAlgorithmException, CertificateEncodingException {
        final MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(cert.getEncoded());
        return md.digest();
    }
    public String getThumbprint()
    {
        try {
            return Base64.getEncoder().encodeToString(getHash(_cert));
        } catch (Exception e) {
            return null;
        }
    }

    private String getValByAttributeTypeFromIssuerDN(String dn, String attributeType)
    {
        String[] dnSplits = dn.split(",");
        for (String dnSplit : dnSplits)
        {
            if (dnSplit.contains(attributeType))
            {
                String[] cnSplits = dnSplit.trim().split("=");
                if(cnSplits[1]!= null)
                {
                    return cnSplits[1].trim();
                }
            }
        }
        return "";
    }

    public static String getCNCert(X509Certificate mycert) {
        String dn = mycert.getSubjectX500Principal().getName();

        String[] dnSplits = dn.split(",");
        for (String dnSplit : dnSplits)
        {
            if (dnSplit.contains("CN="))
            {
                String[] cnSplits = dnSplit.trim().split("=");
                if(cnSplits[1]!= null)
                {
                    return cnSplits[1].trim();
                }
            }
        }
        return "";
    }
    public String getIssuerName()
    {
        return getValByAttributeTypeFromIssuerDN(_cert.getIssuerX500Principal().getName(),"CN=");
    }

    public String getO()
    {
        return getValByAttributeTypeFromIssuerDN(_cert.getSubjectX500Principal().getName(), "O=");
    }

    public String getOU()
    {
        String dn = _cert.getSubjectX500Principal().getName();
        String[] dnSplits = dn.split(",");
        String ou = "";
        for (String dnSplit : dnSplits)
        {
            if (dnSplit.indexOf("OU=")==0)
            {
                if(ou=="") {
                    String[] cnSplits = dnSplit.trim().split("=");
                    if(cnSplits[1]!= null)
                    {
                        ou = cnSplits[1].trim();
                    }
                }else {
                    String[] cnSplits = dnSplit.trim().split("=");
                    if(cnSplits[1]!= null)
                    {
                        ou = cnSplits[1].trim()+", "+ou;
                    }
                }
            }
        }
        return ou;

    }

    public String getSerialNumber() {
        return	_cert.getSerialNumber().toString(16).toUpperCase();
    }
    public String getEmail()
    {

        try {
            if (_cert.getSubjectAlternativeNames() != null) {
                for (List<?> item : _cert.getSubjectAlternativeNames()) {
                    Integer type = (Integer) item.get(0);
                    if (type == 1) {
                        return (String) item.get(1);
                    }
                }
            }
        } catch (CertificateParsingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "";
    }

    public String getCommonName()
    {
        return getValByAttributeTypeFromIssuerDN(_cert.getSubjectX500Principal().getName(), "CN=");
    }
    public String getSurname()
    {
        return getValByAttributeTypeFromIssuerDN(_cert.getSubjectX500Principal().getName(), "SN=");
    }
    public String getLocation()
    {

        return getValByAttributeTypeFromIssuerDN(_cert.getSubjectX500Principal().getName(), "L=");
    }

    public CertInfo(byte[] rawData)
    {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            _cert = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(rawData));

        } catch (java.security.cert.CertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public CertInfo(X509Certificate cert)
    {
        _cert = cert;
    }

    public  String toString()
    {

        return String.format("{0}<{1}>", this.getCommonName(), this.getEmail());
    }

    public static boolean IsSelfSigned(X509Certificate cert)
    {
        try
        {
            if (!cert.getSubjectX500Principal().getName().equals(cert.getIssuerX500Principal().getName()))
                return false;
            cert.verify(cert.getPublicKey());
            return true;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }
}
