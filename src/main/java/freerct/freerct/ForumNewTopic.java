package freerct.freerct;

import java.sql.*;
import java.util.*;

import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

import static freerct.freerct.FreeRCTApplication.generatePage;
import static freerct.freerct.FreeRCTApplication.sql;
import static freerct.freerct.FreeRCTApplication.sqlSync;
import static freerct.freerct.FreeRCTApplication.getCalendar;
import static freerct.freerct.FreeRCTApplication.htmlEscape;
import static freerct.freerct.FreeRCTApplication.renderMarkdown;
import static freerct.freerct.FreeRCTApplication.datetimestring;
import static freerct.freerct.FreeRCTApplication.shortDatetimestring;
import static freerct.freerct.FreeRCTApplication.pluralForm;
import static freerct.freerct.FreeRCTApplication.createLinkifiedHeader;
import static freerct.freerct.FreeRCTApplication.generateForumPostForm;

/** The form to create a new topic in a forum. */
@Controller
public class ForumNewTopic {
	@GetMapping("/forum/{forumID}/new")
	@ResponseBody
	public String fetch(WebRequest request, @PathVariable long forumID) {
		try {
			ResultSet sql = sql("select name,description from forums where id=?", forumID);
			sql.next();
			final String forumName = sql.getString("name");
			final String forumDescription = sql.getString("description");

			String body	=	"<h1>Forum: " + htmlEscape(forumName) + ": New Topic</h1>"
						+	"<p class='forum_description_name'>" + htmlEscape(forumDescription) + "</p>"
						+	generateForumPostForm(true, "Post", "/forum/" + forumID + "/submit_new");

			return generatePage(request, "Forum | " + forumName + " | New Topic", body);
		} catch (Exception e) {
			return new ErrorHandler().error(request);
		}
	}

	@PostMapping("/forum/{forumID}/submit_new")
	public String createTopic(WebRequest request,
			@PathVariable long forumID,
			@RequestPart("subject") String subject,
			@RequestPart("content") String content) {
		try {
			ResultSet sql = sql("select id from users where username=?", request.getRemoteUser());
			sql.next();
			long userID = sql.getLong("id");

			synchronized (sqlSync()) {
				sql("insert into topics (forum,name) value(?,?)", forumID, subject);
				sql = sql("select last_insert_id() as new_id");
			}
			sql.next();
			long topicID = sql.getLong("new_id");

			sql("insert into posts (topic,user,body) value(?,?,?)", topicID, userID, content);

			return "redirect:/forum/topic/" + topicID;
		} catch (Exception e) {
			return "redirect:/error";
		}
	}

	@PostMapping("/render_markdown")
	@ResponseBody
	public String getMarkdownPreview(@RequestBody String content) {
		return renderMarkdown(content);
	}
}
