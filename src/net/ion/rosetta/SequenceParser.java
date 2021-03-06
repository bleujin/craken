/*****************************************************************************
 * Copyright (C) Codehaus.org                                                *
 * ------------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License");           *
 * you may not use this file except in compliance with the License.          *
 * You may obtain a copy of the License at                                   *
 *                                                                           *
 * http://www.apache.org/licenses/LICENSE-2.0                                *
 *                                                                           *
 * Unless required by applicable law or agreed to in writing, software       *
 * distributed under the License is distributed on an "AS IS" BASIS,         *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 * See the License for the specific language governing permissions and       *
 * limitations under the License.                                            *
 *****************************************************************************/
package net.ion.rosetta;

final class SequenceParser extends Parser<Object> {
	private final Parser<?>[] parsers;

	SequenceParser(Parser<?>[] parsers) {
		this.parsers = parsers;
	}

	@Override
	boolean apply(ParseContext ctxt) {
		for (Parser<?> p : parsers) {
			if (!p.run(ctxt))
				return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "sequence";
	}
}