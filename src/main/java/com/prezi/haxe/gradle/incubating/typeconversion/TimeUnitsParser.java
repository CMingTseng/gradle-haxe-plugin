/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.prezi.haxe.gradle.incubating.typeconversion;

import org.gradle.api.InvalidUserDataException;

import java.util.concurrent.TimeUnit;

import static com.prezi.haxe.gradle.incubating.typeconversion.NormalizedTimeUnit.millis;

public class TimeUnitsParser {

    public NormalizedTimeUnit parseNotation(CharSequence notation, int value) {
        String candidate = notation.toString().toUpperCase();
        //jdk5 does not have days, hours or minutes, normalizing to millis
        if (candidate.equals("DAYS")) {
            return millis(value * 24 * 60 * 60 * 1000);
        } else if (candidate.equals("HOURS")) {
            return millis(value * 60 * 60 * 1000);
        } else if (candidate.equals("MINUTES")) {
            return millis(value * 60 * 1000);
        }
        try {
            return new NormalizedTimeUnit(value, TimeUnit.valueOf(candidate));
        } catch (Exception e) {
            throw new InvalidUserDataException("Unable to parse provided TimeUnit: " + notation, e);
        }
    }
}
