package freerct.freerct;

import java.io.*;
import java.security.*;
import java.sql.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.dao.*;
import org.springframework.security.config.annotation.authentication.builders.*;
import org.springframework.security.config.annotation.web.builders.*;
import org.springframework.security.config.annotation.web.configuration.*;
import org.springframework.security.core.*;
import org.springframework.security.core.session.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.factory.*;
import org.springframework.security.crypto.password.*;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.authentication.logout.*;
import org.springframework.security.web.session.*;
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

/** Handles user logins and general security measures. */
@Configuration
@EnableWebSecurity
public class SecurityManager extends WebSecurityConfigurerAdapter {
	/* These constants are stored in the database, DO NOT CHANGE THEM. */
	public static final int USER_STATE_NORMAL      = 0;  ///< State constant for normal users.
	public static final int USER_STATE_ADMIN       = 1;  ///< State constant for administrators.
	public static final int USER_STATE_MODERATOR   = 2;  ///< State constant for moderators.
	public static final int USER_STATE_DEACTIVATED = 3;  ///< State constant for deactivated accounts.
	public static final int USER_STATE_AWAITING    = 4;  ///< State constant for not yet activated accounts.

	/**
	 * Create an encoder with which to encrypt user passwords.
	 * @return Encoder to use.
	 */
	public static PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}

	/**
	 * Generate a unique random alphanumeric string sequence.
	 * @return Unique token.
	 * @throws Exception when no unique string can be generated.
	 */
	public static String generateRandomToken() throws Exception {
		byte[] bytes = new byte[20];
		for (int attempt = 0; attempt < 10; ++attempt) {
			SecureRandom.getInstanceStrong().nextBytes(bytes);
			String str = "";
			for (byte b : bytes) str += String.format("%02x", b).toUpperCase();

			ResultSet sql = sql("select count(id) as nr from users where activation_token=?", str);
			sql.next();
			if (sql.getLong("nr") != 0) continue;  // Token is not unique

			return str;
		}

		throw new Exception("Could not generate a unique token after ten attempts");
	}


	/**
	 * Check whether the current user may edit a forum post.
	 * @param request Web request associated with the current session.
	 * @param postID Post to edit.
	 * @return The user may edit this post.
	 */
	public static boolean mayEditPost(WebRequest request, long postID) {
		if (request.getUserPrincipal() == null) return false;  // Not logged in.
		try {
			ResultSet sql = sql("select id,state from users where username=?", request.getRemoteUser());
			sql.next();
			long userID = sql.getLong("id");
			switch (sql.getInt("state")) {
				case USER_STATE_ADMIN:
				case USER_STATE_MODERATOR:
					// Admins and moderators may always edit all posts.
					return true;
				case USER_STATE_NORMAL:
					// Continue below
					break;
				default:
					// Deactivated or invalid account.
					return false;
			}

			/* Normal users may edit only their own posts,
			 * but only within 24 hours after posting,
			 * and never if the post has been edited by an admin or moderator.
			 */
			sql = sql("select user,editor,created from posts where id=?", postID);
			sql.next();

			if (sql.getLong("user") != userID) return false;

			long editor = sql.getLong("editor");
			if (!sql.wasNull() && editor != userID) return false;

			long delta = Calendar.getInstance().getTimeInMillis() - getCalendar(sql, "created").getTimeInMillis();
			return delta < FreeRCTApplication.POST_EDIT_TIMEOUT;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Check whether the current user may delete a forum post.
	 * @param request Web request associated with the current session.
	 * @param postID Post to delete.
	 * @return The user may delete this post.
	 */
	public static boolean mayDeletePost(WebRequest request, long postID) {
		if (request.getUserPrincipal() == null) return false;  // Not logged in.
		try {
			ResultSet sql = sql("select id,state from users where username=?", request.getRemoteUser());
			sql.next();
			long userID = sql.getLong("id");
			switch (sql.getInt("state")) {
				case USER_STATE_ADMIN:
				case USER_STATE_MODERATOR:
					// Admins and moderators may always edit all posts.
					return true;
				default:
					// Continue below
					break;
			}

			sql = sql("select user from posts where id=?", postID);
			sql.next();
			return sql.getLong("user") == userID;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Check whether the current user is a website administrator.
	 * @param request Web request associated with the current session.
	 * @return The user has admin privileges.
	 */
	public static boolean isAdmin(WebRequest request) {
		if (request.getUserPrincipal() == null) return false;  // Not logged in.
		try {
			ResultSet sql = sql("select state from users where username=?", request.getRemoteUser());
			sql.next();
			return sql.getInt("state") == USER_STATE_ADMIN;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Check whether the current user is a moderator or admin.
	 * @param request Web request associated with the current session.
	 * @return The user has moderator privileges.
	 */
	public static boolean isModerator(WebRequest request) {
		if (request.getUserPrincipal() == null) return false;  // Not logged in.
		try {
			ResultSet sql = sql("select state from users where username=?", request.getRemoteUser());
			sql.next();
			switch (sql.getInt("state")) {
				case USER_STATE_ADMIN:
				case USER_STATE_MODERATOR:
					return true;
				default:
					return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	private class FreeRCTUserDetailsService implements UserDetailsService {
		private class FreeRCTUserDetails implements UserDetails {
			private final String username, password;
			private final int state;
			public FreeRCTUserDetails(String name, String pwd, int s) {
				username = name;
				password = pwd;
				state = s;
			}

			@Override public String getUsername() { return username; }
			@Override public String getPassword() { return password; }
			@Override public Collection<? extends GrantedAuthority> getAuthorities() { return null; }
			@Override public boolean isEnabled              () { return state != USER_STATE_DEACTIVATED && state != USER_STATE_AWAITING; }
			@Override public boolean isAccountNonExpired    () { return true; }
			@Override public boolean isAccountNonLocked     () { return state != USER_STATE_DEACTIVATED && state != USER_STATE_AWAITING; }
			@Override public boolean isCredentialsNonExpired() { return true; }
		}

		@Override
		public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
			try {
				ResultSet userDetails = sql("select password,state from users where username=?", username);

				if (!userDetails.next()) throw new Exception();

				return new FreeRCTUserDetails(username, userDetails.getString("password"), userDetails.getInt("state"));
			} catch (UsernameNotFoundException e) {
				throw e;
			} catch (Exception e) {
				throw new UsernameNotFoundException("User " + username + " is not registered");
			}
		}
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(new FreeRCTUserDetailsService());
		authProvider.setPasswordEncoder(passwordEncoder());

		auth.authenticationProvider(authProvider);
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.csrf().disable()
			.authorizeRequests()
			/* Admin's area – admin only. */
			.antMatchers("/admin/**").hasRole("ADMIN")

			/* Static assets. */
			.antMatchers("/css/**").permitAll()
			.antMatchers("/img/**").permitAll()
			.antMatchers("/public/**").permitAll()

			/* Logging in and error handling. */
			.antMatchers("/login").permitAll()
			.antMatchers("/login/*").permitAll()
			.antMatchers("/signup").permitAll()
			.antMatchers("/signup/*").permitAll()
			.antMatchers("/resetpassword").permitAll()
			.antMatchers("/submit_resetpassword").permitAll()
			.antMatchers("/ok").permitAll()
			.antMatchers("/error").permitAll()
			.antMatchers("/.well-known/acme-challenge/**").permitAll()

			/* Public-facing pages. */
			.antMatchers("/").permitAll()
			.antMatchers("/contact").permitAll()
			.antMatchers("/contribute").permitAll()
			.antMatchers("/download").permitAll()
			.antMatchers("/forum").permitAll()
			.antMatchers("/forum/*").permitAll()
			.antMatchers("/forum/post/*").permitAll()
			.antMatchers("/forum/topic/*").permitAll()
			.antMatchers("/manual").permitAll()
			.antMatchers("/play/*").permitAll()
			.antMatchers("/news").permitAll()
			.antMatchers("/screenshots").permitAll()
			/* The "/user/*" pages are not public. */

			/* All other pages are accessible only to authenticated users. */
			.anyRequest()
			.authenticated()
			.and()
			.formLogin()
			.loginPage("/login#")
			.loginProcessingUrl("/login/signin")
			.defaultSuccessUrl("/", false)
			.failureUrl("/login?type=login_failed#login_form")
			.successHandler(new CustomLoginSuccessHandler())
//			.failureHandler(authenticationFailureHandler())  // We do not need a custom failure handler currently.
			.and()
			.logout()
			.logoutUrl("/logout")
			.logoutSuccessUrl("/")
			.deleteCookies("JSESSIONID")
//			.logoutSuccessHandler(new CustomLogoutSuccessHandler())  // We do not need a custom success handler currently.

			/* When using an invalid or expired cookie. */  // We do not need this at the moment.
//			.and()
//			.sessionManagement()
//			.invalidSessionUrl("/login?type=expired#login_form");
			;
	}

	private class CustomLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
		@Override
		public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication auth)
		throws IOException, ServletException {
			String next = request.getParameter("next");
			if (next == null) {
				super.onAuthenticationSuccess(request, response, auth);
			} else {
				response.sendRedirect(next);
			}
		}
	}
}
