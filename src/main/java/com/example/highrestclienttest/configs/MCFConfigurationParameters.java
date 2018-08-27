/* $Id$ */
/* Modified 2015-07-01 by Bart Superson */
/**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.example.highrestclienttest.configs;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

/** This class represents the configuration information that the QueryModifier
* needs to perform its job.
*/
@Value
@Builder(toBuilder = true)
@SuppressWarnings("all")
public class MCFConfigurationParameters
{
  /** Base URL, e.g. "http://localhost:8345/mcf-authority-service" */
  @Builder.Default
  String authorityServiceBaseURL = "http://localhost:8345/mcf-authority-service";

  /** Connection timeout, e.g. 60000 */
  @Builder.Default
  int connectionTimeout = 60000;

  /** Socket timeout, e.g. 300000 */
  @Builder.Default
  int socketTimeout = 300000;

  /** Allow field prefix, e.g. "allow_token_" */
  @Builder.Default
  String allowFieldPrefix = "allow_token_";

  /** Deny field prefix, e.g. "deny_token_" */
  @Builder.Default
  String denyFieldPrefix = "deny_token_";

  /** Connection pool size, e.g. 50 */
  @Builder.Default
  int connectionPoolSize = 50;


}