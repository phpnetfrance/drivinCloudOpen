package org.phpnet.openDrivinCloudAndroid.Common;

import android.content.Context;
import android.util.Log;

import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;
import org.phpnet.openDrivinCloudAndroid.Model.Account;
import org.phpnet.openDrivinCloudAndroid.Util.DavXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.URL;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

/**
 * Created by clement on 19/04/17.
 */

public class DavProvider {
    private static final String TAG = "DavProvider";
    private final Account user;
    private OkHttpClient httpClient;
    public DavProvider(final Account user, final Context context){
        this.user = user;
        httpClient = new OkHttpClient.Builder()
                .authenticator(new Authenticator() {
                    @Override
                    public Request authenticate(Route route, Response response) throws IOException {
                        Log.d(TAG, "okHttpAuthenticate: auth for response "+response);
                        String creds = Credentials.basic(user.getUsername(), user.getPassword(context));
                        return response.request().newBuilder()
                                .header("Authorization", creds)
                                .build();
                    }
                })
                .build();
    }

    public ListFile getItems(MyFile parent) throws IOException {
        //TODO
        return null;
    }

    public ListFile getItems(String path) throws IOException {
        if(!path.startsWith("/")) path = "/"+path;
        URL url = new URL((user.getUseSSL() ? "https://" : "http://") + user.getHostname() + path);

        Request req = new Request.Builder()
                .url(url)
                .header("Depth", "1")
                .method("PROPFIND", RequestBody.create(MediaType.parse("text/xml"), "<D:propfind xmlns:D='DAV:'><D:allprop/></D:propfind>"))
                .build();



        Response res = httpClient.newCall(req).execute();
        if(!res.isSuccessful()) throw new IOException("Unexpected response "+res+" from server.");

        Log.d(TAG, "getItems response: "+res);
        DavXmlParser xmlParser = new DavXmlParser();
        try {
            xmlParser.parse(DavXmlParser.METHOD_PROPFIND, res.body().byteStream());
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

        return null;
    }
}
