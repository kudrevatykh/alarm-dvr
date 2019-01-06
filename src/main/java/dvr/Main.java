package dvr;

import javax.ws.rs.core.Application;

import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;

import dvr.impl.DVRServiceImpl;
import io.undertow.Undertow;

public class Main {

	public static void main(String[] args) {
		UndertowJaxrsServer server = new UndertowJaxrsServer();
        Undertow.Builder serverBuilder = Undertow.builder().addHttpListener(8080, "0.0.0.0");
        server.start(serverBuilder);
        try {
            server.deploy(createApp());
        } catch (Exception e) {
        	server.stop();
        	throw e;
        }   
	}
	
	private static Application createApp() {
		App app = new App();
		app.setService(new DVRServiceImpl());
		return app;
	}

}
