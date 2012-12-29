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
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class FileScanner {

    public static interface Callback {

        public void found( File file ) throws IOException;
    }

    public String directory;
    public String[] includes;
    public String[] excludes;
    public boolean followSymlinks;
    public Callback callback;

    public final void scan() throws IOException {

        if ( !followSymlinks && isSymlink( this.directory ) ) {
            return;
        }

        Pattern[] includes = getPatterns( this.includes );
        Pattern[] excludes = getPatterns( this.excludes );

        List<String> directories = new LinkedList<String>();
        directories.add( this.directory );

        while (!directories.isEmpty()) {

            File directory = new File( directories.remove( 0 ) );
            File[] files = directory.listFiles();

            if ( files == null || files.length == 0 ) {
                continue;
            }

            for (File file : files) {

                if ( !followSymlinks && isSymlink( file ) ) {
                    continue;
                }

                if ( file.isDirectory() ) {
                    String filePath = file.getAbsolutePath();
                    directories.add( filePath );
                    continue;
                }

                boolean include = included( includes, file );
                boolean exclude = included( excludes, file );

                if ( include && !exclude ) {
                    callback.found( file );
                }
            }
        }
    }

    private boolean isSymlink( String file ) throws IOException {

        return isSymlink( new File( file ) );
    }

    private boolean isSymlink( File file ) throws IOException {

        File parentFile = file.getParentFile();

        if ( parentFile != null ) {
            File parentDirectory = parentFile.getCanonicalFile();
            file = new File( parentDirectory, file.getName() );
        }

        File canonicalFile = file.getCanonicalFile();
        File absoluteFile = file.getAbsoluteFile();
        return !canonicalFile.equals( absoluteFile );
    }

    private boolean included( Pattern[] patterns, File file ) {

        if ( patterns == null || patterns.length == 0 ) {
            return false;
        }

        String fileName = file.getName();
        String filePath = file.getAbsolutePath();

        for (Pattern pattern : patterns) {
            if ( pattern.matcher( fileName ).matches() ) {
                return true;
            }
            if ( pattern.matcher( filePath ).matches() ) {
                return true;
            }
        }

        return false;
    }

    private Pattern[] getPatterns( String[] regex ) {

        if ( regex == null || regex.length == 0 ) {
            return null;
        }

        Pattern[] patterns = new Pattern[regex.length];
        for (int i = 0; i < regex.length; i++) {
            patterns[i] = Pattern.compile( regex[i] );
        }
        return patterns;
    }
}
