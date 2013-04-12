
package com.qian.weeno.io.model;

import com.google.gson.JsonElement;

public class GenericResponse {
    public JsonElement error;
//
//    public void checkResponseForAuthErrorsAndThrow() throws IOException {
//        if (error != null && error.isJsonObject()) {
//            JsonObject errorObject = error.getAsJsonObject();
//            int errorCode = errorObject.get("code").getAsInt();
//            String errorMessage = errorObject.get("message").getAsString();
//            if (400 <= errorCode && errorCode < 500) {
//                // The API currently only returns 400 unfortunately.
//                throw ...
//            }
//        }
//    }
}
