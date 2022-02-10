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

/** The form to edit a forum post. */
@Controller
public class ForumEditPost {
	@GetMapping("/forum/post/edit/{postID}")
	@ResponseBody
	public String editPost(WebRequest request, @PathVariable long postID, @RequestParam(value="error", required=false) String error) {
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

			String body	=	"<h1>Topic: " + htmlEscape(topicName) + ": Edit Post</h1>"
						+	"<p class='forum_description_name'>Forum: <a href='/forum/" + forumID + "'>" + htmlEscape(forumName) + "</a></p>"

						+	"<form><div class='forum_back_button_wrapper'>"
						+		"<input class='form_button' type='submit' value='Back' formaction='/forum/post/" + postID + "'>"
						+	"</div></form>"

						+	generateForumPostForm(true, null, "Edit Post", content, "/forum/post/submit_edit/" + postID, error, false);
						;

			return generatePage(request, "Forum | " + forumName + " | " + topicName + " | Edit Post", body);
		} catch (Exception e) {
			return new ErrorHandler().error(request, "internal_server_error");
		}
	}

	@PostMapping("/forum/post/submit_edit/{postID}")
	public String editPost(WebRequest request, HttpSession session, @PathVariable long postID, @RequestPart("content") String content) {
		try {
			session.setAttribute("freerct-edit-post-content", content);

			if (!SecurityManager.mayEditPost(request, postID)) return "redirect:/forum/post/edit/" + postID + "?error=edit_restricted#post_form";

			content = content.trim();
			if (content.isEmpty()) return "redirect:/forum/post/edit/" + postID + "?error=empty_post#post_form";

			ResultSet sql = sql("select id from users where username=?", request.getRemoteUser());
			sql.next();

			sql("update posts set editor=?, edited=current_timestamp, body=? where id=?", sql.getLong("id"), content, postID);

			session.removeAttribute("freerct-edit-post-content");
			return "redirect:/forum/post/" + postID;
		} catch (Exception e) {
			return "redirect:/error?reason=internal_server_error";
		}
	}
}
