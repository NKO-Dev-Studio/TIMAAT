package de.bitgilde.TIMAAT.rest.filter;

import de.bitgilde.TIMAAT.TIMAATApp;
import de.bitgilde.TIMAAT.model.AccountSuspendedException;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccount;
import de.bitgilde.TIMAAT.model.FIPOP.UserAccountStatus;
import de.bitgilde.TIMAAT.rest.Secured;
import de.bitgilde.TIMAAT.security.TIMAATKeyGenerator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import javax.crypto.SecretKey;
import java.io.IOException;

/*
 Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

/**
 * This filter is performing the authentication for a user token passed through an authorization header.
 * After validating the token the userId, username and userAccount object will be provided inside
 * the {@link ContainerRequestContext}.
 *
 * @author Jens-Martin Loebel <loebel@bitgilde.de>
 * @author Mirko Scherf <mscherf@uni-mainz.de>
 * @author Nico Kotlenga <nico@kotlenga.dev>
 */
@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

  public static final String USER_ID_PROPERTY_NAME = "TIMAAT.userID";
  public static final String USER_NAME_PROPERTY_NAME = "TIMAAT.userName";
  public static final String USER_ACCOUNT_PROPERTY_NAME = "TIMAAT.user";

	private static final String REALM = "TIMAAT";
  private static final String AUTHENTICATION_SCHEME = "Bearer";

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		// Get the Authorization header from the request
    String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    // Validate the Authorization header
    if (!isTokenBasedAuthentication(authorizationHeader)) {
      abortWithUnauthorized(requestContext);
      return;
    }

    // Extract the token from the Authorization header
    String token = authorizationHeader.substring(AUTHENTICATION_SCHEME.length()).trim();
    if (token.equals("null") || token == null) {
      abortWithUnauthorized(requestContext);
      return;
    }
    try {

      // Validate the token
      String username = validateToken(token);

      try {
        // Validate user status
        UserAccount user = validateAccountStatus(username);

        // Authentication succeeded, set request context
        requestContext.setProperty(USER_ID_PROPERTY_NAME, user.getId());
        requestContext.setProperty(USER_NAME_PROPERTY_NAME, user.getAccountName());
        requestContext.setProperty(USER_ACCOUNT_PROPERTY_NAME, user);

      } catch (AccountSuspendedException e) {
        abortWithForbidden(requestContext, "This account has been suspended.");
      }

    } catch (Exception e) {
        abortWithUnauthorized(requestContext);
    }
  }

	private boolean isTokenBasedAuthentication(String authorizationHeader) {

    // Check if the Authorization header is valid
    // It must not be null and must be prefixed with "Bearer" plus a whitespace
    // The authentication scheme comparison must be case-insensitive
    return authorizationHeader != null && authorizationHeader.toLowerCase()
                .startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
  }

  private void abortWithUnauthorized(ContainerRequestContext requestContext) {
    // Abort the filter chain with a 401 status code response
    // The WWW-Authenticate header is sent along with the response
    requestContext.abortWith(
              Response.status(Response.Status.UNAUTHORIZED)
                      .header(HttpHeaders.WWW_AUTHENTICATE,
                              AUTHENTICATION_SCHEME + " realm=\"" + REALM + "\"")
                      .build());
  }

  private void abortWithForbidden(ContainerRequestContext requestContext, String reason) {
    // Abort the filter chain with a 403 status code response
    // The WWW-Authenticate header is sent along with the response
    requestContext.abortWith(
              Response.status(Response.Status.FORBIDDEN)
                      .header(HttpHeaders.WWW_AUTHENTICATE,
                              AUTHENTICATION_SCHEME + " realm=\"" + REALM + "\"")
                      .entity("{\"reason\":\""+reason+"\"}")
                      .build());
  }

  public static UserAccount validateAccountStatus(String username) throws Exception {
    // verify user and user status
    if ( username == null ) throw new Exception("provided credentials invalid");

    UserAccount user = (UserAccount) TIMAATApp.emf.createEntityManager()
      .createQuery("SELECT ua FROM UserAccount ua WHERE ua.accountName=:username")
      .setParameter("username", username)
      .getSingleResult();

    // don't allow suspended accounts
    if ( user.getUserAccountStatus() == UserAccountStatus.suspended )
      throw new AccountSuspendedException();

    return user;
  }

  public static String validateToken(String token) {
    if (token.equals("null") || token == null) return null;
    // Check if the token was issued by the server and if it's not expired
    // Throw an Exception if the token is invalid
    SecretKey key = TIMAATKeyGenerator.generateKey();
    String username = "";
      try {
        username = Jwts.parser().decryptWith(key).build().parseSignedClaims(token).getPayload().getSubject();
      } catch (JwtException e) {
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
    return username;
  }

  public static Boolean isTokenValid(String token) {
      try {
        Jws<Claims> jws = Jwts.parser().decryptWith(TIMAATKeyGenerator.generateKey()).build().parseSignedClaims(token);
      } catch (JwtException e) {
          e.printStackTrace();
          return false;
      }
      return true;
  }

  public static int getTokenClaimUserId(String token) {
      SecretKey key = TIMAATKeyGenerator.generateKey();
      Claims claims = null;
      try {
          claims = Jwts.parser().decryptWith(key).build().parseSignedClaims(token).getPayload();
      } catch(InvalidClaimException ice) {
        ice.printStackTrace();
      }
      int id = claims.get("id", Integer.class);
      return id;
  }

}
