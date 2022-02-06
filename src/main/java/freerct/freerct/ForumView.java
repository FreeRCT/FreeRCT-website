package freerct.freerct;

import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

@Controller
public class ForumView {
	@GetMapping("/forum/")
	@ResponseBody
	public String fetch(WebRequest request) {
		return FreeRCTApplication.generatePage(request, "NOCOM", """
			<h1>NOCOM</h1>
		""");
	}
}
