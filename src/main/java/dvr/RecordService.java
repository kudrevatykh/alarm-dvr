package dvr;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Path("/record")
public class RecordService {
	
	private DVRService service;
	
	private static Logger LOGGER = LoggerFactory.getLogger(RecordService.class);

	@Context
	public void setApp(Application app) {
		this.setService(((App) app).getService());
	}
	
	public void setService(DVRService service) {
        this.service = service;
    }


	@PUT
	@Consumes(MediaType.TEXT_PLAIN)
	public Response startRecoding(
	        String text
			) throws IOException {
		LOGGER.info("processing {}", text);
		Matcher m = Pattern.compile(".*№\\D*(\\d+).*", Pattern.DOTALL).matcher(text);
		if(!m.matches()) {
		    return Response.status(Status.BAD_REQUEST).build();
		}
		String channel = m.group(1);
		LOGGER.info("channel matched {}", channel);
		service.startRecording(channel);
		return Response.accepted().build();
	}
		
}