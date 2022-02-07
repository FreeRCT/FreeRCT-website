package freerct.freerct;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.springframework.context.annotation.*;
import org.springframework.security.authentication.dao.*;
import org.springframework.security.config.annotation.authentication.builders.*;
import org.springframework.security.config.annotation.web.builders.*;
import org.springframework.security.config.annotation.web.configuration.*;
import org.springframework.security.core.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.factory.*;
import org.springframework.security.crypto.password.*;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.authentication.logout.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

@Configuration
@EnableWebSecurity
public class SecurityManager extends WebSecurityConfigurerAdapter {
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
			@Override public boolean isEnabled              () { return state != UserProfile.USER_STATE_DEACTIVATED; }
			@Override public boolean isAccountNonExpired    () { return true; }
			@Override public boolean isAccountNonLocked     () { return state != UserProfile.USER_STATE_DEACTIVATED; }
			@Override public boolean isCredentialsNonExpired() { return true; }
		}

		@Override
		public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
			try {
				ResultSet userDetails = FreeRCTApplication.sql("select password,state from users where username=?", username);

				if (!userDetails.next()) throw new Exception();

				return new FreeRCTUserDetails(username, new BCryptPasswordEncoder().encode​(userDetails.getString("password")), userDetails.getInt("state"));
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
		authProvider.setPasswordEncoder(new BCryptPasswordEncoder());

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

			/* Public-facing pages. */
			.antMatchers("/login").permitAll()
			.antMatchers("/login/*").permitAll()
			.antMatchers("/").permitAll()
			.antMatchers("/news").permitAll()
			.antMatchers("/manual").permitAll()
			.antMatchers("/screenshots").permitAll()
			.antMatchers("/download").permitAll()
			.antMatchers("/contribute").permitAll()
			.antMatchers("/forum").permitAll()
			.antMatchers("/forum/*").permitAll()
			.antMatchers("/forum/topic/*").permitAll()
			.antMatchers("/forum/post/*").permitAll()
			/* The "/user/*" pages are not public. */

			/* Everything else only for authenticated users. */
			.anyRequest()
			.authenticated()
			.and()
			.formLogin()
			.loginPage("/login#")
			.loginProcessingUrl("/login/signin")
			.defaultSuccessUrl("/", false)
			.failureUrl("/login?error=true")
			.successHandler(new CustomLoginSuccessHandler())
//			.failureHandler(authenticationFailureHandler())  // We do not need a custom failure handler currently.
			.and()
			.logout()
			.logoutUrl("/logout")
			.logoutSuccessUrl("/")
			.deleteCookies("JSESSIONID")
			.logoutSuccessHandler(new CustomLogoutSuccessHandler())
			;
	}

	private static List<String> _loggedInUsers = new ArrayList<>();  // Not a set because it may contain duplicates.

	public static SortedSet<String> getLoggedInUsers() {
		SortedSet<String> set = new TreeSet<>();
		for (String str : _loggedInUsers) set.add(str);
		return set;
	}

	private class CustomLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
		@Override
		public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication auth)
		throws IOException, ServletException {
			synchronized (_loggedInUsers) {
				_loggedInUsers.add(((UserDetails)auth.getPrincipal()).getUsername());
			}
			super.onAuthenticationSuccess(request, response, auth);
		}
	}
	private class CustomLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {
		@Override
		public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication auth)
		throws IOException, ServletException {
			synchronized (_loggedInUsers) {
				_loggedInUsers.remove(((UserDetails)auth.getPrincipal()).getUsername());
			}
			super.onLogoutSuccess(request, response, auth);
		}
	}
}
