package org.phpnet.openDrivinCloudAndroid.Common;

import org.phpnet.openDrivinCloudAndroid.Adapter.MyFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by clement on 3/22/17.
 */

public class ListFile extends ArrayList<MyFile> {
    /**
     * Sorts the file list by date
     * @param descending set to true to sort descending
     */
    public void sortDate(boolean descending) throws SortException {
        Map<String, ArrayList<MyFile>> separated = getFilesFoldersSeparated();
        if(!separated.containsKey("files") || !separated.containsKey("folders")){
            throw new SortException();
        }

        Collections.sort((ArrayList<MyFile>) separated.get("files"), MyFile.getDateComparator(descending));
        Collections.sort((ArrayList<MyFile>) separated.get("folders"), MyFile.getDateComparator(descending));
        this.clear();
        this.addAll(separated.get("folders"));
        this.addAll(separated.get("files"));
    }

    /**
     * Sorts the file list by name
     * @param descending set to true to sort descending
     */
    public void sortName(boolean descending) throws SortException {
        Map<String, ArrayList<MyFile>> separated = getFilesFoldersSeparated();
        if(!separated.containsKey("files") || !separated.containsKey("folders")){
            throw new SortException();
        }

        Collections.sort((ArrayList<MyFile>) separated.get("files"), MyFile.getNameComparator(descending));
        Collections.sort((ArrayList<MyFile>) separated.get("folders"), MyFile.getNameComparator(descending));
        this.clear();
        this.addAll(separated.get("folders"));
        this.addAll(separated.get("files"));
    }

    /**
     * Sorts the file list by size
     * @param descending set to true to sort descending
     */
    public void sortSize(boolean descending) throws SortException {
        Map<String, ArrayList<MyFile>> separated = getFilesFoldersSeparated();
        if(!separated.containsKey("files") || !separated.containsKey("folders")){
            throw new SortException();
        }

        Collections.sort((ArrayList<MyFile>) separated.get("files"), MyFile.getSizeComparator(descending));
        Collections.sort((ArrayList<MyFile>) separated.get("folders"), MyFile.getSizeComparator(descending));
        this.clear();
        this.addAll(separated.get("folders"));
        this.addAll(separated.get("files"));
    }

    public ArrayList<MyFile> getFiles(){
        ArrayList<MyFile> filesList = new ArrayList<>();
        for (MyFile file : this) {
            if(!file.isDir()){
                filesList.add(file);
            }
        }
        return filesList;
    }

    public ArrayList<MyFile> getFolders(){
        ArrayList<MyFile> foldersList = new ArrayList<>();
        for (MyFile file : this
                ) {
            if(file.isDir()){
                foldersList.add(file);
            }
        }
        return foldersList;
    }

    public Map<String, ArrayList<MyFile>> getFilesFoldersSeparated(){
        ArrayList<MyFile> filesList = new ArrayList<>();
        ArrayList<MyFile> foldersList = new ArrayList<>();
        for (MyFile file : this
             ) {
            if(file.isDir()){
                foldersList.add(file);
            }else{
                filesList.add(file);
            }
        }
        Map filesFoldersListsMap = new HashMap();
        filesFoldersListsMap.put("files", filesList);
        filesFoldersListsMap.put("folders", foldersList);
        return filesFoldersListsMap;
    }

    public class SortException extends Exception{}
}
