package freerct.freerct;

import java.sql.*;
import java.util.*;

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
import static freerct.freerct.FreeRCTApplication.pluralForm;
import static freerct.freerct.FreeRCTApplication.createLinkifiedHeader;

/** The page with the list of all topics in a forum. */
@Controller
public class ForumView {
	private static class Topic {
		public final long id, nrPosts, firstPostID, lastPostID, views;
		public final String name, creator, lastUpdater;
		public final Calendar created, lastUpdated;
		public Topic(long i, long nrP, long firstID, long lastID, String n, String auth, String lastUpd, Calendar timeFirst, Calendar timeLast, long v) {
			id = i;
			views = v;
			nrPosts = nrP;
			firstPostID = firstID;
			lastPostID = lastID;
			name = n;
			creator = auth;
			lastUpdater = lastUpd;
			created = timeFirst;
			lastUpdated = timeLast;
		}
	}

	@GetMapping("/forum/{forumID}")
	@ResponseBody
	public String fetch(WebRequest request, HttpSession session, @PathVariable long forumID) {
		try {
			ResultSet sql = sql("select name,description from forums where id=?", forumID);
			sql.next();
			final String forumName = sql.getString("name");
			final String forumDescription = sql.getString("description");

			List<Topic> allTopics = new ArrayList<>();
			ResultSet topics = sql("select id,name,views from topics where forum=? order by id desc", forumID);
			int nrPosts = 0;
			while (topics.next()) {
				final long topicID = topics.getLong("id");

				ResultSet posts = sql("select count(id) as nr from posts where topic=?", topicID);
				posts.next();
				final long nrPostsInTopic = posts.getLong("nr");

				ResultSet firstPost = sql("select id,user,created from posts where id=(select min(id) from posts where topic=?)", topicID);
				if (!firstPost.next()) continue;  // Empty topic

				ResultSet lastPost = sql("select id,user, created from posts where id=(select max(id) as id from posts where topic=?)", topicID);
				lastPost.next();

				ResultSet firstAuthor = sql("select username from users where id=?", firstPost.getLong("user"));
				firstAuthor.next();
				ResultSet lastAuthor = sql("select username from users where id=?", firstPost.getLong("user"));
				lastAuthor.next();

				Calendar calendarFirst = getCalendar(firstPost, "created");
				Calendar calendarLast = getCalendar(lastPost, "created");

				allTopics.add(new Topic(topicID, nrPostsInTopic, firstPost.getLong("id"), lastPost.getLong("id"),
						htmlEscape(topics.getString("name")), htmlEscape(firstAuthor.getString("username")),
						htmlEscape(lastAuthor.getString("username")), calendarFirst, calendarLast, topics.getLong("views")));
				nrPosts += nrPostsInTopic;
			}

			String body	=	"<h1>Forum: " + htmlEscape(forumName) + "</h1>"
						+	"<p class='forum_description_name'>" + htmlEscape(forumDescription) + "</p>"
						+	"<p class='forum_description_stats'>"
						+		pluralForm(allTopics.size(), "topic", "topics")
						+		" · " + pluralForm(nrPosts, "post", "posts")
						+	"</p>"

						+	"<form><div class='forum_back_button_wrapper'>"
						+		"<input class='form_button' type='submit' value='Back' formaction='/forum'>"
						+	"</div></form>"
						+	"<form><div class='forum_new_topic_button_wrapper'>"
						+		"<input class='form_button' type='submit' value='New Topic' formaction='/forum/" + forumID + "/new'>"
						+	"</div></form>"
						;

			Topic[] topicsSorted = allTopics.toArray(new Topic[0]);
			Arrays.sort(topicsSorted, (x, y) -> {
				int i = y.lastUpdated.compareTo(x.lastUpdated);
				if (i != 0) return i;
				if (x.lastPostID != y.lastPostID) return x.lastPostID > y.lastPostID ? -1 : 1;
				if (x.id != y.id) return x.id > y.id ? -1 : 1;
				return 0;
			});

			for (Topic t : topicsSorted) {
				body	+=	"<div class='forum_list_entry'>"
						+		"<div>"
						+			"<div class='forum_list_header'><a href='/forum/topic/" + t.id + "'>" + t.name + "</a></div>"
						+			"<div>Created by <a href='/user/" + t.creator + "'>" + t.creator + "</a> on "
						+				datetimestring(t.created, request.getLocale())
						+			"</div>"
						+		"</div>"
						+		"<div class='forum_list_right_column'>"
						+			"<div><a href='/forum/post/" + t.lastPostID + "'>Posts: " + t.nrPosts + "</a></div>"
						+			"<div>Views: " + t.views + "</div>"
//						+			"<div>Most recent post by <a href='/user/" + t.lastUpdater + "'>" + t.lastUpdater + "</a> on "
//						+				datetimestring(t.lastUpdated, request.getLocale())
//						+			"</div>"
						+		"</div>"
						+	"</div>";
			}

			return generatePage(request, session, "Forum | " + forumName, body);
		} catch (Exception e) {
			return new ErrorHandler().error(request, session, "internal_server_error");
		}
	}
}
