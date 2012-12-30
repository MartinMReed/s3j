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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.codehaus.plexus.util.IOUtil;

import com.amazonaws.s3.doc._2006_03_01.ListBucketResult;
import com.amazonaws.s3.doc._2006_03_01.ListEntry;

public class FileSyncer {

    private static final String PROP_ORIG_FILE_PATH = "original.file.path";
    private static final String PROP_LAST_MODIFIED = "last.modified";
    private static final String PROP_KEY = "key";
    private static final String PROP_ENCRYPTED_LENGTH = "encrypted.length";
    private static final String PROP_DECRYPTED_LENGTH = "decrypted.length";
    private static final String PROP_ALGORITHM = "algorithm";

    /*package*/static final String DOT_S3J = ".s3j";

    private static final int MAX_KEYS = 25;

    private static final DateFormat amzDateFormat;

    static {
        amzDateFormat = new SimpleDateFormat( "E, dd MMM yyyy HH:mm:ss Z" );
    }

    public String accessKey;
    public String accessKeyId;
    public String bucket;
    public File privateKey;

    private final HttpClient httpClient;
    private MessageDigest md5Digest;

    public FileSyncer() {

        httpClient = new DefaultHttpClient();
    }

    public void close() {

        httpClient.getConnectionManager().shutdown();
    }

    // http://docs.amazonwebservices.com/AmazonS3/latest/API/APIRest.html
    public List<ListBucketObject> list( String prefix ) throws Exception {

        List<ListBucketObject> contents = new LinkedList<ListBucketObject>();
        while (list( contents, prefix )) {
            // loop
        }
        for (ListBucketObject object : contents) {
            System.out.println( object.key );
        }
        return contents;
    }

    // http://docs.amazonwebservices.com/AmazonS3/latest/API/APIRest.html
    private boolean list( List<ListBucketObject> contents, String prefix ) throws Exception {

        String url = getUrl( "/" );

        StringBuffer params = new StringBuffer();
        params.append( "?max-keys=" );
        params.append( MAX_KEYS );
        if ( !contents.isEmpty() ) {
            ListBucketObject last = contents.get( contents.size() - 1 );
            params.append( "&marker=" );
            params.append( last.key );
        }
        if ( prefix != null && prefix.length() > 0 ) {
            params.append( "&prefix=" );
            params.append( prefix );
        }
        url += params.toString();

        HttpRequestBase httpRequest = new HttpGet( url );
        addHeaders( httpRequest, null );

        System.out.println( "Downloading: " + url );
        HttpUtil.printHeaders( httpRequest );

        JaxbResponseHandler<ListBucketResult> jaxbResponseHandler;
        jaxbResponseHandler = new JaxbResponseHandler<ListBucketResult>( ListBucketResult.class );
        ListBucketResult listBucketResult = httpClient.execute( httpRequest, jaxbResponseHandler );

        List<ListEntry> _contents = listBucketResult.getContents();

        if ( _contents != null && !_contents.isEmpty() ) {
            for (ListEntry listEntry : _contents) {
                ListBucketObject object = new ListBucketObject();
                object.key = listEntry.getKey();
                object.etag = listEntry.getETag();
                object.lastModified = getLastModified( listEntry );
                object.size = listEntry.getSize();
                contents.add( object );
            }
        }

        return listBucketResult.isIsTruncated();
    }

    private long getLastModified( ListEntry listEntry ) {

        XMLGregorianCalendar xmlGregorianCalendar = listEntry.getLastModified();
        GregorianCalendar gregorianCalendar = xmlGregorianCalendar.toGregorianCalendar();
        Date date = gregorianCalendar.getTime();
        return date.getTime();
    }

    // http://docs.amazonwebservices.com/AmazonS3/latest/API/APIRest.html
    public GetObjectResult get( String path ) throws Exception {

        String url = getUrl( path );
        HttpRequestBase httpRequest = new HttpGet( url );
        addHeaders( httpRequest, path );

        System.out.println( "Downloading: " + url );
        HttpUtil.printHeaders( httpRequest );

        FileResponseHandler fileResponseHandler = new FileResponseHandler();
        File file = httpClient.execute( httpRequest, fileResponseHandler );

        GetObjectResult response = new GetObjectResult();
        response.file = file;
        response.bucketPath = path;
        return response;
    }

