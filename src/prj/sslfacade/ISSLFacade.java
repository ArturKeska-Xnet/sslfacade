package prj.sslfacade;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface ISSLFacade
{
    void setHandshakeCompletedListener(IHandshakeCompletedListener hcl);

    void setSSLListener(ISSLListener l);

    void beginHandshake() throws IOException;

    boolean isHandshakeCompleted();

    void encrypt(ByteBuffer plainData) throws SSLException;

    void decrypt(ByteBuffer encryptedData) throws IOException;

    void close();

    boolean isCloseCompleted();

    void terminate();
}
