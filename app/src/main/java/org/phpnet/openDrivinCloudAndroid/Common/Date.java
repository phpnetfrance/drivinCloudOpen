package org.phpnet.openDrivinCloudAndroid.Common;

/**
 * Created by germaine on 20/07/15.
 */
public class Date {


    private int hour;
    private int minute;
    private int year;
    private int month;
    private int day;

    //"15-Jul-2015 16:36  "
    public Date(String date) {
        this.day = Integer.parseInt(date.substring(0, 2));
        this.month = getMonth(date.substring(3, 6));
        this.year = Integer.parseInt(date.substring(7, 11));
        this.hour = Integer.parseInt(date.substring(12, 14));
        this.minute = Integer.parseInt(date.substring(15, 17));
    }

    private Integer getMonth(String month) {
        Months[] months = Months.values();
        Months argMonth = Months.valueOf(month);
        for (int i = 0; i < months.length; ++i) {
            if (argMonth == months[i])
                return (i + 1);
        }
        return null;
    }

    public int compareInt(int thisInt, int argInt) {
        if (thisInt == argInt) {
            return 0;
        } else if (thisInt < argInt) {
            return -1;
        } else {
            return 1;
        }
    }


    /*Return 1 if arg < this
    * Return -1 if arg > this
    * Return 0 if arg == this
    * */
    public int compareTo(Date date) {
        if (date == this)
            return 0;

        if (compareInt(this.year, date.year) != 0)
            return compareInt(this.year, date.year);

        if (compareInt(this.month, date.month) != 0)
            return compareInt(this.month, date.month);

        if (compareInt(this.day, date.day) != 0)
            return compareInt(this.day, date.day);

        if (compareInt(this.hour, date.hour) != 0)
            return compareInt(this.hour, date.hour);

        return compareInt(this.minute, date.minute);
    }

}
