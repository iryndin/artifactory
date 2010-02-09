/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.search.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

import java.io.IOException;
import java.io.Reader;

/**
 * An analyzer that breaks tokens on white spaces and lowercases terms
 *
 * @author Noam Y. Tenne
 */
public class ArtifactNameAnalyzer extends Analyzer {

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        return new ArtifactNameTokenizer(reader);
    }

    @Override
    public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
        Tokenizer tokenizer = (Tokenizer) getPreviousTokenStream();
        if (tokenizer == null) {
            tokenizer = new ArtifactNameTokenizer(reader);
            setPreviousTokenStream(tokenizer);
        } else {
            tokenizer.reset(reader);
        }
        return tokenizer;
    }

    private static class ArtifactNameTokenizer extends Tokenizer {

        public ArtifactNameTokenizer(Reader reader) {
            super(reader);
        }

        protected boolean isTokenChar(char c) {
            return !Character.isWhitespace(c);
        }

        @Override
        public Token next(Token reusableToken) throws java.io.IOException {

            char buf[] = new char[64];
            int c = input.read();
            if (c == -1) {
                return null;
            }
            int i = 0;

            do {
                if (i >= buf.length) {
                    //Resize the buffer if needed
                    char resizedBuf[] = new char[buf.length * 2];
                    System.arraycopy(buf, 0, resizedBuf, 0, buf.length);
                    buf = resizedBuf;
                }
                buf[i++] = Character.toLowerCase((char) c);
                c = input.read();
            } while (!Character.isWhitespace(c) && c != -1);

            reusableToken.reinit(buf, 0, i, 0, 0);
            return reusableToken;
        }
    }
}
