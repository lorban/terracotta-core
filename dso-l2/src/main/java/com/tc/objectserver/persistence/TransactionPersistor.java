package com.tc.objectserver.persistence;

import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.Serializer;

import com.tc.net.ClientID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.SortedSet;

/**
 * @author tim
 */
public class TransactionPersistor {
  private final KeyValueStorage<GlobalTransactionID, GlobalTransactionDescriptor> committed;

  public TransactionPersistor(KeyValueStorage<GlobalTransactionID, GlobalTransactionDescriptor> committed) {
    this.committed = committed;
  }

  public static KeyValueStorageConfig<GlobalTransactionID, GlobalTransactionDescriptor> config() {
    return new ImmutableKeyValueStorageConfig<GlobalTransactionID, GlobalTransactionDescriptor>(GlobalTransactionID.class, GlobalTransactionDescriptor.class, TransactionPersistor.GlobalTransactionIDSerializer.INSTANCE, TransactionPersistor.GlobalTransactionDescriptorSerializer.INSTANCE);
  }

  public Collection<GlobalTransactionDescriptor> loadAllGlobalTransactionDescriptors() {
    return committed.values();
  }

  public void saveGlobalTransactionDescriptor(Transaction tx, GlobalTransactionDescriptor gtx) {
    committed.put(gtx.getGlobalTransactionID(), gtx);
  }

  public void deleteAllGlobalTransactionDescriptors(Transaction tx, SortedSet<GlobalTransactionID> globalTransactionIDs) {
    committed.removeAll(globalTransactionIDs);
  }

  private static class GlobalTransactionIDSerializer extends AbstractIdentifierTransformer<GlobalTransactionID> {
    static final GlobalTransactionIDSerializer INSTANCE = new GlobalTransactionIDSerializer();

    GlobalTransactionIDSerializer() {
      super(GlobalTransactionID.class);
    }

    @Override
    protected GlobalTransactionID createIdentifier(final long id) {
      return new GlobalTransactionID(id);
    }
  }

  private static class GlobalTransactionDescriptorSerializer extends Serializer<GlobalTransactionDescriptor> {
    static final GlobalTransactionDescriptorSerializer INSTANCE = new GlobalTransactionDescriptorSerializer();

    @Override
    public GlobalTransactionDescriptor recover(final ByteBuffer buffer) {
      GlobalTransactionID gid = new GlobalTransactionID(buffer.getLong());
      ServerTransactionID sid = new ServerTransactionID(new ClientID(buffer.getLong()), new TransactionID(buffer.getLong()));
      return new GlobalTransactionDescriptor(sid, gid);
    }

    @Override
    public ByteBuffer transform(final GlobalTransactionDescriptor globalTransactionDescriptor) {
      ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE * 3);
      buffer.putLong(globalTransactionDescriptor.getGlobalTransactionID().toLong());
      buffer.putLong(((ClientID) globalTransactionDescriptor.getServerTransactionID().getSourceID()).toLong());
      buffer.putLong(globalTransactionDescriptor.getClientTransactionID().toLong());
      buffer.flip();
      return buffer;
    }

    @Override
    public boolean equals(final GlobalTransactionDescriptor left, final ByteBuffer right) {
      return left.equals(recover(right));
    }
  }
}