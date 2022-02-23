package freerct.freerct;

import javax.servlet.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

import static freerct.freerct.FreeRCTApplication.generatePage;
import static freerct.freerct.FreeRCTApplication.sql;
import static freerct.freerct.FreeRCTApplication.getCalendar;
import static freerct.freerct.FreeRCTApplication.htmlEscape;
import static freerct.freerct.FreeRCTApplication.renderMarkdown;
import static freerct.freerct.FreeRCTApplication.datetimestring;
import static freerct.freerct.FreeRCTApplication.shortDatetimestring;
import static freerct.freerct.FreeRCTApplication.createLinkifiedHeader;

/** The Contact and Legal Notice page. */
@Controller
public class Contact {
	@GetMapping("/contact")
	@ResponseBody
	public String fetch(WebRequest request, HttpSession session) {
		return generatePage(request, session, "Legal Notice / Contact / Privacy Policy", """
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

			<h1>Privacy Policy</h1>
			<p>
				When you register to this website, it stores your e-mail address and your self-chosen username.
				You can ask the website administrator (see above) to delete your account at any time.
			</p>
			<p>
				This website stores a cookie named <code>JSESSIONID</code> in your browser. It will be deleted when you close your browser.
				You can delete and/or disallow cookies in your browser, though logging in will not be possible then.
			</p>
			<p>
				The infrastructure and disk space for this website is provided by the web host <i>STRATO AG, Otto-Ostrowski-Straße 7, 10249 Berlin</i>.
			</p>
		""");
	}
}
