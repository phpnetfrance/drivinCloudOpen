package org.phpnet.openDrivinCloudAndroid.Adapter;


import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import org.phpnet.openDrivinCloudAndroid.Activities.AcceuilActivity;
import org.phpnet.openDrivinCloudAndroid.Common.TypeFile;
import org.phpnet.openDrivinCloudAndroid.R;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * Created by germaine on 29/06/15.
 */
public class MyFile
    implements Serializable{
    private static final String TAG = "MyFile";

    public String name;
    public String date;
    public String size;
    public Uri pathURL;//Chemin complet dans le drive
    private Date dateObj;
    private double sizeDouble;
    public TypeFile typeFile;
    public String mimeType;

    private static final double KO = 1 << 10;
    private static final double MO = 1 << 20;
    private static final double GO = 1 << 30;
    private static final double TO = 1 << 40;

    private static final String FILE = AcceuilActivity.getContext().getResources().getString(R.string.file);
    private static final String DIR = AcceuilActivity.getContext().getResources().getString(R.string.directory);

    private static final char[] INTERDIT = {'²', '$', '\\', '#', '%', '(', ')', '^', '=', '+', '*', '/', '~', '?', '!', ':', ',', ';', '>', '<', '"'};
    private static final String[] EXTENTIONS_IMG = {"JPG", "jpg", "jpeg", "png", "tiff", "tif", "jig", "jfif", "jp2", "jpx", "j2k", "j2c", "fpx", "pcd"};
    private static final String[] EXTENTIONS_AUDIO = {"mp3", "3gp", "act", "aiff", "aac", "amr", "ape", "jig", "au", "awb", "dct", "dss", "dvf", "flac", "gsm",
            "iklax", "ivs", "m4a", "m4p", "mmf", "mpc", "msv", "ogg", "oga", "opus", "ra", "rm", "raw", "sln", "tta", "vox", "wav", "wma", "wv", "webm"};
    private static final String[] EXTENTIONS_TXT =
            {"as", "mx", "adb", "ads", "ada", "asm", "c", "cpp", "h", "clj", "edn", "cbl", "cbd", "cdb", "cdc", "cob", "coffee", "cfm", "cs", "css",
                    "d", "diff", "patch", "hs", "lhs", "as", "las", "html", "xhtml", "htm", "tpl", "shtml", "dhtml", "phtml", "ini", "inf", "reg", "url",
                    "java", "class", "js", "json", "jsp", "lisp", "lsp", "lua", "mak", "sql", "m", "pas", "inc", "pl", "php", "php3", "ps1",
                    "properties", "py", "pyc", "pyo", "pyw", "pyd", "rb", "rbw", "rs", "sass", "scala", "scm", "ss", "smd", "sh", "sql", "svg", "svgz",
                    "tcl", "tex", "txt", "vbe", "vbs", "wsf", "wsc", "v", "xml", "xslt", "yml", "doc", "docx", "htaccess", "htpasswd", "fi"};


    public static Comparator<MyFile> getNameComparator(final boolean desc){
        return new Comparator<MyFile>() {
            @Override
            public int compare(MyFile lhs, MyFile rhs) {
                if (lhs.getName() == null || rhs.getName() == null)
                    return 0;
                if(!desc) {
                    return lhs.getName().compareToIgnoreCase(rhs.getName());
                }
                else
                    return rhs.getName().compareToIgnoreCase(rhs.getName());
            }
        };
    }
    public static Comparator<MyFile> getSizeComparator(final boolean desc){
        return new Comparator<MyFile>() {
            @Override
            public int compare(MyFile lhs, MyFile rhs) {
                if(!desc)
                    return Double.compare(lhs.getSizeDouble(), rhs.getSizeDouble());
                else
                    return Double.compare(rhs.getSizeDouble(), lhs.getSizeDouble());
            }
        };
    }
    public static Comparator<MyFile> getDateComparator(final boolean desc){
        return new Comparator<MyFile>() {
            @Override
            public int compare(MyFile lhs, MyFile rhs) {
                if (lhs.getDateObj() == null || rhs.getDateObj() == null) {
                    return 0;
                }
                if(!desc) {
                    return lhs.getDateObj().compareTo(rhs.getDateObj());
                }
                else
                    return rhs.getDateObj().compareTo(lhs.getDateObj());
            }
        };
    }



    public MyFile(String name, String date, String size, Uri pathURL, String mimetype) {
        this.name = name;
        this.date = date;
        try {
            this.dateObj = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.ENGLISH).parse(date);
        } catch (ParseException e) {
            Log.e(TAG, "MyFile: Erreur, la date fournie n'est pas au format attendu (dd MMM yyyy HH:mm)",e);
        }
        this.pathURL = pathURL;
        this.mimeType = mimetype;

        this.updateType();

        if (typeFile != TypeFile.DIR) {
            this.sizeDouble = Double.parseDouble(size);
            this.size = getSizeString();
        }
    }

    public Date getDateObj() {
        return dateObj;
    }

    public void setDateObj(Date dateObj) {
        this.dateObj = dateObj;
    }

    public double getSizeDouble() {
        return sizeDouble;
    }

    public static double round(double x) {
        return (double) ((int) (x * 100 + .5)) / 100;
    }

    /*
    * Retourne la taille affiché l'acceuil
    * */
    private String getSizeString() {
        if (sizeDouble == 0) {
            return "0";
        } else if (0 < sizeDouble && sizeDouble < KO) {
            DecimalFormat df = new DecimalFormat("#");
            df.setMaximumFractionDigits(0); //Prevents decimals
            return df.format(sizeDouble) + "o";
        } else if (KO <= sizeDouble && sizeDouble < MO) {
            return round(sizeDouble / KO) + "Ko";
        } else if (MO <= sizeDouble && sizeDouble < GO) {
            return round(sizeDouble / MO) + "Mo";
        } else {
            return round(sizeDouble / GO) + "Go";
        }
    }

    /*
    * Retoune true si name est de type ext
    * false sinon
    * */
    private boolean isType(String[] ext) {
        for (int i = 0; i < ext.length; i++) {
            if (name.endsWith("." + ext[i])) {
                return true;
            }
        }
        return false;
    }

    /*
    * Met à jour le type du fichier
    * */
    public void updateType() {
        if (name.endsWith(".pdf")) {
            this.typeFile = TypeFile.PDF;
        } else if (name.endsWith("/")) {
            this.typeFile = TypeFile.DIR;
        } else if (isType(EXTENTIONS_TXT)) {
            this.typeFile = TypeFile.TXT;
        } else if (isType(EXTENTIONS_IMG)) {
            this.typeFile = TypeFile.IMG;
        } else if (isType(EXTENTIONS_AUDIO)) {
            this.typeFile = TypeFile.AUDIO;
        } else {
            //Refers to all other types
            this.typeFile = TypeFile.NOTYPE;
        }
    }

    public TypeFile getTypeFile() {
        return typeFile;
    }

    public String getType() {
        return typeFile == TypeFile.DIR ? DIR : FILE;
    }

    public String getMimeType(){
        return mimeType;
    }


    /*
    * Retourne true le caractère est interdit
    * false sinon
    * */
    private static boolean interdit(char c) {
        for (char cour : INTERDIT) {
            if (c == cour)
                return true;
        }
        return false;
    }

    /*
    * Retourne true si au moins un caractère interdit
    * false sinon
    * */
    public static boolean interdit(String newName) {
        for (int i = 0; i < newName.length(); ++i) {
            if (interdit(newName.charAt(i)))
                return true;
        }
        return false;
    }

    /*Same name same type*/
    public boolean same(MyFile file) {
        return this.name.equals(file.name) &&
                typeFile == file.typeFile;
    }

    @Override
    public String toString() {
        return "MyFile{" +
                "name='" + name + '\'' +
                ", date='" + date + '\'' +
                ", size='" + size + '\'' +
                ", pathURL='" + pathURL + '\'' +
                ", sizeDouble=" + sizeDouble +
                ", typeFile=" + typeFile +
                ", mimeType='" + mimeType + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MyFile myFile = (MyFile) o;

        if (typeFile != myFile.getTypeFile()) return false;
        if (!name.equals(myFile.name)) return false;
        if (!date.equals(myFile.date)) return false;
        if (!size.equals(myFile.size)) return false;
        return pathURL.equals(myFile.pathURL);

    }

    public boolean isDir() {
        return typeFile == TypeFile.DIR;
    }

    public String getName() {
        return name;
    }

    public Uri getUrl() {
        return pathURL;
    }

    /**
     * Checks if the image is lesser than 6Mo and otherwize if user activated preview of big images
     * @param context
     * @return
     */
    public boolean isPreviewable(Context context){
        if(sizeDouble<6291456) return true;
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_preview_big_images", false);
    }

    public int getIconResId() {
        if(this.isDir()){
            return R.drawable.dc_folder;
        }else if (name.endsWith(".xls") || name.endsWith(".xlsx")) {
            return R.drawable.dc_file;
        } else if (name.endsWith(".css")) {
            return R.drawable.dc_css;
        } else if (name.endsWith(".html")) {
            return R.drawable.dc_html;
        } else if (name.endsWith(".php") || name.endsWith(".php4")) {
            return R.drawable.dc_php;
        } else if (name.endsWith(".js")) {
            return R.drawable.dc_js;
        } else if (name.endsWith(".doc") || name.endsWith(".docx")) {
            return R.drawable.doc;
        } else if (name.endsWith(".ppt") || name.endsWith(".pptx")) {
            return R.drawable.dc_file;
        } else if (name.endsWith(".pdf")) {
            return R.drawable.dc_pdf;
        } else if (name.endsWith(".apk")) {
            return R.drawable.dc_binary;
        } else if (name.endsWith(".txt")) {
            return R.drawable.dc_file;
        } else if (name.endsWith(".JPG") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return R.drawable.dc_image;
        } else if (name.endsWith(".png")) {
            return R.drawable.dc_image;
        } else if (name.endsWith(".zip")) {
            return R.drawable.dc_zip;
        } else if (name.endsWith(".rtf")) {
            return R.drawable.dc_file;
        } else if (name.endsWith(".gif")) {
            return R.drawable.dc_image;
        } else if (name.endsWith(".ai")) {
            return R.drawable.dc_illustrator;
        } else if (name.endsWith(".ps") || name.endsWith(".psd")) {
            return R.drawable.dc_photoshop;
        } else if (name.endsWith(".mov") || name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") || name.endsWith(".wmv")) {
            return R.drawable.dc_video;
        } else if (this.getTypeFile() == TypeFile.AUDIO) {
            return R.drawable.dc_music;
        } else {
            return R.drawable.dc_binary;
        }
    }

    public int getUniqueId(){
        return pathURL.hashCode();
    }
}
