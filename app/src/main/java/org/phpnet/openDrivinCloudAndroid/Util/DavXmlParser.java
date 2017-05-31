package org.phpnet.openDrivinCloudAndroid.Util;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by clement on 21/04/17.
 */

public class DavXmlParser {
    private static final String TAG = "DavXmlParser";
    private static final String ns = "DAV:";
    public static final String METHOD_PROPFIND = "propfindMethod";

    public List parse(String method, InputStream in) throws IllegalArgumentException ,XmlPullParserException, IOException {
        try{
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(in, null);
            parser.nextTag();
            if(method == METHOD_PROPFIND){
                return readFeedPropFind(parser);
            }else{
                throw new IllegalArgumentException("method argument must be one of DavXmlParser.METHOD_PROPFIND");
            }
        }finally {
            in.close();
        }

    }

    private List readFeedPropFind(XmlPullParser parser) throws IOException, XmlPullParserException {
        List entries = new ArrayList();
        parser.require(XmlPullParser.START_TAG, ns, "multistatus");
        while(parser.next() != XmlPullParser.END_TAG){
            if(parser.getEventType() != XmlPullParser.START_TAG){
                continue;
            }
            String name = parser.getName();
            Log.d(TAG, "readFeedPropFind: found tag "+name);
        }
        return null;
    }


}
