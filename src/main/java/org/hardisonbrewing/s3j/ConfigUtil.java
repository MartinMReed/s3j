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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.codehaus.plexus.util.IOUtil;
import org.hardisonbrewing.jaxb.JAXB;
import org.hardisonbrewing.schemas.model.Configuration;
import org.hardisonbrewing.schemas.model.Configuration.Resources;
import org.hardisonbrewing.schemas.model.Resource;
import org.hardisonbrewing.schemas.model.Resource.Excludes;
import org.hardisonbrewing.schemas.model.Resource.Includes;

public final class ConfigUtil {

    private ConfigUtil() {

        // hide constructor
    }

    public static Configuration loadConfiguration( String path ) throws IOException, JAXBException {

        File file = new File( path );

        if ( !file.exists() ) {
            throw new FileNotFoundException( "Configuration file not found: " + file );
        }

        InputStream inputStream = null;

        try {
            inputStream = new FileInputStream( file );
            return JAXB.unmarshal( inputStream, Configuration.class );
        }
        finally {
            IOUtil.close( inputStream );
        }
    }

    public static File getPrivateKey( Configuration configuration ) throws FileNotFoundException {

        String filePath = configuration.getPrivateKey();
        if ( filePath == null || filePath.length() == 0 ) {
            return null;
        }
        filePath = FileUtils.resolveDirectory( filePath );
        File file = new File( filePath );
        if ( !file.exists() ) {
            throw new FileNotFoundException( filePath );
        }
        return file;
    }

    public static String getDirectory( Resource resource ) {

        String directory = resource.getDirectory();
        return FileUtils.resolveDirectory( directory );
    }

    public static List<Resource> getResources( Configuration configuration ) {

        Resources resources = configuration.getResources();
        if ( resources == null ) {
            throw new IllegalStateException();
        }
        List<Resource> _resources = resources.getResource();
        if ( _resources == null || _resources.size() == 0 ) {
            throw new IllegalStateException();
        }
        return _resources;
    }

    public static String[] getIncludes( Resource resource ) {

        Includes includes = resource.getIncludes();
        if ( includes == null ) {
            return null;
        }
        List<String> _includes = includes.getInclude();
        if ( _includes == null || _includes.size() == 0 ) {
            return null;
        }
        String[] __includes = new String[_includes.size()];
        _includes.toArray( __includes );
        return __includes;
    }

    public static String[] getExcludes( Resource resource ) {

        Excludes excludes = resource.getExcludes();
        if ( excludes == null ) {
            return null;
        }
        List<String> _excludes = excludes.getExclude();
        if ( _excludes == null || _excludes.size() == 0 ) {
            return null;
        }
        String[] __excludes = new String[_excludes.size()];
        _excludes.toArray( __excludes );
        return __excludes;
    }
}
