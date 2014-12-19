package cis542.eyeung.fnest;

/**
 * Created by adam on 12/18/2014.
 */
public class fNestServer {
    private String ip;
    private String port;

    public fNestServer() {
        this.ip = "10.0.0.10";
        this.port = "3933";
    }

    public fNestServer(fNestServer x) {
        String temp = x.toString();
        this.ip = temp.split(":")[0];
        this.port = temp.split(":")[1];
    }

    public fNestServer(String ip, String port) {
        this.ip = ip;
        this.port = port;
    }

    public String toString(){
        return (this.ip + ":" + this.port);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof fNestServer)) {
            return false;
        }
        fNestServer t = (fNestServer) o;
        return t.toString().equals(this.toString());
    }
}
