package freerct.freerct;

import java.util.*;

import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

@Controller
public class News {
	private static class Message {
		public final Calendar timestamp;
		public final String slug, title, body, author;
		private Message(Calendar c, String a, String u, String t, String b) {
			timestamp = c;
			slug = u;
			author = a;
			title = t;
			body = b;
		}

		public String print(Locale locale, int margin) {
			return	"<p id='" + slug + "' style='padding-top:" + margin + "px'></p><div class='news'>"
				+		"<h3><a href='/news#" + slug + "' class='linkified_header'>"
				+			title
				+		"</a></h3>"
				+		body
				+		"<p class='news_timestamp'>"
				+			"<a href='/user/" + author + "'>" + author + "</a>"
				+			" ~ "
				+ 			FreeRCTApplication.datetimestring(timestamp, locale)
				+		"</p>"
				+	"</div>";
		}

		public static final Message[] ALL_NEWS = new Message[] {
			new Message(new Calendar.Builder().setDate(2021, 8, 14).setTimeOfDay(22, 00, 00).build(), "Nordfriese",
					"freerct_0_1_coming_soon", "FreeRCT 0.1 Coming Soon", """
						<p>
							The FreeRCT project is making great progress.
							Expect the release of the <strong>first version 0.1</strong> sometime soon!
						</p>
			"""),
			new Message(new Calendar.Builder().setDate(2021, 8, 14).setTimeOfDay(10, 00, 00).build(), "Nordfriese",
					"new_freerct_homepage", "New FreeRCT Homepage", """
						<p>
							Development on the new FreeRCT website has started today.
							You're looking at the result right now.
						</p><p>
							The site is still heavily under development, please be patient until we're done.
						</p>
			"""),
		};
	}

	public static String printLatestNews(Locale locale, int howMany, int margin) {
		if (howMany < 0 || howMany > Message.ALL_NEWS.length) howMany = Message.ALL_NEWS.length;
		String str = "";
		for (int i = 0; i < howMany; ++i) str += Message.ALL_NEWS[i].print(locale, margin);
		return str;
	}

	@GetMapping("/news")
	@ResponseBody
	public String fetch(WebRequest request) {
		String body = "<h1>News Archive</h1>";
		body += "<h2 class='news_count'>";
		if (Message.ALL_NEWS.length == 1) {
			body += "1 news item";
		} else {
			body += Message.ALL_NEWS.length + " news items";
		}
		body += "</h2>" + printLatestNews(request.getLocale(), -1, FreeRCTApplication.DESIRED_PADDING_BELOW_MENU_BAR);

		return FreeRCTApplication.generatePage(request, "News Archive", body);
	}
}
