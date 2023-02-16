package yooga.app.dto;

public class RequestNcmDTO {

    private String ncmInvalid;
    private String ncmValid;
    private Integer idi;

    public String getNcmInvalid() {
        return ncmInvalid;
    }
    public void setNcmInvalid(String ncmInvalid) {
        this.ncmInvalid = ncmInvalid;
    }
    public String getNcmValid() {
        return ncmValid;
    }
    public void setNcmValid(String ncmValid) {
        this.ncmValid = ncmValid;
    }
    public Integer getIdi() {
        return idi;
    }
    public void setIdi(Integer idi) {
        this.idi = idi;
    }
}
