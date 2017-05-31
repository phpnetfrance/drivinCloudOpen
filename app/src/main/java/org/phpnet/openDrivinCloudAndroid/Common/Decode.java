package org.phpnet.openDrivinCloudAndroid.Common;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.io.IOUtils;
import org.apache.webdav.lib.methods.PropFindMethod;
import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;
import org.phpnet.openDrivinCloudAndroid.Listener.ClickLogout;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by germaine on 03/07/15.
 */
public class Decode {
    private static final String TAG = "Decode";
    private Enumeration davResponse;

    private static Decode instance = new Decode();

    public static Decode getInstance() {
        return instance;
    }

    private Decode() {
    }

    /**
     * Convert a stream to a string
     * @param stream The stream to convert to string
     * @param encoding The encoding, leave it to null to use UTF-8
     * @return The stream converted as string
     * @throws IOException
     */
    public String convertStreamToString(InputStream stream, @Nullable String encoding) throws IOException {
        if (stream == null)
            return "";
        StringWriter sw = new StringWriter();
        if(encoding == null) encoding = "UTF-8";
        IOUtils.copy(stream, sw, encoding);
        return sw.toString();
    }

    public ArrayList<MyFile> getListFile(final Uri url) {
        CurrentUser user = CurrentUser.getInstance();
        List<MyFile> listFile = new LinkedList<>();
        final boolean[] connectionSuccess = new boolean[1];
        try {
            if(user.username.equals("")) ClickLogout.getInstance().onClickLogout(true);
            String urlStr = URLDecoder.decode(url.toString(), "UTF-8");
            if(!urlStr.substring(urlStr.length()-1).equals("/")){
                urlStr+="/";
            }
            Log.d(TAG, "getListFile: PROPFIND "+urlStr);
            davResponse = user.wdr.propfindMethod(urlStr, PropFindMethod.DEPTH_1);
            String name, date, size, mimetype;
            while (davResponse.hasMoreElements()) {
                String node = davResponse.nextElement().toString();
                Document doc = getDomElement(node);

                name = doc.getElementsByTagName("D:href").item(0).getTextContent();
                name = URLDecoder.decode(name, "UTF-8");
                NodeList el = doc.getElementsByTagName("D:getcontenttype");
                if(el.getLength() > 0){
                    mimetype = el.item(0).getTextContent();
                }else{
                    //Si webdav ne nous renvoie pas de type, on le définit tel que suit (données binaires arbitraire
                    //@see RFC 2046 section 4.5.1
                    mimetype = "application/octet-stream";
                }

                //Exclude current directory
                if (name.equals("/") || name.equals(urlStr.toString().substring(user.serverURL.toString().length(), urlStr.toString().length()))) {
                    continue;
                }

                name = name.substring(name.substring(0, name.length() - 1).lastIndexOf("/") + 1, name.length());
                date = doc.getElementsByTagName("lp1:getlastmodified").item(0).getTextContent();
                date = date.substring(5, date.length() - 7);
                size = (name.endsWith("/")) ? "0" : doc.getElementsByTagName("lp1:getcontentlength").item(0).getTextContent();
                listFile.add(new MyFile(name, date, size, url, mimetype));
            }
            Log.d(TAG, "getListFile: Retrieved "+listFile.size()+" elements in folder "+url);
        } catch(HttpException e1) {
            Log.e(TAG, "getListFile: Got PROPFIND Response ["+user.wdr.getStatusCode()+"] "+user.wdr.getStatusMessage(), e1);
        } catch (Exception e1) {
            e1.printStackTrace();
            return null;
        } finally {
            //Petite optimisation
            ArrayList<MyFile> listFileArray = new ArrayList<>();
            listFileArray.addAll(listFile);

            return listFileArray;
        }


    }


    private Document getDomElement(String xml) {
        Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {

            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml));
            doc = db.parse(is);

        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
            return null;
        }
        // return DOM
        return doc;
    }
}
