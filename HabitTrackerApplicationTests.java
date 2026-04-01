package com.example.project1.Habit.Tracker;

import com.example.project1.Habit.Tracker.repository.AppUserRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
class HabitTrackerApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AppUserRepository appUserRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void loginPageLoads() throws Exception {
		mockMvc.perform(get("/login"))
			.andExpect(status().isOk())
			.andExpect(content().string(Matchers.containsString("Create an account")));
	}

	@Test
	void registerPageLoads() throws Exception {
		mockMvc.perform(get("/register"))
			.andExpect(status().isOk())
			.andExpect(content().string(Matchers.containsString("Create a habit tracker account.")));
	}

	@Test
	void dashboardRedirectsToLoginWhenAnonymous() throws Exception {
		mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login"));
	}

	@Test
	void userCanRegisterAndThenLogin() throws Exception {
		String username = "rahul" + System.nanoTime();
		String email = username + "@example.com";

		mockMvc.perform(post("/register")
				.with(csrf())
				.param("username", username)
				.param("email", email)
				.param("password", "secret123"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login?registered"));

		assertThat(appUserRepository.findByUsernameIgnoreCase(username)).isPresent();

		mockMvc.perform(post("/login")
				.with(csrf())
				.param("username", username)
				.param("password", "secret123"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/"));
	}

	@Test
	void duplicateUsernameShowsRegistrationError() throws Exception {
		String username = "duplicate" + System.nanoTime();

		mockMvc.perform(post("/register")
				.with(csrf())
				.param("username", username)
				.param("email", username + "@example.com")
				.param("password", "secret123"))
			.andExpect(status().is3xxRedirection());

		mockMvc.perform(post("/register")
				.with(csrf())
				.param("username", username)
				.param("email", "other@example.com")
				.param("password", "secret123"))
			.andExpect(status().isOk())
			.andExpect(content().string(Matchers.containsString("That username is already taken.")));
	}

	@Test
	void restApiAllowsAuthenticatedCreateWithoutCsrfToken() throws Exception {
		mockMvc.perform(post("/habits")
				.with(httpBasic("admin", "admin123"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "Read",
					  "description": "20 minutes",
					  "completed": false
					}
					"""))
			.andExpect(status().isCreated());
	}

	@Test
	void deleteMissingHabitReturnsNotFound() throws Exception {
		mockMvc.perform(delete("/habits/99999")
				.with(httpBasic("admin", "admin123")))
			.andExpect(status().isNotFound());
	}
}
