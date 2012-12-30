/**
 * Copyright (c) 2013 Martin M Reed
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

import javax.xml.bind.JAXBException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;
import org.codehaus.plexus.util.IOUtil;
import org.hardisonbrewing.jaxb.JAXB;

public class JaxbResponseHandler<T> implements ResponseHandler<T> {

    private final Class<T> clazz;

    public JaxbResponseHandler(Class<T> clazz) {

        this.clazz = clazz;
    }

    @Override
    public T handleResponse( HttpResponse httpResponse ) throws HttpResponseException, IOException {

        System.out.println( "  Response Headers" );
        HttpUtil.printHeaders( httpResponse );

        HttpUtil.validateResponseCode( httpResponse );

        HttpEntity entity = null;
        InputStream inputStream = null;

        try {

            entity = httpResponse.getEntity();
            inputStream = entity.getContent();
            return JAXB.unmarshal( inputStream, clazz );
        }
        catch (JAXBException e) {
            throw new IOException( e );
        }
        finally {
            IOUtil.close( inputStream );
            EntityUtils.consume( entity );
        }
    }
}
