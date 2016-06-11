package server.resource;

import util.Constants;
import util.ContentBroker;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class SimplePostResource extends ServerResource {

	@Post
	public void update(Representation entity) {
		String sd = "";
		try {
			sd = URLDecoder.decode(entity.getText(), "utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.out.println("Rest server entity: " + sd);
		ContentBroker cb = new ContentBroker(sd);
		/**
		 * Do something
		 */
		String content = cb.get(Constants.CONTENT);
		String answer = content != null ? content + "; Post received"
				: "no content :(";
		formatAnswer(answer, Status.SUCCESS_OK);
	}

	protected void error(String message) {
		formatAnswer((String) message, Status.CLIENT_ERROR_BAD_REQUEST);
	}
	protected void formatAnswer(String content, Status status) {
		StringRepresentation answerEntity = new StringRepresentation("{\" Your initial content \" : \"" + content + "\" }",
				MediaType.TEXT_PLAIN);
//		System.out.println("--> resource received: " + content);
		getResponse().setEntity(answerEntity);
		getResponse().setStatus(status);
	}
}
