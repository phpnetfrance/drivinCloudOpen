package org.phpnet.openDrivinCloudAndroid.Providers.Cursors;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.MatrixCursor;
import android.os.Build;
import android.provider.DocumentsContract;

import org.phpnet.openDrivinCloudAndroid.Common.User;
import org.phpnet.openDrivinCloudAndroid.R;

/**
 * Created by clement on 18/04/17.
 */

@TargetApi(Build.VERSION_CODES.KITKAT)
public class DrivinCloudRootCursor extends MatrixCursor {

    private static final String[] DEFAULT_ROOT_PROJECTION = new String[] {
            DocumentsContract.Root.COLUMN_ROOT_ID, DocumentsContract.Root.COLUMN_FLAGS, DocumentsContract.Root.COLUMN_ICON, DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID, DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_FLAGS
    };

    public DrivinCloudRootCursor(String[] columnNames) {
        super(columnNames != null ? columnNames : DEFAULT_ROOT_PROJECTION);
    }

    /**
     * Adds a root to the provider. Each root corresponds to an account
     * /!\ Only account with saved password will work here
     * @param account
     * @param context
     */
    public void addRoot(User account, Context context) throws PasswordNeededException {
        if(!account.isPasswordSaved()){
            throw new PasswordNeededException("Account "+account.getUsername()+" has no registered password. A Password is needed to use an account for a storage provider.");
        }
        newRow().add(DocumentsContract.Root.COLUMN_ROOT_ID, account.hashCode())
                .add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, account.getUsername())
                .add(DocumentsContract.Root.COLUMN_SUMMARY, account.getUsername())
                .add(DocumentsContract.Root.COLUMN_TITLE, "drivinCloud")
                .add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.back_logo);
    }

    public class PasswordNeededException extends Exception {
        public PasswordNeededException(String s) {
            super(s);
        }
    }
}
