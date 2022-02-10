package freerct.freerct;

import java.sql.*;
import java.util.*;

import javax.servlet.http.*;
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

/** The form to delete a forum post. */
@Controller
public class ForumDeletePost {
	@GetMapping("/forum/post/delete/{postID}")
	@ResponseBody
	public String deletePost(WebRequest request, @PathVariable long postID, @RequestParam(value="error", required=false) String error) {
		try {
			ResultSet sql = sql("select topic,body from posts where id=?", postID);
			sql.next();
			final long topicID = sql.getLong("topic");
			final String content = sql.getString("body");

			sql = sql("select forum,name from topics where id=?", topicID);
			sql.next();
			final long forumID = sql.getLong("forum");
			final String topicName = sql.getString("name");

			sql = sql("select name from forums where id=?", forumID);
			sql.next();
			final String forumName = sql.getString("name");

			String body	=	"<h1>Topic: " + htmlEscape(topicName) + ": Delete Post</h1>"
						+	"<p class='forum_description_name'>Forum: <a href='/forum/" + forumID + "'>" + htmlEscape(forumName) + "</a></p>"

						+	"<form><div class='forum_back_button_wrapper'>"
						+		"<input class='form_button' type='submit' value='Back' formaction='/forum/post/" + postID + "'>"
						+	"</div></form>"

						+	generateForumPostForm(true, null, "Delete Post", content, "/forum/post/submit_delete/" + postID, error, true);
						;

			return generatePage(request, "Forum | " + forumName + " | " + topicName + " | Delete Post", body);
		} catch (Exception e) {
			return new ErrorHandler().error(request, "internal_server_error");
		}
	}

	@PostMapping("/forum/post/submit_delete/{postID}")
	public String deletePost(WebRequest request, HttpSession session, @PathVariable long postID) {
		try {
			if (!SecurityManager.mayDeletePost(request, postID)) return "redirect:/error?reason=forbidden";

			ResultSet sql = sql("select topic from posts where id=?", postID);
			sql.next();
			final long topicID = sql.getLong("topic");

			sql("delete from posts where id=?", postID);

			sql = sql("select count(id) as nr from posts where topic=?", topicID);
			sql.next();

			if (sql.getLong("nr") > 0) {
				return "redirect:/forum/topic/" + topicID;
			}

			sql = sql("select forum from topics where id=?", topicID);
			sql.next();
			final long forumID = sql.getLong("forum");

			sql("delete from topics where id=?", topicID);

			return "redirect:/forum/" + forumID;
		} catch (Exception e) {
			return "redirect:/error?reason=internal_server_error";
		}
	}
}
