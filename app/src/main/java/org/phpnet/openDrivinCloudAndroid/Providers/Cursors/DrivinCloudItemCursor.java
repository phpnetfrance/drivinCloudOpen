package org.phpnet.openDrivinCloudAndroid.Providers.Cursors;

import android.annotation.TargetApi;
import android.database.MatrixCursor;
import android.os.Build;
import android.provider.DocumentsContract;

import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;

/**
 * Created by clement on 18/04/17.
 */

@TargetApi(Build.VERSION_CODES.KITKAT)
public class DrivinCloudItemCursor extends MatrixCursor {

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[] {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_FLAGS, DocumentsContract.Document.COLUMN_LAST_MODIFIED
    };

    public DrivinCloudItemCursor(String[] columnNames) {
        super(columnNames != null ? columnNames : DEFAULT_DOCUMENT_PROJECTION);
    }

    /**
     * Adds a root to the provider. Each root corresponds to an account
     * /!\ Only account with saved password will work here
     */
    public void addItem(MyFile item) throws PasswordNeededException {

        newRow().add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, item.getUniqueId())
                .add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, item.getName())
                .add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, item.getDateObj().getTime())
                .add(DocumentsContract.Document.COLUMN_SIZE, item.getSizeDouble())
                .add(DocumentsContract.Document.COLUMN_ICON, item.getIconResId())
                .add(DocumentsContract.Document.COLUMN_MIME_TYPE, item.getMimeType());
    }

    public class PasswordNeededException extends Exception {
        public PasswordNeededException(String s) {
            super(s);
        }
    }
}
