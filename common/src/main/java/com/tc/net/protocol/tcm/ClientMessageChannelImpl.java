/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.protocol.tcm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.util.ProductID;
import com.tc.net.ClientID;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.ClientConnectionErrorListener;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.JvmIDUtil;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportInitiator;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionProvider;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class ClientMessageChannelImpl extends AbstractMessageChannel implements ClientMessageChannel {
  private static final Logger logger = LoggerFactory.getLogger(ClientMessageChannel.class);

  private int                             connectAttemptCount;
  private int                             connectCount;
  private final ProductID                 productID;
  private final SessionProvider           sessionProvider;
  private MessageTransportInitiator       initiator;
  private volatile SessionID              channelSessionID = SessionID.NULL_ID;
  private final List<ClientConnectionErrorListener> errorListeners = new CopyOnWriteArrayList<>();

  protected ClientMessageChannelImpl(TCMessageFactory msgFactory, TCMessageRouter router,
                                     SessionProvider sessionProvider, ProductID productId) {
    super(router, logger, msgFactory);
    this.productID = productId;
    this.sessionProvider = sessionProvider;
    this.sessionProvider.initProvider();
  }

  @Override
  public void setMessageTransportInitiator(MessageTransportInitiator initiator) {
    this.initiator = initiator;
  }

  @Override
  public void reset() {
    ChannelStatus status = getStatus();
    status.reset();
    this.sendLayer.reset();
  }

  @Override
  public NetworkStackID open(Collection<ConnectionInfo> info) throws TCTimeoutException, UnknownHostException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    final ChannelStatus status = getStatus();

    synchronized (status) {
      if (status.isOpen()) { throw new IllegalStateException("Channel already open"); }
      // initialize the connection ID, using the local JVM ID
      final ConnectionID cid = new ConnectionID(JvmIDUtil.getJvmID(), (((ClientID) getLocalNodeID()).toLong()), productID);

      final NetworkStackID id = this.initiator.openMessageTransport(info, cid);

      this.channelSessionID = this.sessionProvider.getSessionID();
      channelOpened();
      return id;
    }
  }

  @Override
  public NodeID getRemoteNodeID() {
    return ServerID.NULL_ID;
  }

  @Override
  public ProductID getProductID() {
    return getProductID(productID);
  }

  @Override
  public ChannelID getChannelID() {
    final ChannelStatus status = getStatus();
    synchronized (status) {
      return new ChannelID(this.sendLayer.getConnectionID().getChannelID());
    }
  }

  @Override
  public int getConnectCount() {
    return this.connectCount;
  }

  @Override
  public int getConnectAttemptCount() {
    return this.connectAttemptCount;
  }

  /*
   * Session message filter. To drop old session msgs when session changed.
   */
  @Override
  public void send(TCNetworkMessage message) throws IOException {
// used to do session filtering here.  This wreaks havoc on upper layers to silently drop
// messages on the floor.  send everything through
    super.send(message);
  }

  @Override
  public void notifyTransportConnected(MessageTransport transport) {
    if (!transport.getConnectionID().isNull()) {
      long channelIdLong = transport.getConnectionID().getChannelID();
      setLocalNodeID(new ClientID(channelIdLong));
      this.connectCount++;
    }
    super.notifyTransportConnected(transport);
  }

  @Override
  public void notifyTransportDisconnected(MessageTransport transport, boolean forcedDisconnect) {
    this.channelSessionID = this.sessionProvider.nextSessionID();
    logger.debug("ClientMessageChannel moves to " + this.channelSessionID + " for remote node " + getRemoteNodeID());
    super.notifyTransportDisconnected(transport, forcedDisconnect);
  }

  @Override
  public void notifyTransportConnectAttempt(MessageTransport transport) {
    super.notifyTransportConnectAttempt(transport);
    this.connectAttemptCount++;
  }

  @Override
  public void notifyTransportClosed(MessageTransport transport) {
    super.notifyTransportClosed(transport);
  }

  @Override
  public ClientID getClientID() {
    return (ClientID) getLocalNodeID();
  }

  @Override
  public void addClientConnectionErrorListener(ClientConnectionErrorListener errorListener) {
    errorListeners.add(errorListener);
  }

  @Override
  public void removeClientConnectionErrorListener(ClientConnectionErrorListener errorListener) {
    errorListeners.remove(errorListener);
  }

  @Override
  public void onError(ConnectionInfo connInfo, Exception e) {
    errorListeners.forEach(l->l.onError(connInfo, e));
  }
}
