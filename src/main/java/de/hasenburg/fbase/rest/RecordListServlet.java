package de.hasenburg.fbase.rest;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import control.FBase;
import crypto.CryptoProvider;
import exceptions.FBaseRestException;
import exceptions.FBaseStorageConnectorException;
import model.JSONable;
import model.config.KeygroupConfig;
import model.data.KeygroupID;
import model.messages.Message;

/**
 * 
 * @author jonathanhasenburg
 *
 */
public class RecordListServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(RecordListServlet.class.getName());
	private FBase fBase = null;

	public RecordListServlet(FBase fBase) {
		this.fBase = fBase;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		logger.debug("Received list request with query string " + req.getQueryString());

		PrintWriter w = resp.getWriter();
		KeygroupID keygroupID = KeygroupID.createFromString((req.getParameter("keygroupID")));
		Message m = new Message();

		try {
			if (keygroupID == null) {
				// 400 Bad Request
				throw new FBaseRestException(FBaseRestException.KEYGROUPID_MISSING, 400);
			}

			KeygroupConfig config = null;
			String IDJson = null;
			try {
				config = fBase.configAccessHelper.keygroupConfig_get(keygroupID).getValue0();
				if (config == null) {
					// 404 Not Found
					throw new FBaseRestException(FBaseRestException.NOT_FOUND, 404);
				}

				// create a set of dataidentifier strings
				Set<String> IDs = fBase.connector.dataRecords_list(keygroupID).stream()
						.map(id -> id.toString()).collect(Collectors.toSet());

				ObjectMapper mapper = new ObjectMapper();
				IDJson = mapper.writeValueAsString(IDs); // if this fails, error 500 will be thrown
			} catch (FBaseStorageConnectorException e) {
				// 404 Not Found
				throw new FBaseRestException(FBaseRestException.NOT_FOUND, 404);
			}

			// 200 OK
			resp.setStatus(200);
			m.setTextualInfo("Success");
			m.setContent(CryptoProvider.encrypt(IDJson, config.getEncryptionSecret(),
					config.getEncryptionAlgorithm()));
			m.setContent(IDJson); // Remove to encrypt
			w.write(JSONable.toJSON(m));
		} catch (FBaseRestException e) {
			logger.error(e.getMessage());
			resp.sendError(e.getHttpErrorCode(), e.getMessage());
		} catch (Exception e) {
			// 500 Internal Server Error
			resp.sendError(500, FBaseRestException.SERVER_ERROR);
			e.printStackTrace();
		}

	}

}
