package de.hasenburg.fbase.rest.jersey;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import control.FBase;
import exceptions.FBaseCommunicationException;
import exceptions.FBaseStorageConnectorException;
import model.JSONable;
import model.config.NodeConfig;

/**
 * The only supported node method is get. The client should not be able to run any other
 * operations.
 * 
 * @author jonathanhasenburg
 *
 */
@Path("nodes")
public class NodeResource {

	private static Logger logger = Logger.getLogger(NodeResource.class.getName());

	@Inject
	FBase fBase;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNodeConfig() {

		NodeConfig config = null;
		try {
			config = fBase.configAccessHelper.nodeConfig_get(fBase.configuration.getNodeID());

			if (config == null) {
				return Response.status(404, "Config does not exist").build();
			}
		} catch (FBaseStorageConnectorException | FBaseCommunicationException e) {
			logger.warn(e);
			return Response.status(500, e.getMessage()).build();
		}

		return Response.ok(JSONable.toJSON(config)).build();
	}

}