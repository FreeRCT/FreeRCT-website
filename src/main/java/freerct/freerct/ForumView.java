package freerct.freerct;

import java.sql.*;
import java.util.*;

import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

@Controller
public class ForumView {
	private static class Topic {
		public final long id, nrPosts, firstPostID, lastPostID;
		public final String name, creator, lastUpdater;
		public final Calendar created, lastUpdated;
		public Topic(long i, long nrP, long firstID, long lastID, String n, String auth, String lastUpd, Calendar timeFirst, Calendar timeLast) {
			id = i;
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
	public String fetch(WebRequest request, @PathVariable long forumID) {
		try {
			ResultSet sql = FreeRCTApplication.sql("select name,description from forums where id=?", forumID);
			sql.next();
			final String forumName = sql.getString("name");
			final String forumDescription = sql.getString("description");

			List<Topic> allTopics = new ArrayList<>();
			ResultSet topics = FreeRCTApplication.sql("select id,name from topics where forum=? order by id desc", forumID);
			int nrPosts = 0;
			while (topics.next()) {
				final long topicID = topics.getLong("id");

				ResultSet posts = FreeRCTApplication.sql("select count(id) as nr from posts where topic=?", topicID);
				posts.next();
				final long nrPostsInTopic = posts.getLong("nr");

				ResultSet firstPost = FreeRCTApplication.sql("select id,user,created from posts where id=(select min(id) from posts where topic=?)", topicID);
				if (!firstPost.next()) continue;  // Empty topic

				ResultSet lastPost = FreeRCTApplication.sql("select id,user, created from posts where id=(select max(id) as id from posts where topic=?)", topicID);
				lastPost.next();

				ResultSet firstAuthor = FreeRCTApplication.sql("select username from users where id=?", firstPost.getLong("user"));
				firstAuthor.next();
				ResultSet lastAuthor = FreeRCTApplication.sql("select username from users where id=?", firstPost.getLong("user"));
				lastAuthor.next();

				Calendar calendarFirst = FreeRCTApplication.getCalendar(firstPost, "created");
				Calendar calendarLast = FreeRCTApplication.getCalendar(lastPost, "created");

				allTopics.add(new Topic(topicID, nrPostsInTopic, firstPost.getLong("id"), lastPost.getLong("id"),
						topics.getString("name"), firstAuthor.getString("username"), lastAuthor.getString("username"), calendarFirst, calendarLast));
				nrPosts += nrPostsInTopic;
			}

			String body	=	"<h1>Forum: " + forumName + "</h1>"
						+	"<p class='forum_description_name'>" + forumDescription + "</p>"
						+	"<p class='forum_description_stats'>" + allTopics.size() + " topics · " + nrPosts + " posts</p>"
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
						+				FreeRCTApplication.datetimestring(t.created, request.getLocale())
						+			"</div>"
						+		"</div>"
						+		"<div class='forum_list_right_column'>"
						+			"<div><a href='/forum/post/" + t.lastPostID + "'>Posts: " + t.nrPosts + "</a></div>"
						+			"<div>Most recent post by <a href='/user/" + t.lastUpdater + "'>" + t.lastUpdater + "</a> on "
						+				FreeRCTApplication.datetimestring(t.lastUpdated, request.getLocale())
						+			"</div>"
						+		"</div>"
						+	"</div>";
			}

			return FreeRCTApplication.generatePage(request, "Forum | " + forumName, body);
		} catch (Exception e) {
			return new ErrorHandler().error(request);
		}
	}
}
