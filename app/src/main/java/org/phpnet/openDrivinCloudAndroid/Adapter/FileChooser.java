package org.phpnet.openDrivinCloudAndroid.Adapter;

import org.phpnet.openDrivinCloudAndroid.Common.Months;

import java.io.File;
import java.text.SimpleDateFormat;

/**
 * Created by germaine on 03/07/15.
 */
public class FileChooser {

    public String fileName;
    public boolean isDirectory;
    public String info;
    public String path;
    private File src;

    public FileChooser(File file) {
        this.src = file;
        this.fileName = file.getName();
        this.isDirectory = file.isDirectory();
        this.info = getDate(file.lastModified()) + " , " + getSize(file.length());
        this.path = file.getAbsolutePath() + "/";
    }

    public File getSrc() {
        return src;
    }

    private String getDate(long nbSec) {
        String month = new SimpleDateFormat("MM").format(nbSec);
        String day = new SimpleDateFormat("dd").format(nbSec);
        String year = new SimpleDateFormat("yyyy").format(nbSec);
        String time = new SimpleDateFormat("HH:mm").format(nbSec);
        String date = day + "-" + Months.values()[Integer.parseInt(month) - 1] + "-" + year + " " + time;
        return date;
    }

    private String getSize(long nbOctel) {
        if (nbOctel < 1000) {
            return String.valueOf(nbOctel);
        } else if (1000 <= nbOctel && nbOctel <= 1000000 - 1) {
            return String.valueOf(nbOctel / 1000) + "K";
        } else if (1000000 <= nbOctel && nbOctel <= 1000000000 - 1) {
            return String.valueOf(nbOctel / 1000000) + "M";
        } else {
            return String.valueOf(nbOctel / 1000000000) + "G";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileChooser that = (FileChooser) o;

        if (isDirectory != that.isDirectory) return false;
        if (fileName != null ? !fileName.equals(that.fileName) : that.fileName != null)
            return false;
        if (path != null ? !path.equals(that.path) : that.path != null)
            return false;
        return !(info != null ? !info.equals(that.info) : that.info != null);

    }

    @Override
    public String toString() {
        return "FileChooser{" +
                "fileName='" + fileName + '\'' +
                ", isDirectory=" + isDirectory +
                ", info='" + info + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
