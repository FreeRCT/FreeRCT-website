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
		public final String name, creator;
		public final Calendar created;
		public Topic(long i, long p, long f, long l, String n, String c, Calendar t) {
			id = i;
			nrPosts = p;
			firstPostID = f;
			lastPostID = l;
			name = n;
			creator = c;
			created = t;
		}
	}

	@GetMapping("/forum/{forumID}")
	@ResponseBody
	public String fetch(WebRequest request, @PathVariable long forumID) {
		try {
			ResultSet sql = FreeRCTApplication.sql("select name,description from forums where id=?", forumID);
			if (!sql.next()) return new ErrorHandler().error(request);
			final String forumName = sql.getString("name");
			final String forumDescription = sql.getString("description");

			List<Topic> allTopics = new ArrayList<>();
			ResultSet topics = FreeRCTApplication.sql("select id,name from topics where forum=?", forumID);
			int nrPosts = 0;
			while (topics.next()) {
				final long topicID = topics.getLong("id");

				ResultSet posts = FreeRCTApplication.sql("select count(id) as nr from posts where topic=?", topicID);
				posts.next();
				final long nrPostsInTopic = posts.getLong("nr");

				ResultSet firstPost = FreeRCTApplication.sql("select id,user,created from posts where id=(select min(id) from posts where topic=?)", topicID);
				if (!firstPost.next()) continue;  // Empty topic

				Calendar calendar = Calendar.getInstance();
				calendar.setTime(firstPost.getTimestamp("created"));

				ResultSet author = FreeRCTApplication.sql("select username from users where id=?", firstPost.getLong("user"));
				author.next();

				ResultSet lastPost = FreeRCTApplication.sql("select max(id) as id from posts where topic=?", topicID);
				lastPost.next();

				allTopics.add(new Topic(topicID, nrPostsInTopic, firstPost.getLong("id"), lastPost.getLong("id"),
						topics.getString("name"), author.getString("username"), calendar));
				nrPosts += nrPostsInTopic;
			}

			String body	=	"<h1>Forum: " + forumName + "</h1>"
						+	"<p class='forum_description_name'>" + forumDescription + "</p>"
						+	"<p class='forum_description_stats'>" + allTopics.size() + " topics · " + nrPosts + " posts</p>"
						;

			for (Topic t : allTopics) {
				body	+=	"<div class='forum_list_entry'>"
						+		"<div>"
						+			"<div class='forum_list_header'><a href='/forum/topic/" + t.id + "'>" + t.name + "</a></div>"
						+			"<div>Created by <a href='/user/" + t.creator + "'>" + t.creator + "</a> on "
						+				FreeRCTApplication.datetimestring(t.created, request.getLocale())
						+			"</div>"
						+		"</div>"
						+		"<div class='forum_list_right_column'>"
						+			"<div><a href='/post/" + t.lastPostID + "'>Posts: " + t.nrPosts + "</a></div>"
						+		"</div>"
						+	"</div>";
			}

			return FreeRCTApplication.generatePage(request, "Forum | " + forumName, body);
		} catch (Exception e) {
			return new ErrorHandler().error(request);
		}
	}
}
