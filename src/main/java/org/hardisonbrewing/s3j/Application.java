/**
 * Copyright (c) 2012-2013 Martin M Reed
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.codehaus.plexus.util.IOUtil;
import org.hardisonbrewing.s3j.FileSyncer.PutObjectResult;
import org.hardisonbrewing.schemas.model.Configuration;
import org.hardisonbrewing.schemas.model.Resource;

public class Application {

    private final byte[] newline = "\n".getBytes();

    public static void main( String[] args ) throws Exception {

        if ( args == null || args.length != 1 ) {
            throw new IllegalArgumentException();
        }

        Configuration configuration = ConfigUtil.loadConfiguration( args[0] );

        Application application = new Application();
        application.execute( configuration );
    }

    private void execute( Configuration configuration ) throws IOException, JAXBException {

        System.out.println( "Scanning..." );
        File manifest = scan( configuration );

        System.out.println( "Syncing..." );
        File errorLog = sync( configuration, manifest );

        System.out.println( "Finished!" );

        if ( errorLog.length() > 0 ) {
            System.err.println( "Errors found. Unable to sync files:" );
            FileUtils.printLines( errorLog );
        }
    }

    private File sync( Configuration configuration, File manifest ) throws IOException {

        FileSyncer fileSyncer = new FileSyncer();
        fileSyncer.accessKey = configuration.getAccessKey();
        fileSyncer.accessKeyId = configuration.getAccessKeyId();
        fileSyncer.bucket = configuration.getBucket();
        fileSyncer.privateKey = ConfigUtil.getPrivateKey( configuration );

        // test
//        try {
//            fileSyncer.list( FileSyncer.DOT_S3J );
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }

        File errorLog = FileUtils.createTempFile();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {

            inputStream = new FileInputStream( manifest );
            Reader reader = new InputStreamReader( inputStream );
            BufferedReader bufferedReader = new BufferedReader( reader );

            outputStream = new FileOutputStream( errorLog );

            String path;

            while (( path = bufferedReader.readLine() ) != null) {

                File file = new File( path );

                try {
                    PutObjectResult response = fileSyncer.put( file, true );
                    fileSyncer.get( response.bucketPath, true );
                }
                catch (Exception e) {

                    System.err.println( "Error: \"" + path + "\"" );
                    e.printStackTrace();

                    outputStream.write( path.getBytes() );
                    outputStream.write( newline );
                }
            }
        }
        finally {
            IOUtil.close( inputStream );
            IOUtil.close( outputStream );
        }

        return errorLog;
    }

    private File scan( Configuration configuration ) throws IOException {

        File file = FileUtils.createTempFile();

        FileScanner fileScanner = new FileScanner();
        List<Resource> resources = ConfigUtil.getResources( configuration );

        OutputStream outputStream = null;

        try {

            outputStream = new FileOutputStream( file );
            fileScanner.callback = new FileScannerCallback( outputStream );

            for (Resource resource : resources) {
                fileScanner.directory = ConfigUtil.getDirectory( resource );
                fileScanner.includes = ConfigUtil.getIncludes( resource );
                fileScanner.excludes = ConfigUtil.getExcludes( resource );
                fileScanner.followSymlinks = false;
                fileScanner.scan();
            }
        }
        finally {
            IOUtil.close( outputStream );
        }

        return file;
    }

    private class FileScannerCallback implements FileScanner.Callback {

        private final OutputStream outputStream;

        public FileScannerCallback(OutputStream outputStream) {

            this.outputStream = outputStream;
        }

        @Override
        public void found( File file ) throws IOException {

            String filePath = file.getAbsolutePath();
            outputStream.write( filePath.getBytes() );
            outputStream.write( newline );
            System.out.println( "Found: " + file );
        }
    }
}
