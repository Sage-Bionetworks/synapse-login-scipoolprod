
package synapseawsconsolelogin;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Token;
import org.scribe.model.Verifier;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleWithWebIdentityRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleWithWebIdentityResult;
import com.amazonaws.services.securitytoken.model.Credentials;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;


public class Auth extends HttpServlet {
	private static Logger logger = Logger.getLogger("Auth");

	private static final String REQUIRED_SYNAPSE_TEAM_ID = "273957";
	private static final String CLAIMS = "{\"team\":{\"values\":[\""+REQUIRED_SYNAPSE_TEAM_ID+"\"]},"
			+ "\"user_name\":{\"essential\":true}"
			+ "\"family_name\":{\"essential\":true},"
			+ "\"given_name\":{\"essential\":true},"
			+ "\"email\":{\"essential\":true},"
			+ "\"userid\":{\"essential\":true}}";
	
    private static final String AUTHORIZE_URL_SYNAPSE = 
    		"https://signin.synapse.org?response_type=code&client_id=%s&redirect_uri=%s&"+
    		"claims={\"id_token\":"+CLAIMS+",\"userinfo\":"+CLAIMS+"}";

    private static final String TOKEN_URL_SYNAPSE = "https://repo-prod.prod.sagebase.org/auth/v1/oauth2/token";

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		try {
			doPostIntern(req, resp);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "", e);
			resp.setContentType("text/plain");
			try (ServletOutputStream os=resp.getOutputStream()) {
				os.println("Error:");
				e.printStackTrace(new PrintStream(os));
			}
			resp.setStatus(500);
		}
	}

	private static final String CLIENT_ENDPOINT = "http://synapseawsconsolelogin.appspot.com/";
	private static final String SYNAPSE_BUTTON_URI = "synapse";
	
	private String getRedirectBackUrlSynapse(HttpServletRequest req) throws MalformedURLException {
		return CLIENT_ENDPOINT+SYNAPSE_BUTTON_URI;
	}
		
	private static String getClientIdSynapse() {
		String result = getProperty("SYNAPSE_OAUTH_CLIENT_ID");
		logger.log(Level.WARNING, "SYNAPSE_OAUTH_CLIENT_ID="+result);
		return result;
	}
	
	private static String getClientSecretSynapse() {
		String result =  getProperty("SYNAPSE_OAUTH_CLIENT_SECRET");
		logger.log(Level.WARNING, "SYNAPSE_OAUTH_CLIENT_SECRET="+result);
		return result;
	}
	

	
	private void doPostIntern(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		if (req.getRequestURI().contains(SYNAPSE_BUTTON_URI)) {
			String redirectBackUrl = getRedirectBackUrlSynapse(req);
			String redirectUrl = new OAuth2Api(AUTHORIZE_URL_SYNAPSE, TOKEN_URL_SYNAPSE).
					getAuthorizationUrl(new OAuthConfig(getClientIdSynapse(), null, redirectBackUrl, null, "openid", null));
			resp.setHeader("Location", redirectUrl+"&state=someRandomStateToPassThrough");
			resp.setStatus(303);
		} else {
			throw new RuntimeException("Unexpected URI "+req.getRequestURI());
		}
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		try {
			doGetIntern(req, resp);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "", e);
			resp.setContentType("text/plain");
			try (ServletOutputStream os=resp.getOutputStream()) {
				os.println("Error:");
				e.printStackTrace(new PrintStream(os));
			}
			resp.setStatus(500);
		}
	}
	
	// from https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_providers_enable-console-custom-url.html#STSConsoleLink_programJava
	private String getConsoleLoginURL(Credentials federatedCredentials) throws IOException {

		// The issuer parameter specifies your internal sign-in
		// page, for example https://mysignin.internal.mycompany.com/.
		// The console parameter specifies the URL to the destination console of the
		// AWS Management Console. This example goes to Amazon SNS. 
		// The signin parameter is the URL to send the request to.

		String issuerURL = CLIENT_ENDPOINT;
		String consoleURL = "https://console.aws.amazon.com/servicecatalog";
		String signInURL = "https://signin.aws.amazon.com/federation";
		  
		// Create the sign-in token using temporary credentials,
		// including the access key ID,  secret access key, and security token.
		String sessionJson = String.format(
		  "{\"%1$s\":\"%2$s\",\"%3$s\":\"%4$s\",\"%5$s\":\"%6$s\"}",
		  "sessionId", federatedCredentials.getAccessKeyId(),
		  "sessionKey", federatedCredentials.getSecretAccessKey(),
		  "sessionToken", federatedCredentials.getSessionToken());
		              
		// Construct the sign-in request with the request sign-in token action, a
		// 12-hour console session duration, and the JSON document with temporary 
		// credentials as parameters.

		String getSigninTokenURL = signInURL + 
		                           "?Action=getSigninToken" +
		                           "&DurationSeconds=43200" + 
		                           "&SessionType=json&Session=" + 
		                           URLEncoder.encode(sessionJson,"UTF-8");

		URL url = new URL(getSigninTokenURL);

		// Send the request to the AWS federation endpoint to get the sign-in token
		URLConnection conn = url.openConnection();

		BufferedReader bufferReader = new BufferedReader(new 
		  InputStreamReader(conn.getInputStream()));  
		String returnContent = bufferReader.readLine();

		String signinToken = new JSONObject(returnContent).getString("SigninToken");

		String signinTokenParameter = "&SigninToken=" + URLEncoder.encode(signinToken,"UTF-8");

		// The issuer parameter is optional, but recommended. Use it to direct users
		// to your sign-in page when their session expires.

		String issuerParameter = "&Issuer=" + URLEncoder.encode(issuerURL, "UTF-8");

		// Finally, present the completed URL for the AWS console session to the user

		String destinationParameter = "&Destination=" + URLEncoder.encode(consoleURL,"UTF-8");
		String loginURL = signInURL + "?Action=login" +
		                     signinTokenParameter + issuerParameter + destinationParameter;	
		return loginURL;
	}
	
	public static Jwt<Header,Claims> parseJWT(String token) {
		// Note, we don't check the signature
		String[] pieces = token.split("\\.");
		if (pieces.length!=3) throw new IllegalArgumentException("Expected three sections of the token but found "+pieces.length);
		String unsignedToken = pieces[0]+"."+pieces[1]+".";
		return Jwts.parser().parseClaimsJwt(unsignedToken);
	}
		
	private void doGetIntern(HttpServletRequest req, HttpServletResponse resp)
				throws Exception {
		
		OAuth2Api.BasicOAuth2Service service = null;
		if (req.getRequestURI().contains(SYNAPSE_BUTTON_URI)) {
			service = (OAuth2Api.BasicOAuth2Service)(new OAuth2Api(AUTHORIZE_URL_SYNAPSE, TOKEN_URL_SYNAPSE)).
					createService(new OAuthConfig(getClientIdSynapse(), getClientSecretSynapse(), getRedirectBackUrlSynapse(req), null, null, null));
			String authorizationCode = req.getParameter("code");
			Token idToken = service.getIdToken(null, new Verifier(authorizationCode));
			
			// parse ID Token
			Jwt<Header,Claims> jwt = parseJWT(idToken.getToken());
			String synapseUserId = jwt.getBody().get("userid", String.class);
			// check if a member of 273957.  If not, don't proceed
			List<String> teamIds = jwt.getBody().get("team", List.class);
			boolean isInDesignatedTeam = (teamIds!=null && teamIds.contains(REQUIRED_SYNAPSE_TEAM_ID));
			
			if (isInDesignatedTeam) {
				// get STS token
				AssumeRoleWithWebIdentityRequest assumeRoleWithWebIdentityRequest = new AssumeRoleWithWebIdentityRequest();
				assumeRoleWithWebIdentityRequest.setWebIdentityToken(idToken.getToken());
				assumeRoleWithWebIdentityRequest.setRoleArn("arn:aws:iam::563295687221:role/Service_Catalog_Role");
				assumeRoleWithWebIdentityRequest.setRoleSessionName(synapseUserId);
				AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard().withRegion(Regions.US_EAST_1)
						.withCredentials(new AWSCredentialsProvider() {
							@Override
							public AWSCredentials getCredentials() {
								return new AWSCredentials() {
									@Override
									public String getAWSAccessKeyId() {
										return "dummyKeyId";
									}
									@Override
									public String getAWSSecretKey() {
										return "dummySecret";
									}};
							}
							@Override
							public void refresh() {}}).build();
				
				// TODO pass tags https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRoleWithWebIdentity.html
				AssumeRoleWithWebIdentityResult assumeRoleWithWebIdentityResult = stsClient.assumeRoleWithWebIdentity(assumeRoleWithWebIdentityRequest);
				Credentials credentials = assumeRoleWithWebIdentityResult.getCredentials();
				logger.log(Level.INFO, credentials.toString());
				// redirect to AWS login
				String redirectURL = getConsoleLoginURL(credentials);
				
				resp.setHeader("Location", redirectURL);
				resp.setStatus(302);
			} else {
				resp.setContentType("text/plain");
				try (ServletOutputStream os=resp.getOutputStream()) {
					os.println("To proceed you must be a member of team "+REQUIRED_SYNAPSE_TEAM_ID);
				}
				resp.setStatus(200);
			}
		} else {
			throw new RuntimeException("Unexpected URI "+req.getRequestURI());
		}
	}
	
	
	private static Properties properties = null;

	public static void initProperties() {
		if (properties!=null) return;
		properties = new Properties();
		InputStream is = null;
		try {
			is = Auth.class.getClassLoader().getResourceAsStream("global.properties");
			properties.load(is);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (is!=null) try {
				is.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static String getProperty(String key) {
		return getProperty(key, true);
	}
	
	private static boolean missing(String s) {
		return StringUtils.isEmpty(s) || "null".equals(s);
	}

	public static String getProperty(String key, boolean required) {
		initProperties();
		{
			String embeddedProperty = properties.getProperty(key);
			if (!missing(embeddedProperty)) return embeddedProperty;
		}
		{
			String environmentVariable = System.getenv(key);
			if (!missing(environmentVariable)) return environmentVariable;
		}
		{
			String commandlineOption = System.getProperty(key);
			if (!missing(commandlineOption)) return commandlineOption;
		}
		if (required) throw new RuntimeException("Cannot find value for "+key);
		return null;
	}

}