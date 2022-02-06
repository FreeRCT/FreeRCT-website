package freerct.freerct;

import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

@Controller
public class Contact {
	@GetMapping("/contact")
	@ResponseBody
	public String fetch(WebRequest request) {
		return FreeRCTApplication.generatePage(request, " Legal Notice / Contact", """
			<h1>Legal Notice</h1>
			<p>
				The website <a href="/">freerct.net</a> is a privately owned page to exchange knowledge and experience
				about the free open source game FreeRCT. There is no financial goal or interest.
			</p>

			<h1>Contact</h1>
			<p>
				For questions related to the game or the contents of the website, please post in the forums.
			</p><p>
				If you do not wish to create an account or to make your concern publicly visible,
				please send a message to the website's administrator,
				<a href="/user/Nordfriese">Benedikt Straub</a>, at
				<a href="mailto:benedikt-straub@web.de">benedikt-straub@web.de</a>.
			</p>
		""");
	}
}
