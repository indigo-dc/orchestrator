/*
 * Copyright © 2015-2018 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.reply.orchestrator.utils;

import com.google.common.base.Preconditions;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;

import java.text.ParseException;
import java.util.Date;
import java.util.Optional;

import lombok.experimental.UtilityClass;

@UtilityClass
public class JwtUtils {

  /**
   * Parse a String in a {@link JWT}.
   * 
   * @param jwtToken
   *          the string to parse
   * @return the parsed JWT
   */
  public static JWT parseJwt(String jwtToken) {
    try {
      return JWTParser.parse(jwtToken);
    } catch (RuntimeException | ParseException ex) {
      throw new IllegalArgumentException(String.format("<%S> is not a valid JWT", jwtToken), ex);
    }
  }

  /**
   * Extract the claims from a {@link JWT}.
   * 
   * @param jwtToken
   *          the JWT token
   * @return the JWT's claims
   */
  public static JWTClaimsSet getJwtClaimsSet(JWT jwtToken) {
    try {
      JWTClaimsSet claims = Preconditions.checkNotNull(jwtToken).getJWTClaimsSet();
      return Preconditions.checkNotNull(claims, "JWT token doesn't have valid claims");
    } catch (ParseException ex) {
      throw new IllegalArgumentException("JWT token doesn't have valid claims", ex);
    }
  }

  /**
   * Extract the issuer claim from a {@link JWT}.
   * 
   * @param jwtToken
   *          the JWT token
   * @return the issuer claim
   */
  public static String getIssuer(JWT jwtToken) {
    return Optional
        .ofNullable(JwtUtils.getJwtClaimsSet(jwtToken).getIssuer())
        .orElseThrow(() -> new IllegalArgumentException("No issuer claim found in JWT"));
  }

  /**
   * Extract the subject claim from a {@link JWT}.
   * 
   * @param jwtToken
   *          the JWT token
   * @return the subject claim
   */
  public static String getSubject(JWT jwtToken) {
    return Optional
        .ofNullable(JwtUtils.getJwtClaimsSet(jwtToken).getSubject())
        .orElseThrow(() -> new IllegalArgumentException("No subject claim found in JWT"));
  }

  /**
   * Extract the jti claim from a {@link JWT}.
   * 
   * @param jwtToken
   *          the JWT token
   * @return the jti claim
   */
  public static String getJti(JWT jwtToken) {
    return Optional
        .ofNullable(JwtUtils.getJwtClaimsSet(jwtToken).getJWTID())
        .orElseThrow(() -> new IllegalArgumentException("No jti claim found in JWT"));
  }

  public static Optional<Date> getExpirationTime(JWT jwtToken) {
    return Optional.ofNullable(getJwtClaimsSet(jwtToken).getExpirationTime());
  }

  /**
   * Checks if a {@link JWT} is expired.
   * 
   * @param jwtToken
   *          the JWT token
   * @return true if the JWT is expired, false otherwise
   */
  public static boolean isJtwTokenExpired(JWT jwtToken) {
    return getExpirationTime(jwtToken)
        .filter(expirationDate -> expirationDate.before(new Date()))
        .isPresent();
  }

}
