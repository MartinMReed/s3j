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

import org.apache.http.Header;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;

public class HttpUtil {

    protected HttpUtil() {

        // hide constructor
    }

    public static void printHeaders( HttpMessage httpMessage ) {

        for (Header header : httpMessage.getAllHeaders()) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append( "    " );
            stringBuffer.append( header.getName() );
            stringBuffer.append( ": " );
            stringBuffer.append( header.getValue() );
            System.out.println( stringBuffer.toString() );
        }
    }

    public static void validateResponseCode( HttpResponse httpResponse ) throws HttpResponseException {

        StatusLine statusLine = httpResponse.getStatusLine();
        int statusCode = statusLine.getStatusCode();

        if ( statusCode < 200 || statusCode >= 300 ) {
            String reasonPhrase = statusLine.getReasonPhrase();
            throw new HttpResponseException( statusCode, reasonPhrase );
        }
    }
}
