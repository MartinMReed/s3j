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

import java.util.Properties;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;

public class DataProperties extends Properties {

    private static final long serialVersionUID = 3264378238193269071L;

    public byte[] getBinaryProperty( String key ) throws DecoderException {

        String value = getProperty( key );
        if ( value == null || value.length() == 0 ) {
            return null;
        }
        return Base64.decodeBase64( value );
    }

    public void put( String key, byte[] value ) {

        put( key, Base64.encodeBase64String( value ) );
    }

    public long getLongProperty( String key ) {

        String value = getProperty( key );
        if ( value == null || value.length() == 0 ) {
            return 0;
        }
        return Long.parseLong( value );
    }

    public void put( String key, long value ) {

        put( key, Long.toString( value ) );
    }
}
