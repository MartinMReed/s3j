/**
 * Copyright (c) 2012 Martin M Reed
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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
