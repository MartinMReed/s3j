package org.hardisonbrewing.s3j;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.KeyPair;
import java.security.Security;

import javax.crypto.Cipher;

import org.bouncycastle.openssl.PEMReader;
import org.codehaus.plexus.util.IOUtil;

public class RsaUtil {

    private static final String ALGORITHM = "RSA";

    public static byte[] encrypt( File privateKey, byte[] data ) throws Exception {

        KeyPair keyPair = loadKeyPair( privateKey );

        Cipher cipher = Cipher.getInstance( ALGORITHM );
        cipher.init( Cipher.ENCRYPT_MODE, keyPair.getPublic() );
        return cipher.doFinal( data );
    }

    public static byte[] decrypt( File privateKey, byte[] data ) throws Exception {

        KeyPair keyPair = loadKeyPair( privateKey );

        Cipher cipher = Cipher.getInstance( ALGORITHM );
        cipher.init( Cipher.DECRYPT_MODE, keyPair.getPrivate() );
        return cipher.doFinal( data );
    }

    public static KeyPair loadKeyPair( File privateKey ) throws Exception {

        Security.addProvider( new org.bouncycastle.jce.provider.BouncyCastleProvider() );

        InputStream inputStream = null;

        try {
            inputStream = new FileInputStream( privateKey );
            Reader reader = new InputStreamReader( inputStream );
            PEMReader pemReader = new PEMReader( reader );
            return (KeyPair) pemReader.readObject();
        }
        finally {
            IOUtil.close( inputStream );
        }
    }
}
