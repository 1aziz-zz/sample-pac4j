package controller;

import java.util.HashMap;
import java.util.Map;

import org.pac4j.core.config.Config;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;

import org.pac4j.sparkjava.ApplicationLogoutRoute;
import org.pac4j.sparkjava.CallbackRoute;
import org.pac4j.sparkjava.RequiresAuthenticationFilter;
import org.pac4j.sparkjava.SparkWebContext;

import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.template.mustache.MustacheTemplateEngine;

import static spark.Spark.*;
import java.util.UUID;


@SuppressWarnings({"unchecked"})
public class App {



	private final static MustacheTemplateEngine templateEngine = new MustacheTemplateEngine();
	private static AppConfig appConfig = new AppConfig();
	private static String generatedCode = "";
	public static void main(String[] args) {
		port(8080);

		final Config config = new DemoConfigFactory( templateEngine).build();


		get("/rest/public/", (request, response) -> {
			if (shouldReturnHtml(request)) {
				response.status(200);
				response.type("text/html");

				Map<String, Object> attributes = new HashMap<>();
				String publicKey = appConfig.getValue(AppConfig.Key.PUBLIC);
				attributes.put("result", publicKey);
				return templateEngine.render(new ModelAndView(attributes, "result.mustache"));
			} else {
				response.status(200);
				response.type("application/json");
				return null;
			}
		});

		get("/rest/een/", (request, response) -> {

			if (shouldReturnHtml(request)) {
				response.status(200);
				response.type("text/html");
				return templateEngine.render(new ModelAndView(null, "een.mustache"));
			} else {
				response.status(200);
				response.type("application/json");
				return null;
			}
		});

		get("/rest/twee/", (request, response) -> {
            if (shouldReturnHtml(request)) {
                response.status(200);
                response.type("text/html");
                return templateEngine.render(new ModelAndView(null, "twee.mustache"));
            } else {
                response.status(200);
                response.type("application/json");
                return null;
            }
        });
		post("/rest/verification/", (request, response) -> {
			String username = request.queryParams("username");
			String password = request.queryParams("password");
			MailService mailService = new MailService();
			if (shouldReturnHtml(request)) {
				response.status(200);
				response.type("text/html");
				String message = null;
				Map<String, Object> attributes = new HashMap<>();
				if (username.equals("zokrijgjehetresultaat@gmail.com")
						&& password.equals("utrechtutrecht")) {
					generatedCode = codeGenerator();
					mailService.generateAndSendEmail(generatedCode,username);

				} else {
					message = "Username / password not valid, Please try again.";
				}
				attributes.put("message", message);
				return templateEngine.render(new ModelAndView(attributes, "verification.mustache"));

			} else {
				response.status(200);
				response.type("application/json");
				return null;
			}
		});
		get("/rest/result/", (request, response) -> {
			String codeInput = request.queryParams("verification_code");

			if (shouldReturnHtml(request)) {
				response.status(200);
				response.type("text/html");
				Map<String, Object> attributes = new HashMap<>();
				String key;
				if (codeInput.equals(generatedCode)) {
					key = appConfig.getValue(AppConfig.Key.VERYSECRET);
				} else {
					key = "Invalid code. Please try again.";
				}
				attributes.put("result", key);
				return templateEngine.render(new ModelAndView(attributes, "result.mustache"));
			} else {
				response.status(200);
				response.type("application/json");
				return null;
			}
		});


		post("/rest/result/", (request, response) -> {
			String username = request.queryParams("username");
			String password = request.queryParams("password");
			System.out.printf(username+password);
			String secretKey;
			if (shouldReturnHtml(request)) {
				response.status(200);
				response.type("text/html");

				Map<String, Object> attributes = new HashMap<>();
				if (username.equals("zokrijgjehetresultaat@gmail.com")
						&& password.equals("utrechtutrecht")) {
					secretKey = appConfig.getValue(AppConfig.Key.SECRET);
				} else {
					secretKey = "Username / password not valid";
				}
				attributes.put("result", secretKey);
				return templateEngine.render(new ModelAndView(attributes, "result.mustache"));
			} else {
				response.status(200);
				response.type("application/json");
				return null;
			}
		});
		get("/", App::index, templateEngine);
		final Route callback = new CallbackRoute(config);
		get("/callback", callback);
		post("/callback", callback);
        final RequiresAuthenticationFilter facebookFilter = new RequiresAuthenticationFilter(config, "FacebookClient", "", "excludedPath");
        before("/facebook", facebookFilter);
		get("/facebook", App::protectedIndex, templateEngine);

		get("/logout", new ApplicationLogoutRoute(config));

    }

	private static ModelAndView index(final Request request, final Response response) {
		final Map map = new HashMap();
		map.put("profile", getUserProfile(request, response));
		return new ModelAndView(map, "index.mustache");
	}


	private static ModelAndView protectedIndex(final Request request, final Response response) {
		final Map map = new HashMap();
		map.put("profile", getUserProfile(request, response));
		String topSecret = appConfig.getValue(AppConfig.Key.TOPSECRET);
		map.put("secretkey",topSecret);
		map.put("profileName",getUserProfile(request,response).getAttribute("name"));
		map.put("name",getUserProfile(request,response).getAttribute("first_name"));
		map.put("email",getUserProfile(request,response).getAttribute("email"));

		return new ModelAndView(map, "protectedIndex.mustache");

	}

	private static UserProfile getUserProfile(final Request request, final Response response) {
		final SparkWebContext context = new SparkWebContext(request, response);
		final ProfileManager manager = new ProfileManager(context);
		return manager.get(true);
	}

	private static boolean shouldReturnHtml(Request request) {
		String accept = request.headers("Accept");
		return accept != null && accept.contains("text/html");
	}
	private static String codeGenerator() {
		return UUID.randomUUID().toString();
	}
}
