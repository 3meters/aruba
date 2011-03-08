/*
 * Copyright 2010 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.georain.ripple.model;

/**
 * Encapsulation of a Ripple Error: a Ripple request that could not be 
 * fulfilled.
 * 
 * @author ssoneff@facebook.com
 */
public class RippleError extends Throwable {
  
    private static final long serialVersionUID = 1L;

    private int mErrorCode = 0;
    private String mErrorType;
    
    public RippleError(String message) {
        super(message);
    }

    public RippleError(String message, String type, int code) {
        super(message);
        mErrorType = type;
        mErrorCode = code;
    }
    
    public int getErrorCode() {
        return mErrorCode;
    }
    
    public String getErrorType() {
        return mErrorType;
    }
}
