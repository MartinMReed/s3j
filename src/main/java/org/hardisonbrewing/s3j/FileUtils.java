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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.codehaus.plexus.util.IOUtil;

public class FileUtils extends org.codehaus.plexus.util.FileUtils {

    protected FileUtils() {

        // hide constructor
    }

    public static File createTempFile() throws IOException {

        File file = File.createTempFile( "s3j-", ".xml" );
        file.deleteOnExit();
        return file;
    }

    public static void printLines( File manifest ) throws IOException {

        InputStream inputStream = null;

        try {

            inputStream = new FileInputStream( manifest );
            Reader reader = new InputStreamReader( inputStream );
            BufferedReader bufferedReader = new BufferedReader( reader );

            String path;

            while (( path = bufferedReader.readLine() ) != null) {
                System.err.println( "  " + path );
            }
        }
        finally {
            IOUtil.close( inputStream );
        }
    }
}
