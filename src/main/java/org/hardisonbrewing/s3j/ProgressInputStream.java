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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.input.CountingInputStream;

public class ProgressInputStream extends CountingInputStream {

    private long length;

    public ProgressInputStream(InputStream inputStream, long length) {

        super( inputStream );

        this.length = length;
    }

    private void updateProgress() {

        long read = getByteCount();

        int progress = (int) ( ( read / (double) length ) * 100 );

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append( "|" );
        for (int i = 0; i < 100; i++) {
            if ( i <= progress ) {
                stringBuffer.append( "=" );
            }
            else {
                stringBuffer.append( " " );
            }
        }
        stringBuffer.append( "| " );
        stringBuffer.append( progress );
        stringBuffer.append( "%\r" );
        System.out.print( stringBuffer.toString() );
    }

    @Override
    public int read() throws IOException {

        int read = super.read();
        updateProgress();
        return read;
    }

    @Override
    public int read( byte[] b ) throws IOException {

        int read = super.read( b );
        updateProgress();
        return read;
    }

    @Override
    public int read( byte[] b, int off, int len ) throws IOException {

        int read = super.read( b, off, len );
        updateProgress();
        return read;
    }
}
