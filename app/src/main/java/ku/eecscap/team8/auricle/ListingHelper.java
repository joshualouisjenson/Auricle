package ku.eecscap.team8.auricle;

/**
 * Created by Austin Kurtti on 4/23/2017.
 * Last Edited by Austin Kurtti on 4/26/2017
 */

public class ListingHelper {
    private int id;
    private String filename, format, length, dateCreated;

    public int getId() {
        return id;
    }

    public void setId(int inId) {
        id = inId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String inFilename) {
        filename = inFilename;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String inFormat) {
        format = inFormat;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String inLength) {
        length = inLength;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String inDateCreated) {
        dateCreated = inDateCreated;
    }
}
