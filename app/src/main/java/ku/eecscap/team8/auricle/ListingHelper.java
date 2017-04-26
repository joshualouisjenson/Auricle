package ku.eecscap.team8.auricle;

/**
 * Created by Austin Kurtti on 4/23/2017.
 * Last Edited by Austin Kurtti on 4/25/2017
 */

public class ListingHelper {
    private String filename, length, dateCreated;
    private int id;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String inFilename) {
        filename = inFilename;
    }

    public int getId() {
        return id;
    }

    public void setId(int inId) {
        id = inId;
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
