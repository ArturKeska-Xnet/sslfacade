package prj.sslfacade;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;

public class SSLFacade implements ISSLFacade
{

    private Handshaker _handshaker;
    private IHandshakeCompletedListener _hcl;
    private final Worker _worker;
    private boolean _clientMode;

    public SSLFacade(SSLContext context, boolean client,
                     boolean clientAuthRequired, ITaskHandler taskHandler)
    {
        //Currently there is no support for SSL session reuse,
        // so no need to take a peerHost or port from the host application
        SSLEngine engine = makeSSLEngine(context, client, clientAuthRequired);
        Buffers buffers = new Buffers(engine.getSession());
        _worker = new Worker(engine, buffers);
        _handshaker = new Handshaker(_worker, taskHandler);
        _clientMode = client;
    }

    @Override
    public boolean isClientMode() {
      return _clientMode;
    }
    
    @Override
    public void setHandshakeCompletedListener(IHandshakeCompletedListener hcl)
    {
        _hcl = hcl;
    }

    @Override
    public void setSSLListener(ISSLListener l)
    {
        _worker.setSSLListener(l);
    }

    @Override
    public void setCloseListener(ISessionClosedListener l)
    {
        _handshaker.setSessionClosedListener(l);
    }

    @Override
    public void beginHandshake() throws SSLException
    {
        attachCompletionListener();
        _handshaker.begin();
    }

    @Override
    public boolean isHandshakeCompleted()
    {
        return (_handshaker == null) || _handshaker.isFinished();
    }

    @Override
    public void encrypt(ByteBuffer plainData) throws SSLException
    {
        _worker.wrap(plainData);
    }

    @Override
    public void decrypt(ByteBuffer encryptedData) throws SSLException
    {
        _worker.unwrap(encryptedData);
        if (!isHandshakeCompleted())
        {
            _handshaker.carryOn();
        }
    }

    @Override
    public void close()
    {
    /* Called if we want to properly close SSL */
        _worker.close(true);
    }

    @Override
    public boolean isCloseCompleted()
    {
    /* Host application should only close underlying transport after
     close_notify packet generated by wrap has been sent to peer. Use this
     method to check if the packet has been generated
     */
        return _worker.isCloseCompleted();
    }

    @Override
    public void terminate()
    {
    /* Called if peer closed connection unexpectedly */
        _worker.close(false);
    }

    /* Privates */
    private void attachCompletionListener()
    {
        _handshaker.addCompletedListener(new IHandshakeCompletedListener()
        {
            @Override
            public void onComplete()
            {
                //_handshaker = null;
                if (_hcl != null)
                {
                    _hcl.onComplete();
                    _hcl = null;
                }
            }
        });
    }

    private SSLEngine makeSSLEngine(SSLContext context, boolean client, boolean clientAuthRequired)
    {
        SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(client);
        engine.setNeedClientAuth(clientAuthRequired);
        return engine;
    }
}