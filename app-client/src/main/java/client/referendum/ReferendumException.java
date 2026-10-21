package client.referendum;

public class ReferendumException extends Exception {

    private final ReferendumError error;

    public ReferendumException(ReferendumError error) {
        super(error.name());
        this.error = error;
    }

    public ReferendumException(ReferendumError error, Throwable cause) {
        super(error.name(), cause);
        this.error = error;
    }

    public ReferendumError getError() {
        return error;
    }
}