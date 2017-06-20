/*
 * Copyright © 2015-2017 Santer Reply S.p.A.
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

import lombok.experimental.UtilityClass;

import java.text.ParseException;
import java.util.Date;
import java.util.Optional;

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
      return JWTParser.parse(Preconditions.checkNotNull(jwtToken));
    } catch (ParseException ex) {
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

  public static JWTClaimsSet getJwtClaimsSet(String jwtToken) {
    return getJwtClaimsSet(parseJwt(jwtToken));
  }

  public static Optional<Date> getExpirationTimeFromJwt(JWT jwtToken) {
    return Optional.ofNullable(getJwtClaimsSet(jwtToken).getExpirationTime());
  }

  public static Optional<Date> getExpirationTimeFromJwt(String jwtToken) {
    return getExpirationTimeFromJwt(parseJwt(jwtToken));
  }

  public static boolean isJtwTokenExpired(JWT jwtToken) {
    return getExpirationTimeFromJwt(jwtToken)
        .filter(expirationDate -> expirationDate.before(new Date())).isPresent();
  }

  public static boolean isJtwTokenExpired(String jwtToken) {
    return isJtwTokenExpired(parseJwt(jwtToken));
  }

}
