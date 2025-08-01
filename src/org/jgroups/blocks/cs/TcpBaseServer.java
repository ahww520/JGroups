package org.jgroups.blocks.cs;

import org.jgroups.Address;
import org.jgroups.util.SocketFactory;
import org.jgroups.util.ThreadFactory;

/**
 * Common base class for TCP based clients and servers
 * @author Bela Ban
 * @since  3.6.5
 */
public abstract class TcpBaseServer extends BaseServer {
    protected int     peer_addr_read_timeout=2000; // max time in milliseconds to block on reading peer address
    protected int     buffered_inputstream_size=8192;
    protected int     buffered_outputstream_size=8192;
    protected boolean non_blocking_sends;       // https://issues.redhat.com/browse/JGRP-2759
    protected int     max_send_queue=1024;      // when non_blocking, how many messages to queue max?
    protected boolean use_lock_to_send=true;    // e.g. a single sender doesn't need to acquire the send_lock

    protected TcpBaseServer(ThreadFactory f, SocketFactory sf, int recv_buf_size) {
        super(f, sf, recv_buf_size);
    }

    @Override
    protected TcpConnection createConnection(Address dest) throws Exception {
        return non_blocking_sends? new TcpConnectionNonBlocking(dest, this, max_send_queue).useLockToSend(use_lock_to_send)
          : new TcpConnection(dest, this).useLockToSend(use_lock_to_send);
    }


    public int           peerAddressReadTimeout()           {return peer_addr_read_timeout;}
    public TcpBaseServer peerAddressReadTimeout(int t)      {this.peer_addr_read_timeout=t; return this;}
    public int           getBufferedInputStreamSize()       {return buffered_inputstream_size;}
    public TcpBaseServer setBufferedInputStreamSize(int s)  {this.buffered_inputstream_size=s; return this;}
    public int           getBufferedOutputStreamSize()      {return buffered_outputstream_size;}
    public TcpBaseServer setBufferedOutputStreamSize(int s) {this.buffered_outputstream_size=s; return this;}
    public boolean       nonBlockingSends()                 {return non_blocking_sends;}
    public TcpBaseServer nonBlockingSends(boolean b)        {this.non_blocking_sends=b; return this;}
    public int           maxSendQueue()                     {return max_send_queue;}
    public TcpBaseServer maxSendQueue(int s)                {this.max_send_queue=s; return this;}
    public boolean       useLockToSend()                    {return use_lock_to_send;}
    public TcpBaseServer useLockToSend(boolean u)           {this.use_lock_to_send=u; return this;}


}