    // http://docs.amazonwebservices.com/AmazonS3/latest/API/APIRest.html
    public GetObjectResult get( String path, boolean decrypt ) throws Exception {

        if ( !decrypt || privateKey == null ) {
            return get( path );
        }

        DataProperties properties = downloadProperties( path );
        String algorithm = properties.getProperty( PROP_ALGORITHM );
        long decryptedLength = properties.getLongProperty( PROP_DECRYPTED_LENGTH );
        long encryptedLength = properties.getLongProperty( PROP_ENCRYPTED_LENGTH );
        byte[] encryptedKey = properties.getBinaryProperty( PROP_KEY );
        long lastModified = properties.getLongProperty( PROP_LAST_MODIFIED );
        String originalFilePath = properties.getProperty( PROP_ORIG_FILE_PATH );

        System.out.println( "  S3J Properties" );
        PropertiesUtil.printProperties( properties );

        byte[] rawKey = RsaUtil.decrypt( privateKey, encryptedKey );

        Cipher cipher = Cipher.getInstance( algorithm );
        cipher.init( Cipher.DECRYPT_MODE, new SecretKeySpec( rawKey, algorithm ) );

        String url = getUrl( path );
        HttpRequestBase httpRequest = new HttpGet( url );
        addHeaders( httpRequest, path );

        System.out.println( "Downloading: " + url );
        HttpUtil.printHeaders( httpRequest );

        FileResponseHandler fileResponseHandler = new FileResponseHandler();
        fileResponseHandler.contentLength = encryptedLength;
        fileResponseHandler.cipher = cipher;

        File file = httpClient.execute( httpRequest, fileResponseHandler );
        file.setLastModified( lastModified );

        long fileLength = file.length();

        if ( decryptedLength != fileLength ) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append( "Downloaded size (" );
            stringBuffer.append( fileLength );
            stringBuffer.append( ") does not match the expected decrypted length (" );
            stringBuffer.append( decryptedLength );
            stringBuffer.append( ")." );
            throw new IOException( stringBuffer.toString() );
        }

