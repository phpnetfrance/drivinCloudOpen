package org.phpnet.openDrivinCloudAndroid.Providers;

import android.database.Cursor;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsProvider;
import android.support.annotation.RequiresApi;

import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;
import org.phpnet.openDrivinCloudAndroid.Common.Settings;
import org.phpnet.openDrivinCloudAndroid.Common.User;
import org.phpnet.openDrivinCloudAndroid.Providers.Cursors.DrivinCloudItemCursor;
import org.phpnet.openDrivinCloudAndroid.Providers.Cursors.DrivinCloudRootCursor;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by clement on 18/04/17.
 */


@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class DrivinCloudOpenDocumentProvider extends DocumentsProvider {
    private static final String TAG = "DrivinCloudDocumentProv";
    /**
     * Lets store the users here as content providers
     */
    private static Map<String, User> providerList;
    private static Map<Long, String> documentIdToProvider;

    /**
     * Here we list the current drivinCloud acconts with saved password
     * No network call for the moment
     * @param projection
     * @return
     * @throws FileNotFoundException
     */
    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        final DrivinCloudRootCursor res = new DrivinCloudRootCursor(projection);
        //Init cache
        if(providerList == null){
            providerList = new HashMap<String, User>();
        }

        for (Settings.User user: Settings.getUsers(getContext(), null)) {
            try {
                if(user.isPasswordSaved()) {
                    //User commonUser = new User(user.getHost(), user.getUsername(), user.getPassword(), user.getHost());
                    User commonUser = null;
                    providerList.put(commonUser.getUID(), commonUser);
                    res.addRoot(commonUser, getContext());
                }
            } catch (DrivinCloudRootCursor.PasswordNeededException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        final DrivinCloudItemCursor res = new DrivinCloudItemCursor(projection);
        final MyFile parentFolder = getFileForDocumentId(parentDocumentId);
        User user = getRootForDocumentId(parentDocumentId);
        //for(MyFile file : ){
            //TODO
        //}

        //if(parentFolder == null)

        return null;
    }

    private MyFile getFileForDocumentId(String parentDocumentId) throws FileNotFoundException {
        if(parentDocumentId.contains(":")){
            String[] splitedDocId = parentDocumentId.split(":");
            String rootName = splitedDocId[0];
            String filePath = splitedDocId[1];
            if(filePath == "") filePath = "/";
            User user = getRootForDocumentId(parentDocumentId);
            if(user == null) throw new FileNotFoundException("User "+rootName+" not found");

        }else{
            throw new FileNotFoundException("Invalid document id : "+parentDocumentId);
        }
        return null;
    }

    private String getDocumentIdForFile(User root, MyFile file){
        return root.getUID()+":"+file.getUrl();
    }

    private User getRootForDocumentId(String documentId){
        // Try to retrieve root from cache
        User usr = providerList.get(documentIdToProvider.get(documentId));

        if(usr == null && documentId.contains(":")){
            String[] splitedDocId = documentId.split(":");
            usr = getRoot(splitedDocId[0]);
        }
        return usr;
    }

    private User getRoot(String rootName){
        return providerList.get(rootName);
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        return null;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
        return null;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

}
