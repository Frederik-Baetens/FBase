package tasks;

import org.apache.log4j.Logger;

import control.FBase;
import exceptions.FBaseStorageConnectorException;
import model.config.KeygroupConfig;
import model.config.NodeConfig;
import model.config.ReplicaNodeConfig;
import tasks.TaskManager.TaskName;

class UpdateKeygroupConfigTask extends Task<Boolean> {

	private static Logger logger = Logger.getLogger(UpdateKeygroupConfigTask.class.getName());
	
	private KeygroupConfig config = null;

	public UpdateKeygroupConfigTask(KeygroupConfig config, TaskManager taskmanager) {
		super(TaskName.UpdateKeygroupConfig, taskmanager);
		this.config = config;
	}
	
	@Override
	public Boolean executeFunctionality() {
		
		// store config in database
		try {
			FBase.connector.createKeygroup(config.getKeygroupID());
		} catch (FBaseStorageConnectorException e) {
			// no problem, it just already existed
		}
		try {
			FBase.connector.putKeygroupConfig(config.getKeygroupID(), config);
		} catch (FBaseStorageConnectorException e) {
			logger.fatal("Could not store keygroup configuration in node DB, nothing changed");
			return false;
		}

		if (config.getReplicaNodes() != null) {
			logger.debug("Subscribing to replica nodes of config " + config.getKeygroupID());
			for (ReplicaNodeConfig rnConfig: config.getReplicaNodes()) {
				logger.debug("Subscribing to machines of node " + rnConfig.getNodeID());
				// get node configs
				NodeConfig nodeConfig = null;
				try {
					nodeConfig = FBase.connector.getNodeConfig(rnConfig.getNodeID());
					// subscribe to all machines 
					// TODO I: we currently don't load balance the subscriptions, no failover (it is just done by the machine that runs this task)
					// TODO I: if this task is used more than once for the same keygroup config by different machines, they all subscribe to all publishers
					int publisherPort = nodeConfig.getPublisherPort();
					for (String machine: nodeConfig.getMachines()) {
						FBase.subscriptionRegistry.subscribeTo(machine, publisherPort, config.getEncryptionSecret(), 
								config.getEncryptionAlgorithm(), config.getKeygroupID());
					}
				} catch (FBaseStorageConnectorException e) {
					logger.error("Could not get node configuration from node DB for " + rnConfig.getNodeID());
				}
						
			}
		} else {
			logger.debug("No replica nodes exist config " + config.getKeygroupID());
		}
		
		return true;
		
	}

	

}
