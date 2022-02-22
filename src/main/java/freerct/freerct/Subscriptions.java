package freerct.freerct;

import java.sql.*;
import java.util.*;

import javax.servlet.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

import static freerct.freerct.FreeRCTApplication.generatePage;
import static freerct.freerct.FreeRCTApplication.sql;
import static freerct.freerct.FreeRCTApplication.sendEMail;
import static freerct.freerct.FreeRCTApplication.getCalendar;
import static freerct.freerct.FreeRCTApplication.htmlEscape;
import static freerct.freerct.FreeRCTApplication.renderMarkdown;
import static freerct.freerct.FreeRCTApplication.datetimestring;
import static freerct.freerct.FreeRCTApplication.shortDatetimestring;
import static freerct.freerct.FreeRCTApplication.createLinkifiedHeader;

/** The pages to manage e-mail notification subscriptions. */
@Controller
public class Subscriptions {
	public static void sendNewPostMails(long postID) {
		try {
			ResultSet sql = sql("select id,default_enable from noticetypes where slug='forum_new_post'");
			sql.next();
			final long noticetypeID = sql.getLong("id");
			final boolean sendDefault = sql.getInt("default_enable") > 0;

			sql = sql("select topic,user,body from posts where id=?", postID);
			sql.next();
			final long topicID = sql.getLong("topic");
			final long postAuthorID = sql.getLong("user");
			final String postBody = htmlEscape(sql.getString("body"));

			sql = sql("select name from topics where id=?", topicID);
			sql.next();
			final String topicName = htmlEscape(sql.getString("name"));

			sql = sql("select username from users where id=?", postAuthorID);
			sql.next();
			final String authorName = htmlEscape(sql.getString("username"));

			sql = sql("select distinct user from subscriptions where topic=?", topicID);
			while (sql.next()) {
				final long userID = sql.getLong("user");
				if (userID == postAuthorID) continue;

				ResultSet userDetails = sql("select state from notification_settings where user=? and notice=?", userID, noticetypeID);
				if (userDetails.next() ? userDetails.getInt("state") > 0 : sendDefault) {
					userDetails = sql("select email from users where id=?", userID);
					userDetails.next();
					sendEMail(userDetails.getString("email"), "Forum New Post",
							"A new post was added to the topic \"" + topicName + "\" by " + authorName + ":\n\n"
							+ postBody
			    			+ "\n\n-------------------------\n"
			    			+ "Link to post: https://freerct.net/forum/post/" + postID
						, true);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@GetMapping("/forum/topic/subscribe/{topicID}")
	public String subscribeToTopic(WebRequest request, HttpSession session, @PathVariable long topicID) {
		try {
			ResultSet sql = sql("select id from users where username=?", request.getRemoteUser());
			sql.next();
			sql("insert into subscriptions (user,topic) value (?,?)", sql.getLong("id"), topicID);

			return "redirect:/forum/topic/" + topicID;
		} catch (Exception e) {
			return "redirect:/error?reason=internal_server_error";
		}
	}

	@GetMapping("/forum/topic/unsubscribe/{topicID}")
	public String unsubscribeFromTopic(WebRequest request, HttpSession session, @PathVariable long topicID) {
		try {
			ResultSet sql = sql("select id from users where username=?", request.getRemoteUser());
			sql.next();
			sql("delete from subscriptions where user=? and topic=?", sql.getLong("id"), topicID);

			return "redirect:/forum/topic/" + topicID;
		} catch (Exception e) {
			return "redirect:/error?reason=internal_server_error";
		}
	}
}
