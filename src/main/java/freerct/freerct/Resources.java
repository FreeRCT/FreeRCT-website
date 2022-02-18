package freerct.freerct;

import java.io.*;

import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.*;

/** Resolves dynamic resource locations. */
@Configuration
@EnableWebMvc
public class Resources extends WebMvcConfigurerAdapter {
	public static final File RESOURCES_DIR = new File("resources");

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry
			.addResourceHandler("/**")
			.addResourceLocations("file:" + RESOURCES_DIR.getAbsolutePath() + "/")
			;
	}
}
