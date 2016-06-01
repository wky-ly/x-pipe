package com.ctrip.xpipe.redis.keeper;

import java.io.IOException;

/**
 * @author wenchao.meng
 *
 * May 31, 2016
 */
public interface ReplicationStoreManager {
	
	/**
	 * create new replication store
	 * @return
	 * @throws IOException 
	 */
	ReplicationStore create() throws IOException;
	
	/**
	 * get the newest replication store
	 * @return
	 * @throws IOException 
	 */
	ReplicationStore getCurrent() throws IOException;
	
	
	void destroy(ReplicationStore replicationStore);
	

	String getClusterName();
	
	String getShardName();
}
