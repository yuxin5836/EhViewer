/*
 * Copyright 2017 Hippo Seven
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

package com.hippo.ehviewer.client.converter;

/*
 * Created by Hippo on 1/18/2017.
 */

import android.support.annotation.NonNull;
import com.hippo.ehviewer.client.EhConverter;
import com.hippo.ehviewer.client.exception.GeneralException;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.result.SignInResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link retrofit2.Converter} to parse the html of signing in.
 */
public class SignInConverter extends EhConverter<SignInResult> {

  private static final Pattern PATTERN_NAME = Pattern.compile("<p>You are now logged in as: (.+?)<");
  private static final Pattern PATTERN_ERROR = Pattern.compile(
      "(?:<h4>The error returned was:</h4>\\s*<p>(.+?)</p>)"
          + "|(?:<span class=\"postcolor\">(.+?)</span>)");

  @NonNull
  @Override
  public SignInResult convert(String body) throws Exception {
    Matcher m = PATTERN_NAME.matcher(body);
    if (m.find()) {
      return new SignInResult(m.group(1));
    } else {
      m = PATTERN_ERROR.matcher(body);
      if (m.find()) {
        throw new GeneralException(m.group(1) == null ? m.group(2) : m.group(1));
      } else {
        throw new ParseException("Can't parse the html of signing in", body);
      }
    }
  }


  ////////////////
  // Pain part
  ////////////////

  @NonNull
  @Override
  public SignInResult error(Throwable t) {
    return SignInResult.error(t);
  }
}