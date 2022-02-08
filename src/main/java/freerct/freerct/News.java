package freerct.freerct;

import java.sql.*;
import java.util.*;

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
import static freerct.freerct.FreeRCTApplication.DESIRED_PADDING_BELOW_MENU_BAR;

@Controller
public class News {
	private static long countNews() {
		try {
			ResultSet sql = sql("select count(id) as nr from news");
			sql.next();
			return sql.getLong("nr");
		} catch (Exception e) {
			return 0;
		}
	}

	public static String printLatestNews(Locale locale, long howMany, int margin) {
		try {
			final long nr = countNews();
			if (howMany < 0 || howMany > nr) howMany = nr;
			String body = "";

			ResultSet sql = sql("select id,author,timestamp,slug,title,body from news order by timestamp desc limit ?", howMany);
			while (sql.next()) {
				String slug = htmlEscape(sql.getString("slug"));

				ResultSet author = sql("select username from users where id=?", sql.getLong("author"));
				author.next();
				String authorName = htmlEscape(author.getString("username"));

				body	+=	"<p id='" + slug + "' style='padding-top:" + margin + "px'></p><div class='news'>"
					+		"<h3><a href='/news#" + slug + "' class='linkified_header'>"
					+			renderMarkdown(sql.getString("title"))
					+		"</a></h3>"
					+		renderMarkdown(sql.getString("body"))
					+		"<p class='news_timestamp'>"
					+			"<a href='/user/" + authorName + "'>" + authorName + "</a>"
					+			" ~ "
					+ 			datetimestring(getCalendar(sql, "timestamp"), locale)
					+		"</p>"
					+	"</div>";
			}

			return body;
		} catch (Exception e) {
			return "";
		}
	}

	@GetMapping("/news")
	@ResponseBody
	public String fetch(WebRequest request) {
		String body = "<h1>News Archive</h1>";
		body += "<h2 class='news_count'>";
		final long nr = countNews();
		if (nr == 1) {
			body += "1 news item";
		} else {
			body += nr + " news items";
		}
		body += "</h2>" + printLatestNews(request.getLocale(), -1, DESIRED_PADDING_BELOW_MENU_BAR);

		return generatePage(request, "News Archive", body);
	}
}
