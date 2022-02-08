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
import static freerct.freerct.FreeRCTApplication.pluralForm;
import static freerct.freerct.FreeRCTApplication.createLinkifiedHeader;

/** The page with the list of all forums. */
@Controller
public class ForumList {
	private static class Forum {
		public final long id;
		public final String name, description;
		public long nrTopics, nrPosts;
		public Forum(long i, String n, String d) {
			id = i;
			name = n;
			description = d;
			nrTopics = 0;
			nrPosts = 0;
		}
	}

	@GetMapping("/forum")
	@ResponseBody
	public String fetch(WebRequest request) {
		List<Forum> allForums = new ArrayList<>();
		try {
			ResultSet forums = sql("select id,name,description from forums order by id asc");
			long nrTotalTopics = 0;
			long nrTotalPosts = 0;
			while (forums.next()) {
				Forum f = new Forum(forums.getLong("id"), forums.getString("name"), forums.getString("description"));
				ResultSet topics = sql("select id,name from topics where forum=?", f.id);
				while (topics.next()) {
					++f.nrTopics;
					ResultSet posts = sql("select count(id) as nr from posts where topic=?", topics.getLong("id"));
					posts.next();
					f.nrPosts += posts.getLong("nr");
				}

				nrTotalTopics += f.nrTopics;
				nrTotalPosts += f.nrPosts;
				allForums.add(f);
			}

			String body	=	"<h1>Forums</h1>"
						+	"<p class='forum_description_stats'>"
						+		pluralForm(allForums.size(), "forum", "forums")
						+		" · " + pluralForm(nrTotalTopics, "topic", "topics")
						+		" · " + pluralForm(nrTotalPosts, "post", "posts")
						+	"</p>"
						;

			for (Forum f : allForums) {
				body	+=	"<a class='forum_list_entry' href='/forum/" + f.id + "'>"
						+		"<div>"
						+			"<div class='forum_list_header'>" + renderMarkdown(f.name, true) + "</div>"
						+			"<div>" + renderMarkdown(f.description, true) + "</div>"
						+		"</div>"
						+		"<div class='forum_list_right_column'>"
						+			"<div>Topics: " + f.nrTopics + "</div>"
						+			"<div>Posts: " + f.nrPosts + "</div>"
						+		"</div>"
						+	"</a>";
			}

			return generatePage(request, "Forums", body);
		} catch (SQLException e) {
			return new ErrorHandler().error(request);
		}
	}
}
