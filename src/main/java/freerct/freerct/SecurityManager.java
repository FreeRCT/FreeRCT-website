package freerct.freerct;

import java.sql.*;
import java.util.*;

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
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;

@Configuration
@EnableWebSecurity
public class SecurityManager extends WebSecurityConfigurerAdapter {
	private class FreeRCTUserDetailsService implements UserDetailsService {
		private class FreeRCTUserDetails implements UserDetails {
			private final String username, password;
			public FreeRCTUserDetails(String name, String pwd) {
				username = name;
				password = pwd;
			}

			@Override public String getUsername() { return username; }
			@Override public String getPassword() { return password; }
			@Override public Collection<? extends GrantedAuthority> getAuthorities() { return null; }
			@Override public boolean isEnabled              () { return true; }
			@Override public boolean isAccountNonExpired    () { return true; }
			@Override public boolean isAccountNonLocked     () { return true; }
			@Override public boolean isCredentialsNonExpired() { return true; }
		}

		@Override
		public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
			try {
				ResultSet userDetails = FreeRCTApplication.sql("select password from users where username=?", username);
				if (!userDetails.next()) throw new Exception();
				return new FreeRCTUserDetails(username, new BCryptPasswordEncoder().encode​(userDetails.getString("password")));
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

	/* @Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
		auth
			.inMemoryAuthentication()
			.withUser("user")
			.password(encoder.encode("password"))
			.roles("USER")
			.and()
			.withUser("admin")
			.password(encoder.encode("admin"))
			.roles("USER", "ADMIN");
	} */

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.csrf().disable()
			.authorizeRequests()
			/* Admin's area. */
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
// NOCOM disabled for testing only
//			.antMatchers("/forum/topic/*").permitAll()
//			.antMatchers("/forum/post/*").permitAll()
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
//			.failureHandler(authenticationFailureHandler())  // ???
			.and()
			.logout()
			.logoutUrl("/logout")
			.logoutSuccessUrl("/")
			.deleteCookies("JSESSIONID")
//			.logoutSuccessHandler(logoutSuccessHandler())  // ???
			;
	}
}
