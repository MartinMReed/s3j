package org.hardisonbrewing.s3j;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class AesUtil {

    public static final String ALGORITHM = "AES";

    public static byte[] generateKey() throws Exception {

        KeyGenerator keyGenerator = KeyGenerator.getInstance( ALGORITHM );
        keyGenerator.init( 128 );

        SecretKey secretKey = keyGenerator.generateKey();
        return secretKey.getEncoded();
    }
}