        GetObjectResult response = new GetObjectResult();
        response.file = file;
        response.properties = properties;
        response.bucketPath = path;
        response.originalPath = originalFilePath;
        return response;
    }

    private DataProperties downloadProperties( String path ) throws Exception {

        path = getPropertiesPath( path );
        GetObjectResult response = get( path );

        InputStream inputStream = null;

        try {

            inputStream = new FileInputStream( response.file );

            DataProperties properties = new DataProperties();
            properties.load( inputStream );
            return properties;
        }
        finally {
            IOUtil.close( inputStream );
        }
    }

    // http://docs.amazonwebservices.com/AmazonS3/latest/API/APIRest.html
    public HttpResponse head( String path ) throws Exception {

        String url = getUrl( path );
        HttpRequestBase httpRequest = new HttpHead( url );
        addHeaders( httpRequest, path );

        System.out.println( "Head: " + url );
        HttpUtil.printHeaders( httpRequest );

        return httpClient.execute( httpRequest );
    }

    public PutObjectResult put( File file ) throws Exception {

        String path = getBucketPath( file );
        long length = file.length();

        InputStream inputStream = null;

        try {
            inputStream = new FileInputStream( file );
            put( path, inputStream, length );
        }
        finally {
            IOUtil.close( inputStream );
        }

        PutObjectResult response = new PutObjectResult();
        response.file = file;
        response.bucketPath = path;
        return response;
    }

    public PutObjectResult put( File file, boolean encrypted ) throws Exception {

        if ( !encrypted || privateKey == null ) {
            return put( file );
        }

        long decryptedLength = file.length();
        String path = getBucketPath( file );

        byte[] rawKey = AesUtil.generateKey();
        Cipher cipher = Cipher.getInstance( AesUtil.ALGORITHM );
        cipher.init( Cipher.ENCRYPT_MODE, new SecretKeySpec( rawKey, AesUtil.ALGORITHM ) );

        int encryptedLength = cipher.getOutputSize( (int) file.length() );

        InputStream inputStream = null;

        try {
            inputStream = new FileInputStream( file );
            inputStream = new CipherInputStream( inputStream, cipher );
            put( path, inputStream, encryptedLength );
        }
        finally {
            IOUtil.close( inputStream );
        }

        byte[] encryptedKey = RsaUtil.encrypt( privateKey, rawKey );

        DataProperties properties = new DataProperties();
        properties.put( PROP_ALGORITHM, AesUtil.ALGORITHM );
        properties.put( PROP_DECRYPTED_LENGTH, decryptedLength );
        properties.put( PROP_ENCRYPTED_LENGTH, encryptedLength );
        properties.put( PROP_KEY, encryptedKey );
        properties.put( PROP_ORIG_FILE_PATH, file.getAbsolutePath() );
        properties.put( PROP_LAST_MODIFIED, file.lastModified() );
        uploadProperties( path, properties );

        PutObjectResult response = new PutObjectResult();
        response.file = file;
        response.properties = properties;
        response.bucketPath = path;
        return response;
    }

    private void uploadProperties( String path, Properties properties ) throws Exception {

        path = getPropertiesPath( path );

        ByteArrayOutputStream outputStream = null;
        byte[] bytes;

        try {
            outputStream = new ByteArrayOutputStream();
            properties.store( outputStream, null );
            bytes = outputStream.toByteArray();
        }
        finally {
            IOUtil.close( outputStream );
        }

        InputStream inputStream = null;

        try {
            inputStream = new ByteArrayInputStream( bytes );
            put( path.toString(), inputStream, bytes.length );
        }
        finally {
            IOUtil.close( inputStream );
        }
    }

    private String getPropertiesPath( String path ) {

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append( DOT_S3J );
        stringBuffer.append( File.separator );
        stringBuffer.append( path );
        return stringBuffer.toString();
    }

    // http://docs.amazonwebservices.com/AmazonS3/latest/API/APIRest.html
    public void put( String path, InputStream inputStream, long length ) throws Exception {

        String url = getUrl( path );

        HttpPut httpRequest = new HttpPut( url );
        httpRequest.addHeader( HTTP.EXPECT_DIRECTIVE, HTTP.EXPECT_CONTINUE );
        addHeaders( httpRequest, path );

        System.out.println( "Uploading: " + url );
        HttpUtil.printHeaders( httpRequest );

        if ( md5Digest == null ) {
            md5Digest = MessageDigest.getInstance( "MD5" );
        }
        else {
            md5Digest.reset();
        }

        HttpResponse httpResponse;
        HttpEntity httpEntity = null;

        try {
            inputStream = new ProgressInputStream( inputStream, length );
            inputStream = new DigestInputStream( inputStream, md5Digest );
            httpRequest.setEntity( new InputStreamEntity( inputStream, length ) );
            httpResponse = httpClient.execute( httpRequest );
            httpEntity = httpResponse.getEntity();
        }
        finally {
            EntityUtils.consume( httpEntity );
        }

        System.out.println( "  Response Headers" );
        HttpUtil.printHeaders( httpResponse );

        HttpUtil.validateResponseCode( httpResponse );

        byte[] digest = md5Digest.digest();
        String digestHex = Hex.encodeHexString( digest );

        String etag = getHeaderValue( httpResponse, "ETag" );

        if ( !etag.equals( digestHex ) ) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append( "Uploaded digest (" );
            stringBuffer.append( digestHex );
            stringBuffer.append( ") does not match ETag (" );
            stringBuffer.append( etag );
            stringBuffer.append( ")." );
            throw new IOException( stringBuffer.toString() );
        }
    }

    /*package*/static String getBucketPath( File file ) {

        StringBuffer stringBuffer = new StringBuffer();
        while (file != null && file.getParent() != null) {
            if ( stringBuffer.length() > 0 ) {
                stringBuffer.insert( 0, File.separator );
            }
            stringBuffer.insert( 0, file.getName() );
            file = file.getParentFile();
        }

        String path = stringBuffer.toString();
        path = path.replace( ' ', '_' );
        return path;
    }

    private String getHeaderValue( HttpResponse httpResponse, String name ) {

        Header header = httpResponse.getFirstHeader( name );
        String value = header.getValue();
        if ( value.startsWith( "\"" ) ) {
            value = value.substring( 1 );
        }
        if ( value.endsWith( "\"" ) ) {
            value = value.substring( 0, value.length() - 1 );
        }
        return value;
    }

    private String getUrl( String path ) throws Exception {

        if ( path != null && path.length() > 0 ) {
            path = "/" + path;
        }
        URI uri = new URI( "http", bucket + ".s3.amazonaws.com", path, null );
        return uri.toString();
    }

    private void addHeaders( HttpRequestBase httpRequest, String path ) throws Exception {

        long expires = System.currentTimeMillis() + ( 1000 * 60 );

        httpRequest.addHeader( "Host", bucket + ".s3.amazonaws.com" );
        httpRequest.addHeader( "x-amz-date", amzDateFormat( expires ) );

        String resource = "/" + bucket + "/";
        if ( path != null && path.length() > 0 ) {
            resource += path;
        }
        String signature = signature( accessKey, expires, resource, httpRequest );
        httpRequest.addHeader( "Authorization", "AWS " + accessKeyId + ":" + signature );

    }

    private String amzDateFormat( long date ) {

        return amzDateFormat.format( new Date( date ) );
    }

    // http://docs.amazonwebservices.com/AmazonS3/2006-03-01/dev/RESTAuthentication.html#RESTAuthenticationQueryStringAuth
    // http://associates-amazon.s3.amazonaws.com/signed-requests/helper/index.html
    private String signature( String accessKey, long expires, String resource, HttpRequestBase request ) throws Exception {

        StringBuffer stringBuffer = new StringBuffer();

        // HTTP-VERB
        stringBuffer.append( request.getMethod() );
        stringBuffer.append( "\n" );

        // Content-MD5
//        if ( !( request instanceof HttpEntityEnclosingRequestBase ) ) {
        stringBuffer.append( "\n" );
//        }
//        else {
//            HttpEntityEnclosingRequestBase entityRequest = (HttpEntityEnclosingRequestBase) request;
//            HttpEntity entity = entityRequest.getEntity();
//            stringBuffer.append( md5( IOUtil.toByteArray( entity.getContent() ) ) );
//            stringBuffer.append( "\n" );
//        }

        //Content-Type
//        if ( !( request instanceof HttpEntityEnclosingRequestBase ) ) {
        stringBuffer.append( "\n" );
//        }
//        else {
//            HttpEntityEnclosingRequestBase entityRequest = (HttpEntityEnclosingRequestBase) request;
//            HttpEntity entity = entityRequest.getEntity();
//            stringBuffer.append( entity.getContentType().getValue() );
//            stringBuffer.append( "\n" );
//        }

        // Expires
        boolean hasAmzDateHeader = request.getLastHeader( "x-amz-date" ) != null;
        stringBuffer.append( hasAmzDateHeader ? "" : expires );
        stringBuffer.append( "\n" );

        // CanonicalizedAmzHeaders
        for (Header header : request.getAllHeaders()) {
            String name = header.getName();
            if ( name.startsWith( "x-amz" ) ) {
                stringBuffer.append( name );
                stringBuffer.append( ":" );
                stringBuffer.append( header.getValue() );
                stringBuffer.append( "\n" );
            }
        }

        stringBuffer.append( resource ); // CanonicalizedResource

        String signature = stringBuffer.toString();
        byte[] bytes = signature.getBytes( "UTF-8" );
        bytes = hmacSHA1( accessKey, bytes );
        signature = Base64.encodeBase64String( bytes );
        return signature;
    }

    // http://docs.amazonwebservices.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/AuthJavaSampleHMACSignature.html
    private byte[] hmacSHA1( String key, byte[] data ) throws NoSuchAlgorithmException, InvalidKeyException {

        SecretKey secretKeySpec = new SecretKeySpec( key.getBytes(), "HmacSHA1" );
        Mac mac = Mac.getInstance( "HmacSHA1" );
        mac.init( secretKeySpec );
        mac.update( data );
        return mac.doFinal();
    }

    public static final class PutObjectResult {

        public File file;
        public Properties properties;
        public String bucketPath;
    }

    public static final class GetObjectResult {

        public File file;
        public Properties properties;
        public String bucketPath;
        public String originalPath;
    }

    public static final class ListBucketObject {

        public String key;
        public String etag;
        public long size;
        public long lastModified;
    }
}
