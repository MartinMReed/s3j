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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;
import org.codehaus.plexus.util.IOUtil;

public class FileResponseHandler implements ResponseHandler<File> {

    public Cipher cipher;
    public long contentLength;

    @Override
    public File handleResponse( HttpResponse httpResponse ) throws HttpResponseException, IOException {

        System.out.println( "  Response Headers" );
        HttpUtil.printHeaders( httpResponse );

        HttpUtil.validateResponseCode( httpResponse );

        File file = FileUtils.createTempFile();

        long contentLength = 0;

        HttpEntity entity = null;
        InputStream inputStream = null;
        CountingInputStream countingInputStream = null;
        OutputStream outputStream = null;

        try {

            entity = httpResponse.getEntity();

            contentLength = entity.getContentLength();
            validateContentLength( contentLength );

            inputStream = entity.getContent();

            inputStream = new ProgressInputStream( inputStream, contentLength );

            // put this before the cipher so we get the encrypted length
            countingInputStream = new CountingInputStream( inputStream );
            inputStream = countingInputStream;

            if ( cipher != null ) {
                inputStream = new CipherInputStream( inputStream, cipher );
            }

            outputStream = new FileOutputStream( file );

            IOUtil.copy( inputStream, outputStream );
        }
        finally {
            IOUtil.close( inputStream );
            EntityUtils.consume( entity );
            IOUtil.close( outputStream );
        }

        long readLength = countingInputStream.getByteCount();
        validateDownloadLength( contentLength, readLength );

        return file;
    }

    private void validateContentLength( long contentLength ) throws IOException {

        if ( this.contentLength > 0 && contentLength != this.contentLength ) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append( "Server reported content-length (" );
            stringBuffer.append( contentLength );
            stringBuffer.append( ") does not match the expected content-length (" );
            stringBuffer.append( this.contentLength );
            stringBuffer.append( ")." );
            throw new IOException( stringBuffer.toString() );
        }
    }

    private void validateDownloadLength( long expected, long actual ) throws IOException {

        if ( expected != actual ) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append( "Downloaded size (" );
            stringBuffer.append( actual );
            stringBuffer.append( ") does not match the expected content-length (" );
            stringBuffer.append( expected );
            stringBuffer.append( ")." );
            throw new IOException( stringBuffer.toString() );
        }
    }
}
