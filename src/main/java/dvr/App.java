package dvr;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class App extends Application {
	
	private DVRService service;
	
	 @Override
     public Set<Class<?>> getClasses()
     {
        HashSet<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(RecordService.class);
        return classes;
     }
	 
	 public void setService(DVRService service) {
        this.service = service;
    }
	 
	 public DVRService getService() {
        return service;
    }

}
